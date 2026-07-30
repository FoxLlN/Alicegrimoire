package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

public class DefenderStrategy implements ICombatStrategy {

    private Vec3 lastTargetPos = null;
    private int moveCooldown = 0;
    private int attackCooldown = 0;

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (owner == null) return;

        CombatParameters params = doll.getDollData().getCombatParams();
        double guardSpeed = params.getGuardSpeed();
        double guardRadius = params.getGuardRadius();
        double holdDistance = params.getHoldDistance();

        // ===== 从战斗参数读取近战配置 =====
        double attackRange = params.getAttackRange();
        double attackVerticalRange = params.getAttackVerticalRange();
        int attackCooldownMax = params.getAttackCooldown();

        // 直接使用传入的 target（由 DollEntity 自动索敌维护）
        LivingEntity faceTarget = target;

        // 2. 面向目标
        if (faceTarget != null && doll.distanceTo(faceTarget) <= 16.0 && doll.getSensing().hasLineOfSight(faceTarget)) {
            doll.getLookControl().setLookAt(faceTarget, 30.0F, 30.0F);
        }

        // 3. 移动逻辑：保持在玩家 guardRadius 半径内
        double distToOwner = doll.distanceTo(owner);
        Vec3 moveTarget = null;

        if (distToOwner > guardRadius) {
            Vec3 dir = owner.position().subtract(doll.position()).normalize();
            double step = Math.min(holdDistance, distToOwner - guardRadius);
            moveTarget = owner.position().subtract(dir.scale(step));
        } else if (faceTarget != null && distToOwner > guardRadius * 0.5) {
            Vec3 dir = faceTarget.position().subtract(owner.position()).normalize();
            moveTarget = owner.position().add(dir.scale(1.5));
        }

        if (moveTarget != null) {
            moveTarget = new Vec3(moveTarget.x, owner.getY() + 1.5, moveTarget.z);
            if (lastTargetPos == null || moveTarget.distanceToSqr(lastTargetPos) > 0.25) {
                lastTargetPos = moveTarget;
                moveCooldown = 0;
            }
            if (moveCooldown <= 0) {
                doll.getMoveControl().setWantedPosition(moveTarget.x, moveTarget.y, moveTarget.z, guardSpeed);
                moveCooldown = 2;
            } else {
                moveCooldown--;
            }
        } else {
            lastTargetPos = null;
        }

        // ===== 4. 近战反击：敌人碰到人偶时自动攻击（含垂直判定） =====
        if (faceTarget != null) {
            // ---- 计算水平距离 ----
            double dx = faceTarget.getX() - doll.getX();
            double dz = faceTarget.getZ() - doll.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            // ---- 计算垂直距离 ----
            double targetHeight = faceTarget.getEyeHeight() * 0.6;
            double targetY = faceTarget.getY() + targetHeight;
            double dy = targetY - doll.getY();

            boolean horizontalInRange = horizontalDist <= attackRange;
            boolean verticalInRange = Math.abs(dy) <= attackVerticalRange;
            boolean canSee = doll.getSensing().hasLineOfSight(faceTarget);

            if (horizontalInRange && verticalInRange && canSee) {
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else if (!doll.isSameOwner(faceTarget)) {
                    doll.doHurtTarget(faceTarget);
                    attackCooldown = attackCooldownMax;
                }
            } else {
                if (attackCooldown > 0) {
                    attackCooldown--;
                }
            }
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown > 0 && attackCooldown < 8;
    }
}