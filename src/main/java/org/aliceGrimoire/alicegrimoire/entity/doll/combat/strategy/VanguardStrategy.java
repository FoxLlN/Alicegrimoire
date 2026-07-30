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
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.WeaponType;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 游击策略：一击脱离（递进射击）
 * - 近战模式：冲向目标 → 黏住攻击直到命中 → 等待延迟 → 撤回玩家身边 → 等待 2-3 秒 → 重复
 * - 远程模式：冲锋至远程最佳距离 → 黏住射击直到命中 → 等待延迟 → 撤回玩家身边 → 等待 2-3 秒 → 重复
 * - 三叉戟（附魔激流）：高速冲刺冲向目标，然后进入黏住攻击
 * 
 * 与近卫策略的区别：攻击成功后立即进入撤回，不会持续连击。
 */
public class VanguardStrategy implements ICombatStrategy {
    private enum Phase {
        CHARGING,      // 冲锋阶段
        ATTACKING,     // 黏住攻击阶段（持续尝试攻击，直到命中）
        POST_ATTACK,   // 攻击后等待阶段（命中后延迟后撤回）
        RETREATING,    // 撤回玩家身边
        WAITING        // 等待冷却
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.Random RANDOM = new java.util.Random();

    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    private int attackCooldown = 0;          // 用于远程攻击冷却
    private LivingEntity lastTarget = null;
    private boolean isRanged = false;
    private boolean isTrident = false;
    private WeaponType cachedWeapon = WeaponType.NONE;

    // 攻击成功后的延迟计时（等待结束后进入撤回）
    private int postAttackTimer = 0;

    // 超时保护：仅在距离合适且攻击未命中时累计，距离不合适时重置
    private int attemptTicks = 0;
    private static final int MAX_ATTEMPT = 80; // 最多尝试 4 秒（80 tick）

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;

        if (lastTarget != target) {
            reset();
            doll.resetSmoothedTargetY();
            lastTarget = target;
        }

        refreshWeaponMode(doll);

        CombatParameters params = doll.getDollData().getCombatParams();
        int postAttackDelay = params.getAttackDelay();
        int chargeDuration = params.getChargeDuration();
        double holdDistance = params.getHoldDistance();
        double retreatSpeed = params.getRetreatSpeed();
        double retreatThreshold = params.getRetreatThreshold();
        double waitSpeed = params.getWaitSpeed();
        double waitDistance = params.getWaitDistance();
        int waitDurationBase = params.getWaitDuration();
        int rangedCooldown = params.getRangedCooldown();
        double meleeReach = params.getAttackRange();
        double minRange = params.getRangedMinDistance();
        double maxRange = params.getRangedMaxDistance();
        double chargeSpeed = params.getChargeSpeed();
        double riptideMultiplier = params.getRiptideMultiplier();

        double deadZoneLow = 0.3;
        double deadZoneHigh = 0.5;

        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 获取平滑后的目标 Y 坐标（过滤击退跳跃）
        double smoothedY = doll.getSmoothedTargetY(target);

        switch (phase) {
            case CHARGING:
                // ---------- 冲锋阶段 ----------
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
                                speedMultiplier = Math.min(speedMultiplier, 15.0);
                                doll.setDeltaMovement(dir.scale(chargeSpeed * speedMultiplier));
                                doll.setYRot(-((float) Math.atan2(dir.x, dir.z)) * (180F / (float) Math.PI));
                                doll.yBodyRot = doll.getYRot();
                            }
                            isRiptideCharging = true;

