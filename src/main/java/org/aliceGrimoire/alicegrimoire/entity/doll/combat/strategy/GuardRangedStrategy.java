package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

/**
 * 近卫远程策略：四步循环 + 抵近射击
 * 在黏住阶段保持 2 格距离进行射击（而不是近战攻击）
 */
public class GuardRangedStrategy implements ICombatStrategy {
    private static final java.util.Random RANDOM = new java.util.Random();
    private enum Phase {
        CHARGING, STICKING, RETREATING, WAITING
    }
    
    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private int attackCooldown = 0;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    
    private static final int CHARGE_DURATION = 15;
    private static final int RETREAT_DURATION = 20;
    private static final int ATTACK_COOLDOWN = 20; // 远程攻击冷却稍长
    private static final double SHOOT_RANGE = 2.0; // 抵近射击保持 2 格
    

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;
        
        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);
        
        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (phase) {
            case CHARGING:
                if (phaseTicks < CHARGE_DURATION) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(2.0)); // 停在 2 格外
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 1.5);
                    phaseTicks++;
                } else {
                    phase = Phase.STICKING;
                    phaseTicks = 0;
                }
                break;

            case STICKING:
                // 保持在 2 格距离
                if (dist > SHOOT_RANGE + 0.5) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(SHOOT_RANGE));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
                } else if (dist < SHOOT_RANGE - 0.5) {
                    Vec3 away = doll.position().subtract(target.position()).normalize();
                    Vec3 targetPos = doll.position().add(away.scale(0.5));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
                }
                
                // 射击判定（使用远程攻击）
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else if (canSee && dist <= 8.0 && dist >= 1.0) {
                    if (!doll.isSameOwner(target)) {
                        doll.performRangedAttack(target, 1.0F);
                    }
                    attackCooldown = ATTACK_COOLDOWN;
                }
                
                if (doll.hurtTime > 0) {
                    phase = Phase.RETREATING;
                    phaseTicks = 0;
                }
                break;

            case RETREATING:
                if (phaseTicks < RETREAT_DURATION) {
                    Vec3 dir = owner.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = owner.position().subtract(dir.scale(2.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, 1.5);
                    phaseTicks++;
                } else {
                    phase = Phase.WAITING;
                    phaseTicks = 0;
                    waitDuration = 40 + RANDOM.nextInt(20);
                }
                break;

            case WAITING:
                if (phaseTicks < waitDuration) {
                    Vec3 dir = owner.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = owner.position().subtract(dir.scale(2.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, 0.3);
                    phaseTicks++;
                } else {
                    phase = Phase.CHARGING;
                    phaseTicks = 0;
                }
                break;
        }
    }

    @Override
    public boolean isAttacking() {
        return phase == Phase.CHARGING || phase == Phase.STICKING;
    }
}