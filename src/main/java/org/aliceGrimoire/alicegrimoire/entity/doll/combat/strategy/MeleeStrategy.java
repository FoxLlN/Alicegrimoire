package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 近战策略：标准人偶。
 * - 瞄准目标腰部，攻击判定采用圆柱体范围（水平距离 + 垂直容差）
 * - 攻击前强制面向目标，自动适应目标高度（低头/抬头攻击）
 * - 攻击距离 1.5 格，停止距离 1.0 格
 * 
 * 调试日志已开启，用于跟踪移动目标点和距离计算。
 */
public class MeleeStrategy implements ICombatStrategy {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private int attackCooldown = 0;

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || !target.isAlive()) return;
        
        // 从 DollData 读取所有参数
        CombatParameters params = doll.getDollData().getCombatParams();
        double attackRange = params.getAttackRange();
        double attackVerticalRange = params.getAttackVerticalRange();
        double stopDistance = params.getStopDistance();
        int cooldownMax = params.getAttackCooldown();

        // ---- 计算距离 ----
        double dx = target.getX() - doll.getX();
        double dz = target.getZ() - doll.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // 目标腰部位置（实体眼睛高度的 60% 位置，更接近实际身体中心）
        double targetHeight = target.getEyeHeight() * 0.6;
        double targetY = target.getY() + targetHeight;
        double dy = targetY - doll.getY();

        // ---- 面向目标腰部（支持低头/抬头） ----
        doll.getLookControl().setLookAt(
                target.getX(), targetY, target.getZ(),
                60.0F, 60.0F
        );

        //LOGGER.info("【移动】目标高度为{}", targetY);

        // ---- 移动逻辑：停在目标前方 1.0 格 ----
        Vec3 targetPos = null;
        if (horizontalDist > stopDistance) {
            Vec3 horizontalDir = new Vec3(dx / horizontalDist, 0, dz / horizontalDist);
            double targetX = target.getX() - horizontalDir.x * stopDistance;
            double targetZ = target.getZ() - horizontalDir.z * stopDistance;
            
            // Y 轴移动：平滑靠近目标腰部高度
            double targetYMove = doll.getY() + dy * 0.3;
            if (Math.abs(dy) < 1.0) {
                targetYMove = targetY;
            }
            
            targetPos = new Vec3(targetX, targetYMove, targetZ);
            // LOGGER.info("【移动】水平距离>停止距离，目标点=({}, {}, {)", targetPos.x, targetPos.y, targetPos.z);
            doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0);
        } else if (horizontalDist < 0.5 && Math.abs(dy) > 1.0) {
            double targetYMove = doll.getY() + dy * 0.3; // 已经是平滑
            // 但若 dy 很大，可进一步限制单次移动量
            targetPos = new Vec3(doll.getX(), targetYMove, doll.getZ());
            doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0);
            // LOGGER.info("【移动】水平小于 高度为{}.", targetPos.y);
        } else {
            // LOGGER.info("【移动】距离合适，不移动。水平距离={}, 垂直差值={}", horizontalDist, dy);
        }

        // ---- 攻击冷却 ----
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        // ---- 攻击判定（圆柱体范围：水平距离 + 垂直容差） ----
        boolean horizontalInRange = horizontalDist <= attackRange;
        boolean verticalInRange = Math.abs(dy) <= attackVerticalRange;
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        // LOGGER.info("【攻击判定】水平在范围内={}, 垂直在范围内={}, 视线可见={}", horizontalInRange, verticalInRange, canSee);

        if (horizontalInRange && verticalInRange && canSee) {
            // 攻击前再次强制面向目标（确保方向准确）
            doll.getLookControl().setLookAt(
                target.getX(), targetY, target.getZ(),
                60.0F, 60.0F
            );
            
            if (!doll.isSameOwner(target)) {
                doll.doHurtTarget(target);
                // LOGGER.info("【攻击】对目标 {} 发动了攻击！", target.getName().getString());
            }
            
            attackCooldown = cooldownMax; // 0.5 秒冷却
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown > 2 && attackCooldown < 10;
    }
}