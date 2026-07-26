package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

import java.util.Random;

/**
 * 射手三叉戟策略：像溺尸一样投射三叉戟
 * 行为与射手弓相似，但使用三叉戟投射（由 performRangedAttack 处理）
 */
public class SharpshooterTridentStrategy implements ICombatStrategy {
    private int attackCooldown = 0;
    private int strafeTimer = 0;
    private Vec3 strafeDirection = Vec3.ZERO;
    private static final Random RANDOM = new Random();

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null) return;

        // 从 DollData 读取所有参数
        CombatParameters params = doll.getDollData().getCombatParams();
        double minDistance = params.getTridentMinDistance();
        double maxDistance = params.getTridentMaxDistance();
        int tridentCooldown = params.getTridentCooldown();
        int strafeInterval = params.getStrafeInterval();
        
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
            Vec3 awayPos = doll.position().add(targetDir.scale(3.0));
            doll.getMoveControl().setWantedPosition(awayPos.x, doll.getY() + 0.5, awayPos.z, 1.0);
        } else if (dist > maxDistance) {
            Vec3 towardPos = doll.position().subtract(targetDir.scale(1.0));
            doll.getMoveControl().setWantedPosition(towardPos.x, doll.getY() + 0.5, towardPos.z, 0.6);
        } else {
            Vec3 strafeOffset = strafeDirection.scale(2.0);
            Vec3 targetPos = doll.position().add(strafeOffset);
            doll.getMoveControl().setWantedPosition(targetPos.x, doll.getY() + 0.5, targetPos.z, 0.8);
        }
        
        // ---- 射击（投射三叉戟） ----
        if (attackCooldown > 0) {
            attackCooldown--;
        } else if (canSee && dist >= minDistance && dist <= maxDistance) {
            if (!doll.isSameOwner(target)) {
                doll.performRangedAttack(target, 1.0F);
            }
            attackCooldown = tridentCooldown;
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown < 10;
    }
}