package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.WeaponType;

/**
 * 游击策略：一击脱离
 * 近战模式：冲向目标 → 攻击一次 → 立即撤回玩家身边 → 等待 2-3 秒
 * 远程模式（持有弓/弩/三叉戟时）：冲锋至远程最佳距离 → 射击一次 → 立即撤回玩家身边 → 等待 2-3 秒
 */
public class VanguardStrategy implements ICombatStrategy {
    private enum Phase {
        CHARGING, ATTACKING, RETREATING, WAITING
    }
    private static final java.util.Random RANDOM = new java.util.Random();

    // 临时冷却变量（用于远程攻击冷却）
    private int attackCooldown = 0;
    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;          // 用于攻击成功后的延迟计时
    private boolean hasAttacked = false;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    private LivingEntity lastTarget = null;
    private boolean isRanged = false;
    private WeaponType cachedWeapon = WeaponType.NONE;

    // 新增：尝试攻击计数（防止无限等待）
    private int attemptTicks = 0;
    private static final int MAX_ATTEMPT = 30; // 最多尝试1.5秒（30 tick）

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;

        if (lastTarget != target) {
            reset();
            lastTarget = target;
        }

        refreshWeaponMode(doll);

        CombatParameters params = doll.getDollData().getCombatParams();
        int chargeDuration = params.getChargeDuration();
        double holdDistance = params.getHoldDistance();
        double retreatSpeed = params.getRetreatSpeed();
        double retreatThreshold = params.getRetreatThreshold();
        double waitSpeed = params.getWaitSpeed();
        double waitDistance = params.getWaitDistance();
        int waitDurationBase = params.getWaitDuration();

        // 远程参数
        double rangedMin = params.getRangedMinDistance();
        double rangedMax = params.getRangedMaxDistance();
        int rangedCooldown = params.getRangedCooldown();

        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (phase) {
            case CHARGING:
                if (phaseTicks < chargeDuration) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    double stopDistance = isRanged ? rangedMin : holdDistance;
                    Vec3 targetPos = target.position().subtract(dir.scale(stopDistance));
                    doll.getMoveControl().setWantedPosition(
                        targetPos.x, target.getY() + 0.5, targetPos.z,
                        params.getChargeSpeed()
                    );
                    phaseTicks++;
                } else {
                    phase = Phase.ATTACKING;
                    phaseTicks = 0;
                    hasAttacked = false;
                    attemptTicks = 0; // 重置尝试计数
                }
                break;

            case ATTACKING:
                // ===== 如果尚未攻击，尝试攻击 =====
                if (!hasAttacked) {
                    // 尝试攻击
                    if (canSee) {
                        if (isRanged) {
                            if (dist >= rangedMin && dist <= rangedMax && !doll.isSameOwner(target)) {
                                doll.performRangedAttack(target, 1.0F);
                                hasAttacked = true;
                                attackCooldown = rangedCooldown;
                                phaseTicks = 0; // 重置延迟计时器
                            }
                        } else {
                            if (dist <= params.getAttackRange() + 1.0 && !doll.isSameOwner(target)) {
                                doll.doHurtTarget(target);
                                hasAttacked = true;
                                phaseTicks = 0; // 重置延迟计时器
                            }
                        }
                    }

                    // 增加尝试计数
                    attemptTicks++;
                    // 如果尝试次数过多仍未能攻击，强制撤回（避免无限卡住）
                    if (attemptTicks > MAX_ATTEMPT) {
                        phase = Phase.RETREATING;
                        phaseTicks = 0;
                        attemptTicks = 0;
                        hasAttacked = false; // 标记为未攻击，但已强制撤回
                    }
                } else {
                    // ===== 攻击成功后，等待延迟再撤回 =====
                    phaseTicks++;
                    if (phaseTicks > params.getAttackDelay()) {
                        phase = Phase.RETREATING;
                        phaseTicks = 0;
                        attemptTicks = 0;
                    }
                }
                break;

            case RETREATING:
                double distToOwner = doll.distanceTo(owner);
                if (distToOwner > retreatThreshold) {
                    doll.followOwner(owner, retreatSpeed, holdDistance);
                } else {
                    phase = Phase.WAITING;
                    phaseTicks = 0;
                    waitDuration = waitDurationBase + RANDOM.nextInt(20);
                }
                break;

            case WAITING:
                if (phaseTicks < waitDuration) {
                    doll.followOwner(owner, waitSpeed, waitDistance);
                    phaseTicks++;
                } else {
                    phase = Phase.CHARGING;
                    phaseTicks = 0;
                }
                break;
        }
    }

    @Override
    public boolean isAttacking() {
        return phase == Phase.CHARGING || phase == Phase.ATTACKING;
    }

    @Override
    public void reset() {
        this.phase = Phase.CHARGING;
        this.phaseTicks = 0;
        this.hasAttacked = false;
        this.waitDuration = 40 + RANDOM.nextInt(20);
        this.attackCooldown = 0;
        this.attemptTicks = 0;
    }

    private void refreshWeaponMode(DollEntity doll) {
        WeaponType current = doll.getDollData().getWeaponType();
        if (current == cachedWeapon) return;
        cachedWeapon = current;
        isRanged = (current == WeaponType.BOW || current == WeaponType.CROSSBOW || current == WeaponType.TRIDENT);
    }
}