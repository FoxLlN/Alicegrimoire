package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

/**
 * 游击策略：一击脱离
 * 冲向目标 → 攻击一次 → 立即撤回玩家身边 → 等待 2-3 秒 → 重复
 * 与近卫策略类似，但不会在攻击后黏住目标
 */
public class VanguardStrategy implements ICombatStrategy {
    private enum Phase {
        CHARGING,   // 冲锋
        ATTACKING,  // 攻击（瞬发）
        RETREATING, // 撤回
        WAITING     // 等待
    }
    private static final java.util.Random RANDOM = new java.util.Random();
    
    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private boolean hasAttacked = false;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    private LivingEntity lastTarget = null;

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;
        
        if (lastTarget != target) {
            reset();
            lastTarget = target;
        }

        // 从 DollData 读取所有参数
        CombatParameters params = doll.getDollData().getCombatParams();
        int chargeDuration = params.getVanguardChargeDuration();
        double attackDistance = params.getAttackDistance();

        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);
        
        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (phase) {
            case CHARGING:
                // 冲锋：快速冲向目标
                if (phaseTicks < chargeDuration) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(1.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 2.0);
                    phaseTicks++;
                } else {
                    // 冲锋结束，进入攻击阶段
                    phase = Phase.ATTACKING;
                    phaseTicks = 0;
                    hasAttacked = false;
                }
                break;

            case ATTACKING:
                // 攻击（仅在进入阶段时执行一次）
                if (!hasAttacked && canSee && dist <= attackDistance + 1.0) {
                    if (!doll.isSameOwner(target)) {
                        doll.doHurtTarget(target);
                    }
                    hasAttacked = true;
                }
                phaseTicks++;
                // 无论是否攻击成功，0.3 秒后进入撤回阶段
                if (phaseTicks > 6) {
                    phase = Phase.RETREATING;
                    phaseTicks = 0;
                }
                break;

            case RETREATING:
                double distToOwner = doll.distanceTo(owner);
                if (distToOwner > 3.0) {
                    // 使用跟随速度的1.2倍，目标距离1.5格
                    doll.followOwner(owner, 1.2, 1.5);
                } else {
                    phase = Phase.WAITING;
                    phaseTicks = 0;
                    waitDuration = 40 + RANDOM.nextInt(20);
                }
                break;

            case WAITING:
                // 等待 2-3 秒
                if (phaseTicks < waitDuration) {
                    // 动态跟随玩家，而不是固定在某个点
                    doll.followOwner(owner, 0.8, 1.0);
                    phaseTicks++;
                } else {
                    // 重置循环
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

    // 重置策略状态
    @Override
    public void reset() {
        this.phase = Phase.CHARGING;
        this.phaseTicks = 0;
        this.hasAttacked = false;
        this.waitDuration = 40 + RANDOM.nextInt(20);
    }
}