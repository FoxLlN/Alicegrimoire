package org.aliceGrimoire.alicegrimoire.entity.doll.movement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollState;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollStateManager;
import org.aliceGrimoire.alicegrimoire.event.PlayerMoveDetector;

import com.mojang.logging.LogUtils;

/**
 * 人偶移动控制器，负责根据当前状态决定移动目标点和速度。
 * - IDLE 状态：随机游荡（类似悦灵/恼鬼的徘徊）
 * - FOLLOWING 状态：跟随玩家（速度 = 玩家疾跑速度 × 1.1）
 * - ENGAGING 和 RECOVERING 状态：不干预移动（由战斗策略或状态机自行控制）
 * 速度三档：游荡（0.1）、跟随（动态）、出击（0.35×1.5）
 */
public class DollMovementHandler {
    private static final java.util.Random RANDOM = new java.util.Random();

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private final DollEntity doll;
    private final DollStateManager stateManager;

    // 游荡/跟随的目标位置（用于 MoveControl）
    private Vec3 wantedPosition = Vec3.ZERO;
    private double speedModifier = 0.1;

    public DollMovementHandler(DollEntity doll, DollStateManager stateManager) {
        this.doll = doll;
        this.stateManager = stateManager;
    }

    /**
     * 每帧调用，根据当前状态设置移动目标。
     */
    public void tick() {
        // ===== 人偶哨：不主动移动状态 =====
        if (doll.getNoMovementTicks() > 0) {
            // 不主动移动，但可以被拴绳拖动
            // 强制清除移动目标
            doll.getMoveControl().setWantedPosition(
                doll.getX(), doll.getY(), doll.getZ(), 0
            );
            doll.setDeltaMovement(Vec3.ZERO);
            return; // 跳过其他移动逻辑
        }

        DollState state = stateManager.getCurrentState();
        LivingEntity owner = doll.getOwner();

        // ===== 强制拖回（最高优先级） =====
        if (owner != null && doll.isTethered()) {
            double distance = doll.distanceTo(owner);
            double dragStart = doll.getDollData().getDragStartRange(); // 默认16
            if (distance > dragStart) {
                Vec3 targetPos = owner.position().add(0, 0.5, 0);
                // 速度随距离增大，最小2.0，最大5.0
                double speed = Math.min(2.0 + (distance - dragStart) * 0.1, 5.0);
                doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, speed);
                // 直接返回，不执行后续移动逻辑
                return;
            }
        }

