package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction.IDamageReactionStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction.ReactionStrategyRegistry;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.WeaponType;

import java.util.Random;

/**
 * 近卫策略：四步循环
 * 根据武器类型自动适配战斗行为：
 * - 近战武器（剑/矛）：近战攻击，冲锋速度 1.8
 * - 远程武器（弓/弩）：抵近射击，保持在 2 格距离
 * - 三叉戟：激流冲锋，冲锋速度 2.5
 * 
 * 步骤1: 冲锋 → 步骤2: 黏住连击 → 步骤3: 撤回玩家身边 → 步骤4: 等待 2-3 秒
 */
public class GuardStrategy implements ICombatStrategy {
    private static final Random RANDOM = new Random();

    private enum Phase {
        CHARGING,      // 冲锋中
        STICKING,      // 黏住连击
        RETREATING,    // 撤回
        WAITING        // 等待
    }

    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private int attackCooldown = 0;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    private float lastHealth = -1;
    private IDamageReactionStrategy reactionStrategy;
    private LivingEntity lastTarget = null;

    // ===== 根据武器类型缓存的配置 =====
    private WeaponType cachedWeaponType = WeaponType.NONE;
    private double chargeSpeed = 1.8;
    private double stickAttackRange = 2.5;
    private boolean isRanged = false;
    private boolean isTrident = false;

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;

        // 目标变化检测 + 重置
        if (lastTarget != target) {
            reset();
            lastTarget = target;
        }

        // ===== 根据当前武器刷新配置 =====
        refreshWeaponConfig(doll);

        CombatParameters params = doll.getDollData().getCombatParams();
        int chargeDuration = params.getChargeDuration();
        double stickRange = params.getStickRange();
        int attackCooldownMax = params.getAttackCooldown();
        int shootCooldown = params.getShootCooldown();
        double shootRange = params.getShootRange();

        // 获取伤害反应策略
        if (reactionStrategy == null) {
            reactionStrategy = ReactionStrategyRegistry.create(params.getReactionType());
        }

        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // ===== 防御性编程：只有 STICKING 阶段才检测伤害 =====
        if (phase != Phase.STICKING) {
            lastHealth = -1;
        } else {
            float currentHealth = doll.getHealth();
            if (reactionStrategy.shouldRetreat(doll, target, owner, currentHealth, lastHealth, phaseTicks)) {
                phase = Phase.RETREATING;
                phaseTicks = 0;
                lastHealth = -1;
            }
            lastHealth = currentHealth;
        }

        // ===== 状态机主循环 =====
        switch (phase) {
            case CHARGING:
                handleCharging(doll, target, chargeDuration);
                break;

            case STICKING:
                handleSticking(doll, target, canSee, dist, stickRange, shootRange,
                        attackCooldownMax, shootCooldown, stickAttackRange);
                break;

            case RETREATING:
                handleRetreating(doll, owner);
                break;

            case WAITING:
                handleWaiting(doll, owner);
                break;
        }
    }

    // ========== 各阶段处理私有方法 ==========

    private void handleCharging(DollEntity doll, LivingEntity target, int chargeDuration) {
        if (phaseTicks < chargeDuration) {
            if (isTrident) {
                // 三叉戟：激流冲锋（直接设置速度）
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                doll.setDeltaMovement(dir.scale(chargeSpeed));
            } else {
                // 近战/远程：移动控制冲锋
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                double offset = isRanged ? 2.0 : 1.0;
                Vec3 targetPos = target.position().subtract(dir.scale(offset));
                doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, chargeSpeed);
            }
            phaseTicks++;
        } else {
            phase = Phase.STICKING;
            phaseTicks = 0;
        }
    }

    private void handleSticking(DollEntity doll, LivingEntity target, boolean canSee,
                                double dist, double stickRange, double shootRange,
                                int attackCooldownMax, int shootCooldown, double stickAttackRange) {
        // 移动逻辑
        if (isRanged) {
            // 远程：保持 2 格距离
            if (dist > shootRange + 0.5) {
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                Vec3 targetPos = target.position().subtract(dir.scale(shootRange));
                doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
            } else if (dist < shootRange - 0.5) {
                Vec3 away = doll.position().subtract(target.position()).normalize();
                Vec3 targetPos = doll.position().add(away.scale(0.5));
                doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
            }
        } else {
            // 近战/三叉戟：保持在目标周围
            if (dist > stickRange) {
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                Vec3 targetPos = target.position().subtract(dir.scale(stickRange - 0.5));
                doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 1.0);
            } else if (dist < 1.0) {
                Vec3 away = doll.position().subtract(target.position()).normalize();
                Vec3 targetPos = doll.position().add(away.scale(1.0));
                doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
            }
        }

        // 攻击判定
        if (attackCooldown > 0) {
            attackCooldown--;
        } else if (canSee) {
            if (isRanged) {
                // 远程攻击
                if (dist <= 8.0 && dist >= 1.0 && !doll.isSameOwner(target)) {
                    doll.performRangedAttack(target, 1.0F);
                    attackCooldown = shootCooldown;
                }
            } else {
                // 近战攻击（包括三叉戟）
                double range = isTrident ? 2.5 : stickAttackRange;
                if (dist <= range && !doll.isSameOwner(target)) {
                    doll.doHurtTarget(target);
                    attackCooldown = attackCooldownMax;
                }
            }
        }
    }

    private void handleRetreating(DollEntity doll, LivingEntity owner) {
        double distToOwner = doll.distanceTo(owner);
        if (distToOwner > 3.0) {
            doll.followOwner(owner, 1.2, 1.5);
        } else {
            phase = Phase.WAITING;
            phaseTicks = 0;
            waitDuration = 40 + RANDOM.nextInt(20);
            lastHealth = -1;
        }
    }

    private void handleWaiting(DollEntity doll, LivingEntity owner) {
        if (phaseTicks < waitDuration) {
            doll.followOwner(owner, 0.8, 1.0);
            phaseTicks++;
        } else {
            phase = Phase.CHARGING;
            phaseTicks = 0;
            lastHealth = -1;
        }
    }

    // ========== 武器配置刷新 ==========

    private void refreshWeaponConfig(DollEntity doll) {
        WeaponType currentWeapon = doll.getDollData().getWeaponType();
        if (currentWeapon == cachedWeaponType) return;

        cachedWeaponType = currentWeapon;
        isRanged = (currentWeapon == WeaponType.BOW || currentWeapon == WeaponType.CROSSBOW);
        isTrident = (currentWeapon == WeaponType.TRIDENT);

        if (isTrident) {
            chargeSpeed = 2.5;
            stickAttackRange = 2.5;
        } else if (isRanged) {
            chargeSpeed = 1.5;
            stickAttackRange = 2.0;
        } else {
            chargeSpeed = 1.8;
            stickAttackRange = 2.5;
        }
    }

    // ========== ICombatStrategy 接口方法 ==========

    @Override
    public boolean isAttacking() {
        return phase == Phase.CHARGING || phase == Phase.STICKING;
    }

    @Override
    public void reset() {
        this.phase = Phase.CHARGING;
        this.phaseTicks = 0;
        this.attackCooldown = 0;
        this.waitDuration = 40 + RANDOM.nextInt(20);
        this.lastHealth = -1;
    }
}