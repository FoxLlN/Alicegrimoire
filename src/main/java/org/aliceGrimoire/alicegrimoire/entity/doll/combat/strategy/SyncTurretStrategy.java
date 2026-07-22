package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

import java.util.Random;

/**
 * 神射策略：保持与玩家同步移动，并远程射箭。
 */
public class SyncTurretStrategy implements ICombatStrategy {
    private int attackCooldown;
    private static final Random RANDOM = new Random();

    public SyncTurretStrategy() {
        this.attackCooldown = RANDOM.nextInt(20); // 初始随机 0~1 秒
    }

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (owner == null) return;

        // 1. 同步移动
        Vec3 playerPos = owner.position();
        Vec3 lookVec = owner.getLookAngle();
        Vec3 offset = new Vec3(lookVec.x * 0.5, 1.5, lookVec.z * 0.5);
        Vec3 targetPos = playerPos.add(offset);
        doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0);

        // 2. 射箭
        if (attackCooldown > 0) attackCooldown--;
        if (attackCooldown <= 0 && doll.getSensing().hasLineOfSight(target) && doll.distanceTo(target) <= 16.0) {
            if (!doll.isSameOwner(target)) {
                doll.performRangedAttack(target, 1.0F);
            }
            attackCooldown = 30; // 1.5 秒冷却
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown < 15;
    }
}