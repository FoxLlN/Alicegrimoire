package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

import java.util.Random;

/**
 * 掠夺策略：使用弩射击，保持与目标 6 格距离（风筝）。
 * 靠近时后退，远离时靠近（但速度慢）。
 */
public class RangedKiteStrategy implements ICombatStrategy {
    private int attackCooldown;
    private static final Random RANDOM = new Random();

    public RangedKiteStrategy() {
        this.attackCooldown = RANDOM.nextInt(30); // 初始随机 0~1.5 秒
    }

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);

        // 移动逻辑：维持 6 格距离
        if (dist < 6.0) {
            Vec3 away = doll.position().subtract(target.position()).normalize().scale(3.0);
            doll.getMoveControl().setWantedPosition(doll.getX() + away.x, doll.getY() + 1.0, doll.getZ() + away.z, 0.8);
        } else if (dist > 6.0) {
            Vec3 dir = target.position().subtract(doll.position()).normalize().scale(2.0);
            doll.getMoveControl().setWantedPosition(doll.getX() + dir.x, doll.getY() + 1.0, doll.getZ() + dir.z, 0.6);
        }

        if (attackCooldown > 0) attackCooldown--;
        if (attackCooldown <= 0 && canSee && dist <= 12.0) {
            if (!doll.isSameOwner(target)) {
                doll.performRangedAttack(target, 1.0F);
            }
            attackCooldown = 40; // 2 秒冷却
        }
    }

    @Override
    public boolean isAttacking() {
        return attackCooldown < 20;
    }
}