        switch (state) {
            case IDLE:
                // 随机游荡
                if (shouldUpdateWantedPosition()) {
                    Vec3 target = generateRandomTarget(owner);
                    setWantedPosition(target, getWanderSpeed());
                }
                break;

            case FOLLOWING:
                // 跟随玩家
                if (owner != null && doll.isPlayerActivelyMoving()) {
                    Vec3 followPos = calculateFollowPosition(owner);
                    double speed = getFollowSpeed(owner);
                    setWantedPosition(followPos, speed);
                }
                break;

            case ENGAGING:
                // 战斗期间，移动由战斗策略控制
                break;
            case RECOVERING:
                // 返回玩家
                if (owner != null) {
                    Vec3 followPos = calculateFollowPosition(owner);
                    double speed = Math.max(getFollowSpeed(owner) * 0.8, 0.15);
                    setWantedPosition(followPos, speed);
                }
                break;
        }
    }

    /**
     * 是否需要更新目标位置（随机间隔）
     */
    private boolean shouldUpdateWantedPosition() {
        // 每 20 tick（1秒）更新一次，避免频繁计算
        return doll.tickCount % 20 == 0 || !doll.getMoveControl().hasWanted();
    }

    /**
     * 生成随机游荡目标（以主人或自身为中心，半径 8 格）
     */
    private Vec3 generateRandomTarget(LivingEntity owner) {
        Vec3 center = owner != null ? owner.position() : doll.position();
        double range = doll.isReturning() ? 3.0 : 8.0; // 返回模式范围缩小到3格
        double x = center.x + (doll.getRandom().nextDouble() - 0.5) * 2 * range;
        double y = center.y + 1.0 + doll.getRandom().nextDouble() * 3.0;
        double z = center.z + (doll.getRandom().nextDouble() - 0.5) * 2 * range;
        return new Vec3(x, y, z);
    }

    /**
     * 计算跟随玩家的目标位置（同步移动，在玩家附近偏移）
     */
    private Vec3 calculateFollowPosition(LivingEntity owner) {
        // 跟随玩家时，目标位置在玩家正上方 1.5 格处，保持与玩家相对静止
        // 可以加一个小偏移，避免完全重叠
        Vec3 lookVec = owner.getLookAngle();
        Vec3 offset = new Vec3(
                lookVec.x * 0.5,
                1.5,
                lookVec.z * 0.5
        );
        return owner.position().add(offset);
    }

    /**
     * 设置移动目标并应用速度
     */
    private void setWantedPosition(Vec3 target, double speed) {
        this.wantedPosition = target;
        this.speedModifier = speed;
        doll.getMoveControl().setWantedPosition(
                target.x, target.y, target.z, speed
        );
    }

    // ========== 速度方法（从 DollData 读取） ==========

    // 计算游荡速度（默认 0.1）
    public double getWanderSpeed() {
        return doll.getDollData().getWanderSpeed();
    }

    // 计算跟随速度
    public double getFollowSpeed(LivingEntity owner) {
        double multiplier = doll.getDollData().getFollowSpeedMultiplier();
        
        double playerSpeed = PlayerMoveDetector.getPlayerSpeed(owner.getUUID());
        if (playerSpeed <= 0.01) {
            Vec3 vel = owner.getDeltaMovement();
            playerSpeed = vel.length();
        }
        if (playerSpeed <= 0.01) {
            playerSpeed = owner.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.2;
        }
        
        double dx = doll.getX() - owner.getX();
        double dz = doll.getZ() - owner.getZ();
        double dist = Math.sqrt(dx*dx + dz*dz);
        double factor = (dist <= 2.0) ? 1.0 : 1.0 + Math.min((dist - 2.0) / 8.0, 1.0);
        
        double followSpeed = playerSpeed * multiplier * factor;
        return Math.max(playerSpeed, followSpeed);
    }

    // ========== 移动方法 ==========
    public double getStrikeSpeed(LivingEntity owner) {
        double followSpeed = getFollowSpeed(owner); // 动态跟随速度
        double multiplier = doll.getDollData().getStrikeSpeedMultiplier(); // 默认1.5
        return followSpeed * multiplier;
    }

    // ========== 辅助方法 ==========

    /**
     * 强制将人偶移动到指定位置（用于冲锋、瞬移等）
     */
    public void moveTo(Vec3 target, double speed) {
        setWantedPosition(target, speed);
    }

    /**
     * 直接设置人偶位置（用于脱困）
     */
    public void teleportTo(Vec3 target) {
        doll.moveTo(target.x, target.y, target.z);
    }

    
    /**
     * 以指定倍率跟随玩家（使用跟随速度作为基准）
     * @param doll 人偶实体
     * @param owner 玩家实体
     * @param speedMultiplier 速度倍率（1.0 = 正常跟随速度，0.4 = 缓慢跟随）
     * @param desiredDistance 期望与玩家的水平距离
     */
    public void followOwner(DollEntity doll, LivingEntity owner, double speedMultiplier, double desiredDistance) {
        if (owner == null) return;

        // 如果处于不移动状态，不执行跟随逻辑
        if (doll.getNoMovementTicks() > 0) {
            return;
        }
        
        double dx = doll.getX() - owner.getX();
        double dz = doll.getZ() - owner.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        // 如果在理想距离范围内，仅做随机飘动
        if (Math.abs(horizontalDist - desiredDistance) < 0.5) {
            if (doll.tickCount % 20 == 0) {
                double angle = RANDOM.nextDouble() * 2 * Math.PI;
                Vec3 randomOffset = new Vec3(Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
                Vec3 targetPos = owner.position().add(randomOffset.x, 1.0, randomOffset.z);
                doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 0.1);
            }
            return;
        }
        
        // 【核心改动】使用 getFollowSpeed 获取基准速度
        double baseSpeed = getFollowSpeed(owner);
        double speed = Math.min(baseSpeed * speedMultiplier, 1.0);
        
        // 计算目标方向
        Vec3 dirFromOwner;
        if (horizontalDist < 0.01) {
            dirFromOwner = owner.getLookAngle();
        } else {
            dirFromOwner = new Vec3(dx / horizontalDist, 0, dz / horizontalDist);
        }
        
        Vec3 targetPos = owner.position().add(
            dirFromOwner.x * desiredDistance,
            1.0 + (doll.getRandom().nextDouble() - 0.5) * 0.5,
            dirFromOwner.z * desiredDistance
        );
        
        doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, speed);
        doll.getLookControl().setLookAt(owner, 30.0F, 30.0F);
    }
}