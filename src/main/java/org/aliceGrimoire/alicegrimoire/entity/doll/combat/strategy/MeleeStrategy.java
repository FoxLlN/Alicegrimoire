package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

/**
 * 近战策略：标准人偶、持剑人偶共用。
 * - 瞄准目标腰部，攻击判定采用圆柱体范围（水平距离 + 垂直容差）
 * - 攻击前强制面向目标，自动适应目标高度（低头/抬头攻击）
 * - 攻击距离 1.6 格，停止距离 1.2 格
 */
public class MeleeStrategy implements ICombatStrategy {
    private int attackCooldown = 0;
    
    // 配置参数
    private static final double ATTACK_RANGE = 1.6;          // 攻击水平距离
    private static final double ATTACK_VERTICAL_RANGE = 3.0; // 攻击垂直容差（腰部上下 ±1.5 格）
    private static final double STOP_DISTANCE = 1.2;          // 停止水平距离

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || !target.isAlive()) return;

        // ---- 计算距离 ----
        double dx = target.getX() - doll.getX();
        double dz = target.getZ() - doll.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // 目标腰部位置（实体眼睛高度的 60% 位置，更接近实际身体中心）
        double targetHeight = target.getEyeHeight() * 0.6;
        double targetY = target.getY() + targetHeight;
        double dy = targetY - doll.getY();

        // ---- 面向目标腰部（支持低头/抬头） ----
        // 使用 LookControl 的坐标版本，直接看向世界坐标
        doll.getLookControl().setLookAt(
            target.getX(), targetY, target.getZ(),
            60.0F, 60.0F
        );

        // ---- 移动逻辑：停在目标前方 1.0 格 ----
        if (horizontalDist > STOP_DISTANCE) {
            Vec3 horizontalDir = new Vec3(dx / horizontalDist, 0, dz / horizontalDist);
            double targetX = target.getX() - horizontalDir.x * STOP_DISTANCE;
            double targetZ = target.getZ() - horizontalDir.z * STOP_DISTANCE;
            
            // Y 轴移动：平滑靠近目标腰部高度
            double targetYMove = doll.getY() + dy * 0.3;
            if (Math.abs(dy) < 1.0) {
                targetYMove = targetY;
            }
            
            Vec3 targetPos = new Vec3(targetX, targetYMove, targetZ);
            doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0);
        } else if (horizontalDist < 0.5 && Math.abs(dy) > 1.0) {
            // 水平很近但垂直距离较大，只做垂直移动
            Vec3 targetPos = new Vec3(doll.getX(), targetY, doll.getZ());
            doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0);
        }

        // ---- 攻击冷却 ----
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        // ---- 攻击判定（圆柱体范围：水平距离 + 垂直容差） ----
        boolean horizontalInRange = horizontalDist <= ATTACK_RANGE;
        boolean verticalInRange = Math.abs(dy) <= ATTACK_VERTICAL_RANGE;
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        if (horizontalInRange && verticalInRange && canSee) {
            // 攻击前再次强制面向目标（确保方向准确）
            doll.getLookControl().setLookAt(
                target.getX(), targetY, target.getZ(),
                60.0F, 60.0F
            );
            
            if (!doll.isSameOwner(target)) {
                doll.doHurtTarget(target);
            }
            
            attackCooldown = 10; // 0.5 秒冷却
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown > 2 && attackCooldown < 10;
    }
}