                            // ===== 检测是否已经到达目标附近（提前结束冲锋） =====
                            if (dist <= holdDistance + 0.5) {
                                phase = Phase.ATTACKING;
                                phaseTicks = 0;
                                attemptTicks = 0;
                                attackCooldown = 0;
                                postAttackTimer = 0;
                                break;
                            }
                        }
                    }

                    // ===== 非激流冲刺：移动控制冲锋 =====
                    if (!isRiptideCharging) {
                        Vec3 dir = target.position().subtract(doll.position()).normalize();
                        double distance = isRanged ? minRange : holdDistance;
                        Vec3 targetPos = target.position().subtract(dir.scale(distance));
                        doll.getMoveControl().setWantedPosition(
                            targetPos.x, smoothedY + 0.1, targetPos.z,
                            chargeSpeed
                        );
                    }

                    phaseTicks++;
                } else {
                    phase = Phase.ATTACKING;
                    phaseTicks = 0;
                    attemptTicks = 0;
                    attackCooldown = 0;
                    postAttackTimer = 0;
                }
                break;

            case ATTACKING:
                // ---------- 黏住攻击阶段 ----------
                // 1. 移动逻辑：保持距离在攻击范围内（近战为 holdDistance，远程为 minRange 附近）
                if (isRanged) {
                    // 远程模式：保持在 minRange 附近（带死区）
                    double targetDist = minRange;
                    double diff = dist - targetDist;

                    if (diff > deadZoneHigh) {
                        // 太远：靠近到 targetDist
                        Vec3 dir = target.position().subtract(doll.position()).normalize();
                        double approachDist = Math.min(targetDist, maxRange);
                        Vec3 targetPos = target.position().subtract(dir.scale(approachDist));
                        doll.getMoveControl().setWantedPosition(
                            targetPos.x, smoothedY + 0.1, targetPos.z, chargeSpeed
                        );
                    } else if (diff < -deadZoneLow) {
                        // 太近：后退到 targetDist + 0.3
                        double backDist = targetDist + 0.3;
                        Vec3 away = doll.position().subtract(target.position()).normalize();
                        double moveBack = backDist - dist;
                        Vec3 targetPos = doll.position().add(away.scale(moveBack));
                        doll.getMoveControl().setWantedPosition(
                            targetPos.x, smoothedY + 0.1, targetPos.z, chargeSpeed
                        );
                    }
                    // 在死区内不移动
                } else {
                    // 近战模式（包括普通三叉戟和近战武器）：保持在 holdDistance 附近
                    if (dist > holdDistance) {
                        Vec3 dir = target.position().subtract(doll.position()).normalize();
                        Vec3 targetPos = target.position().subtract(dir.scale(holdDistance - 0.5));
                        doll.getMoveControl().setWantedPosition(
                            targetPos.x, smoothedY + 0.1, targetPos.z, chargeSpeed
                        );
                    } else if (dist < 1.0) {
                        Vec3 away = doll.position().subtract(target.position()).normalize();
                        Vec3 targetPos = doll.position().add(away.scale(1.0));
                        doll.getMoveControl().setWantedPosition(
                            targetPos.x, smoothedY + 0.1, targetPos.z, chargeSpeed
                        );
                    }
                }

                // ---------- 2. 攻击尝试 ----------
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else if (canSee) {
                    boolean hit = false;
                    boolean inAttackRange = false;

                    if (isRanged) {
                        // 远程攻击条件：距离在 [minRange - deadZoneLow, maxRange] 之间
                        if (dist >= minRange - deadZoneLow && dist <= maxRange) {
                            inAttackRange = true;
                            if (!doll.isSameOwner(target)) {
                                doll.performRangedAttack(target, 1.0F);
                                hit = true;
                                attackCooldown = rangedCooldown;
                            }
                        }
                    } else {
                        // 近战攻击（包括普通三叉戟）：距离 ≤ meleeReach
                        if (dist <= meleeReach) {
                            inAttackRange = true;
                            if (!doll.isSameOwner(target)) {
                                doll.doHurtTarget(target);
                                hit = true;
                                attackCooldown = postAttackDelay;
                            }
                        }
                    }

                    if (hit) {
                        // 攻击成功，进入后延迟
                        phase = Phase.POST_ATTACK;
                        phaseTicks = 0;
                        postAttackTimer = 0;
                        doll.getMoveControl().setWantedPosition(
                            doll.getX(), doll.getY(), doll.getZ(), 0
                        );
                        attemptTicks = 0;
                    } else {
                        // 未命中：根据是否在攻击范围内处理超时计时
                        if (inAttackRange) {
                            attemptTicks++;
                        } else {
                            attemptTicks = 0;
                        }
                    }
                } else {
                    attemptTicks = 0;
                }

                // 超时保护：如果尝试次数过多，强制撤回
                if (attemptTicks > MAX_ATTEMPT) {
                    phase = Phase.RETREATING;
                    phaseTicks = 0;
                    attemptTicks = 0;
                }
                break;

            case POST_ATTACK:
                // ---------- 攻击后等待阶段 ----------
                postAttackTimer++;
                if (postAttackTimer > postAttackDelay) {
                    phase = Phase.RETREATING;
                    phaseTicks = 0;
                }
                break;

            case RETREATING:
                // ---------- 撤回玩家身边 ----------
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
                // ---------- 等待冷却 ----------
                if (phaseTicks < waitDuration) {
                    doll.followOwner(owner, waitSpeed, waitDistance);
                    phaseTicks++;
                } else {
                    phase = Phase.CHARGING;
                    phaseTicks = 0;
                }
                break;
        }
        
        // ===== 强制面向目标 =====
        double dx = target.getX() - doll.getX();
        double dz = target.getZ() - doll.getZ();
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
        doll.setYRot(yaw);
        doll.yBodyRot = doll.getYRot();
    }

    @Override
    public boolean isAttacking() {
        return phase == Phase.CHARGING || phase == Phase.ATTACKING;
    }

    @Override
    public void reset() {
        this.phase = Phase.CHARGING;
        this.phaseTicks = 0;
        this.waitDuration = 40 + RANDOM.nextInt(20);
        this.attackCooldown = 0;
        this.attemptTicks = 0;
        this.postAttackTimer = 0;
        this.lastTarget = null;
    }

    private void refreshWeaponMode(DollEntity doll) {
        WeaponType current = doll.getDollData().getWeaponType();
        if (current == cachedWeapon) return;
        cachedWeapon = current;
        isRanged = (current == WeaponType.BOW || current == WeaponType.CROSSBOW);
        isTrident = (current == WeaponType.TRIDENT);
    }
}