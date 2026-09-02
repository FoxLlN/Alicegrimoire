package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction.IDamageReactionStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction.ReactionStrategyRegistry;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.WeaponType;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Random;

/**
 * 近卫策略：四步循环
 * 根据武器类型自动适配战斗行为：
 * - 近战武器（剑/矛）：近战攻击
 * - 远程武器（弓/弩）：抵近射击
 * - 三叉戟：普通三叉戟使用移动控制冲锋，附魔激流的三叉戟使用高速冲刺
 * 
 * 步骤1: 冲锋 → 步骤2: 黏住连击 → 步骤3: 撤回玩家身边 → 步骤4: 等待
 * 
 * 所有数值均从 CombatParameters 读取，支持织魔台改装
 */
public class GuardStrategy implements ICombatStrategy {
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Phase {
        CHARGING, STICKING, RETREATING, WAITING
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
    private boolean isRanged = false;
    private boolean isTrident = false;

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;

        // 目标变化检测 + 重置
        if (lastTarget != target) {
            reset();
            doll.resetSmoothedTargetY();
            lastTarget = target;
        }

        // ===== 根据当前武器刷新配置 =====
        refreshWeaponConfig(doll);

        CombatParameters params = doll.getDollData().getCombatParams();
        int chargeDuration = params.getChargeDuration();
        double holdDistance = params.getHoldDistance();
        double retreatThreshold = params.getRetreatThreshold();
        double rangedMinDistance = params.getRangedMinDistance();

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
                handleCharging(doll, target, chargeDuration, holdDistance, params.getChargeSpeed(), 
                               rangedMinDistance, params.getRiptideMultiplier());
                break;

            case STICKING:
                handleSticking(doll, target, canSee, dist, params);
                break;

            case RETREATING:
                handleRetreating(doll, owner, retreatThreshold, params.getRetreatSpeed(), 
                                 holdDistance, params.getWaitDuration());
                break;

            case WAITING:
                handleWaiting(doll, owner, params.getWaitSpeed(), params.getWaitDistance());
                break;
        }
        // ===== 强制面向目标 =====
        double dx = target.getX() - doll.getX();
        double dz = target.getZ() - doll.getZ();
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
        doll.setYRot(yaw);
        doll.yBodyRot = doll.getYRot();
    }


    // ========== 各阶段处理私有方法 ==========

    /**
     * 冲锋阶段处理
     * - 三叉戟（附魔激流）：高速冲刺（直接设置速度）
     * - 其他所有武器（近战/远程/普通三叉戟）：移动控制冲锋
     */
    private void handleCharging(DollEntity doll, LivingEntity target, int chargeDuration,
                            double holdDistance, double chargeSpeed, double rangedMinDistance,
                            double riptideMultiplier) {
        if (phaseTicks < chargeDuration) {
            boolean isRiptideCharging = false;

            // ===== 检测激流冲刺（仅三叉戟 + 激流附魔） =====
            if (isTrident) {
                ItemStack mainHand = doll.getDollData().getWeapon();
                ItemEnchantments enchantments = mainHand.get(DataComponents.ENCHANTMENTS);
                int riptideLevel = 0;
                if (enchantments != null) {
                    for (var entry : enchantments.entrySet()) {
                        Holder<Enchantment> holder = entry.getKey();
                        if (holder.unwrapKey().isPresent() &&
                            holder.unwrapKey().get().equals(Enchantments.RIPTIDE)) {
                            riptideLevel = entry.getValue();
                            break;
                        }
                    }
                }
                if (riptideLevel > 0) {
                    // ===== 激流冲刺：只在第一 tick 施加一次速度 =====
                    if (phaseTicks == 0) {
                        Vec3 dir = target.position().subtract(doll.position()).normalize();
                        double speedMultiplier = 1.0 + riptideLevel * riptideMultiplier;
                        speedMultiplier = Math.min(speedMultiplier, 6.0);
                        doll.setDeltaMovement(dir.scale(chargeSpeed * speedMultiplier));
                        // 朝目标方向
                        doll.setYRot(-((float) Math.atan2(dir.x, dir.z)) * (180F / (float) Math.PI));
                        doll.yBodyRot = doll.getYRot();
                    }
                    isRiptideCharging = true;

                    // ===== 检测是否已经到达目标附近（提前结束冲锋） =====
                    double dist = doll.distanceTo(target);
                    // 如果距离小于黏住距离，立刻进入 STICKING
                    if (dist <= holdDistance + 0.5) {
                        phase = Phase.STICKING;
                        phaseTicks = 0;
                        return;
                    }
                }
            }

            // ===== 非激流冲刺：移动控制冲锋 =====
            if (!isRiptideCharging) {
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                double distance = isRanged ? rangedMinDistance : holdDistance;
                Vec3 targetPos = target.position().subtract(dir.scale(distance));
                doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, chargeSpeed);
            }

            phaseTicks++;
        } else {
            phase = Phase.STICKING;
            phaseTicks = 0;
        }
    }

    /**
     * 黏住阶段（STICKING）处理
     * 
     * @param doll              人偶实体
     * @param target            当前攻击目标
     * @param canSee            是否视线通畅
     * @param currentDist       当前与目标的距离
     * @param params            战斗参数（包含所有配置）
     */
    private void handleSticking(DollEntity doll, LivingEntity target, boolean canSee,
                                double currentDist, CombatParameters params) {
        // 从参数中获取各项配置
        double holdDist = params.getHoldDistance();          // 近战黏住距离
        double minRange = params.getRangedMinDistance();     // 远程最小攻击距离
        double maxRange = params.getRangedMaxDistance();     // 远程最大攻击距离
        int meleeCooldown = params.getAttackCooldown();      // 近战攻击冷却
        int rangedCooldown = params.getRangedCooldown();     // 远程射击冷却
        double meleeReach = params.getAttackRange();         // 近战攻击距离（判定范围）
        double speed = params.getChargeSpeed();              // 移动速度倍率

        // 死区：距离在 [minRange - 0.3, minRange + 0.5] 内不移动，避免频繁调整
        double deadZoneLow = 0.3;   // 低于 minRange - 0.3 时后退
        double deadZoneHigh = 0.5;  // 高于 minRange + 0.5 时靠近

        // ---------- 移动逻辑 ----------
        if (isRanged) {
            // ===== 远程模式：抵近射击，保持在 minRange 附近 =====
            double targetDist = minRange;
            double diff = currentDist - targetDist;

            if (diff > deadZoneHigh) {
                // 太远：靠近到 targetDist
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                Vec3 targetPos = target.position().subtract(dir.scale(targetDist));
                double smoothedY = doll.getSmoothedTargetY(target);
                doll.getMoveControl().setWantedPosition(
                    targetPos.x, smoothedY + 0.1, targetPos.z, speed
                );
            } else if (diff < -deadZoneLow) {
                // 太近：后退到 targetDist + 0.3（留出缓冲，避免反复后退）
                double backDist = targetDist + 0.3;
                Vec3 away = doll.position().subtract(target.position()).normalize();
                // 计算需要后退的距离，使最终距离约为 backDist
                double moveBack = backDist - currentDist;
                Vec3 targetPos = doll.position().add(away.scale(moveBack));
                double smoothedY = doll.getSmoothedTargetY(target);
                doll.getMoveControl().setWantedPosition(
                    targetPos.x, smoothedY + 0.1, targetPos.z, speed
                );
            }
            // 在死区内：不主动移动，让目标或其它因素自然调整
        } else {
            // ===== 近战模式：保持在 holdDist 附近 =====
            if (currentDist > holdDist) {
                Vec3 dir = target.position().subtract(doll.position()).normalize();
                Vec3 targetPos = target.position().subtract(dir.scale(holdDist - 0.5));
                double smoothedY = doll.getSmoothedTargetY(target);
                doll.getMoveControl().setWantedPosition(
                    targetPos.x, smoothedY + 0.1, targetPos.z, speed
                );
            } else if (currentDist < 1.0) {
                Vec3 away = doll.position().subtract(target.position()).normalize();
                Vec3 targetPos = doll.position().add(away.scale(1.0));
                double smoothedY = doll.getSmoothedTargetY(target);
                doll.getMoveControl().setWantedPosition(
                    targetPos.x, smoothedY + 0.1, targetPos.z, speed
                );
            }
        }

        // ---------- 攻击判定 ----------
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        if (!canSee) return;

        if (isRanged) {
            // 远程攻击：距离在 [minRange - deadZoneLow, maxRange] 之间
            if (currentDist >= minRange - deadZoneLow && currentDist <= maxRange && !doll.isSameOwner(target)) {
                doll.performRangedAttack(target, 1.0F);
                attackCooldown = rangedCooldown;
            }
        } else {
            // 近战攻击：距离 ≤ meleeReach
            if (currentDist <= meleeReach && !doll.isSameOwner(target)) {
                doll.doHurtTarget(target);
                attackCooldown = meleeCooldown;
            }
        }
    }

    private void handleRetreating(DollEntity doll, LivingEntity owner, double retreatThreshold, double retreatSpeed, double holdDistance, int waitDurationBase) {
        double distToOwner = doll.distanceTo(owner);
        if (distToOwner > retreatThreshold) {
            doll.followOwner(owner, retreatSpeed, holdDistance);
        } else {
            phase = Phase.WAITING;
            phaseTicks = 0;
            waitDuration = waitDurationBase + RANDOM.nextInt(20);
            lastHealth = -1;
        }
    }

    private void handleWaiting(DollEntity doll, LivingEntity owner, double waitSpeed, double waitDistance) {
        if (phaseTicks < waitDuration) {
            doll.followOwner(owner, waitSpeed, waitDistance);
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
        // 重置反应策略
        if (this.reactionStrategy != null) {
            this.reactionStrategy.reset();
        }
    }
}