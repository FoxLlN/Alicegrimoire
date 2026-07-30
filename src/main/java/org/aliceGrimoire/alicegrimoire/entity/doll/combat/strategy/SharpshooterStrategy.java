package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

import java.util.Random;

public class SharpshooterStrategy implements ICombatStrategy {

    private static final Random RANDOM = new Random();

    private int attackCooldown = 0;
    private Vec3 strafeDirection = Vec3.ZERO;
    private LivingEntity lastTarget = null;

    // ===== 走位参数 =====
    private static final double STRAFE_AMPLITUDE = 0.6;          // 幅度（格），适中
    private static final double APPROACH_SPEED_MULTIPLIER = 0.6;

    // ===== 方向平滑 =====
    private Vec3 targetDirection = Vec3.ZERO;
    private int directionChangeTimer = 0;
    private int nextChangeDelay = 20 + RANDOM.nextInt(20);
    private static final double DIRECTION_SMOOTHING = 0.15;

    // ===== 视线遮挡 =====
    private int seekViewTimer = 0;
    private Vec3 seekDirection = Vec3.ZERO;

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null) return;

        if (lastTarget != target) {
            doll.resetSmoothedTargetY();
            lastTarget = target;
            strafeDirection = Vec3.ZERO;
            targetDirection = Vec3.ZERO;
            directionChangeTimer = 0;
            nextChangeDelay = 20 + RANDOM.nextInt(20);
        }

        CombatParameters params = doll.getDollData().getCombatParams();
        double minDistance = params.getRangedMinDistance();
        double maxDistance = params.getRangedMaxDistance();
        int rangedCooldown = params.getRangedCooldown();
        double speed = params.getChargeSpeed();
        double strafeMultiplier = params.getStrafeMultiplier();

        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        double smoothedY = doll.getSmoothedTargetY(target);
        Vec3 targetPos = target.position();

        Vec3 toTarget = targetPos.subtract(doll.position());
        Vec3 horizontalToTarget = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        Vec3 awayFromTarget = horizontalToTarget.scale(-1);
        Vec3 perpendicular = new Vec3(-horizontalToTarget.z, 0, horizontalToTarget.x);

        Vec3 moveTarget = null;
        double moveSpeed = speed;

        // ===== 距离管理 =====
        if (dist < minDistance - 0.8) {
            double retreatDistance = minDistance - dist + 0.8;
            Vec3 pos = doll.position().add(awayFromTarget.scale(retreatDistance));
            moveTarget = new Vec3(pos.x, smoothedY + 0.1, pos.z);
            strafeDirection = Vec3.ZERO;
            targetDirection = Vec3.ZERO;
            directionChangeTimer = 0;
        } else if (dist > maxDistance) {
            Vec3 pos = targetPos.add(horizontalToTarget.scale(maxDistance - 0.8));
            moveTarget = new Vec3(pos.x, smoothedY + 0.1, pos.z);
            moveSpeed = speed * APPROACH_SPEED_MULTIPLIER;
            strafeDirection = Vec3.ZERO;
            targetDirection = Vec3.ZERO;
            directionChangeTimer = 0;
        } else if (!canSee && dist >= minDistance && dist <= maxDistance) {
            // 视线遮挡：横向寻找视野
            if (seekViewTimer % 15 == 0 || seekDirection.lengthSqr() < 0.01) {
                double angle = (RANDOM.nextBoolean() ? 1 : -1) * (0.8 + RANDOM.nextDouble() * 0.4);
                seekDirection = perpendicular.scale(angle);
                seekDirection = seekDirection.add(horizontalToTarget.scale(RANDOM.nextDouble() * 0.2 - 0.1));
                seekDirection = seekDirection.normalize();
            }
            seekViewTimer++;
            Vec3 pos = doll.position().add(seekDirection.scale(STRAFE_AMPLITUDE));
            moveTarget = new Vec3(pos.x, smoothedY + 0.1, pos.z);
            moveSpeed = speed * 0.4;
        } else {
            // ===== 正常走位 =====
            directionChangeTimer++;
            if (directionChangeTimer > nextChangeDelay) {
                // 横向为主，前后为辅
                double horizontalAngle = (RANDOM.nextDouble() - 0.5) * 1.6; // -0.8 ~ 0.8
                double forwardAngle = (RANDOM.nextDouble() - 0.5) * 0.8;   // -0.4 ~ 0.4
                double heightDiff = targetPos.y - doll.getY();
                double verticalAngle = Math.max(-0.05, Math.min(0.05, heightDiff * 0.02));

                targetDirection = perpendicular.scale(horizontalAngle)
                        .add(horizontalToTarget.scale(forwardAngle))
                        .add(new Vec3(0, verticalAngle, 0));
                double length = targetDirection.length();
                if (length > 0.01) {
                    targetDirection = targetDirection.scale(1.0 / length);
                }

                directionChangeTimer = 0;
                nextChangeDelay = 15 + RANDOM.nextInt(25);
            }

            // 平滑过渡
            if (targetDirection.lengthSqr() > 0.01) {
                if (strafeDirection.lengthSqr() < 0.01) {
                    strafeDirection = targetDirection;
                } else {
                    strafeDirection = strafeDirection.lerp(targetDirection, DIRECTION_SMOOTHING);
                    double len = strafeDirection.length();
                    if (len > 0.01) {
                        strafeDirection = strafeDirection.scale(1.0 / len);
                    }
                }
            }

            // 应用走位
            Vec3 pos = doll.position().add(strafeDirection.scale(STRAFE_AMPLITUDE));
            double targetY = Math.max(smoothedY - 1.0, Math.min(smoothedY + 1.0, pos.y));
            moveTarget = new Vec3(pos.x, targetY, pos.z);
            moveSpeed = speed * strafeMultiplier;
        }

        // ===== 障碍物检测 =====
        if (moveTarget != null) {
            moveTarget = checkAndClimbObstacle(doll, doll.position(), moveTarget);
        }

        if (moveTarget != null) {
            doll.getMoveControl().setWantedPosition(moveTarget.x, moveTarget.y, moveTarget.z, moveSpeed);
        }

        // 面向目标
        double dx = target.getX() - doll.getX();
        double dz = target.getZ() - doll.getZ();
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
        doll.setYRot(yaw);
        doll.yBodyRot = doll.getYRot();

        // 射击
        if (attackCooldown > 0) {
            attackCooldown--;
        } else if (canSee && dist >= minDistance && dist <= maxDistance) {
            if (!doll.isSameOwner(target)) {
                doll.performRangedAttack(target, 1.0F);
                attackCooldown = rangedCooldown;
            }
        }
    }

    private Vec3 checkAndClimbObstacle(DollEntity doll, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.1) return to;

        Vec3 step = dir.normalize().scale(0.3);
        int steps = Math.min((int) (length / 0.3) + 1, 10);

        for (int i = 1; i <= steps; i++) {
            Vec3 checkPos = from.add(step.scale(i));
            BlockPos bp = BlockPos.containing(checkPos.x, checkPos.y, checkPos.z);
            if (doll.level().getBlockState(bp).isSolid()) {
                for (double offset = 0.5; offset <= 1.5; offset += 0.5) {
                    Vec3 newTarget = new Vec3(to.x, to.y + offset, to.z);
                    BlockPos newBp = BlockPos.containing(newTarget.x, newTarget.y, newTarget.z);
                    BlockPos headBp = newBp.above();
                    if (!doll.level().getBlockState(newBp).isSolid() &&
                        !doll.level().getBlockState(headBp).isSolid()) {
                        return newTarget;
                    }
                }
                return to;
            }
        }
        return to;
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown < 10;
    }
}