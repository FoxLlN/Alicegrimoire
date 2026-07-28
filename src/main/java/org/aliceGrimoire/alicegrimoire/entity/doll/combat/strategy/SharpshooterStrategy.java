package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

import java.util.Random;

/**
 * 射手策略：远离目标 + 随机走位 + 射击
 * 根据武器类型自动适配投射物（弓/弩/三叉戟）
 */
public class SharpshooterStrategy implements ICombatStrategy {
    private int attackCooldown = 0;
    private int strafeTimer = 0;
    private Vec3 strafeDirection = Vec3.ZERO;
    private static final Random RANDOM = new Random();

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null) return;

        CombatParameters params = doll.getDollData().getCombatParams();
        double minDistance = params.getRangedMinDistance();
        double maxDistance = params.getRangedMaxDistance();
        int rangedCooldown = params.getRangedCooldown();
        int strafeInterval = params.getStrafeInterval();
        double chargeSpeed = params.getChargeSpeed(); 

        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // ---- 随机走位 ----
        strafeTimer++;
        if (strafeTimer > strafeInterval + RANDOM.nextInt(20)) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            strafeDirection = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            strafeTimer = 0;
        }

        // ---- 移动逻辑 ----
        Vec3 targetDir = doll.position().subtract(target.position()).normalize();

        if (dist < minDistance) {
            // 太近：后退
            Vec3 awayPos = doll.position().add(targetDir.scale(3.0));
            doll.getMoveControl().setWantedPosition(awayPos.x, doll.getY() + 0.5, awayPos.z, chargeSpeed);
        } else if (dist > maxDistance) {
            // 太远：稍微靠近
            Vec3 towardPos = doll.position().subtract(targetDir.scale(1.0));
            doll.getMoveControl().setWantedPosition(towardPos.x, doll.getY() + 0.5, towardPos.z, chargeSpeed * 0.6);
        } else {
            // 在最佳距离内：横向走位
            Vec3 strafeOffset = strafeDirection.scale(2.0);
            Vec3 targetPos = doll.position().add(strafeOffset);
            doll.getMoveControl().setWantedPosition(targetPos.x, doll.getY() + 0.5, targetPos.z, chargeSpeed * 0.8);
        }

        // ---- 射击 ----
        if (attackCooldown > 0) {
            attackCooldown--;
        } else if (canSee && dist >= minDistance && dist <= maxDistance) {
            if (!doll.isSameOwner(target)) {
                doll.performRangedAttack(target, 1.0F);
            }
            attackCooldown = rangedCooldown;
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown < 10;
    }
}