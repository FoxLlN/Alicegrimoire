package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

/**
 * 近卫三叉戟策略：四步循环 + 激流冲刺
 * 在冲锋阶段使用激流冲刺（快速冲向目标）
 */
public class GuardTridentStrategy implements ICombatStrategy {
    private static final java.util.Random RANDOM = new java.util.Random();

    private enum Phase {
        CHARGING, STICKING, RETREATING, WAITING
    }
    
    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private int attackCooldown = 0;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    
    private static final int CHARGE_DURATION = 12;
    private static final int RETREAT_DURATION = 20;
    private static final int ATTACK_COOLDOWN = 8;
    private static final double STICK_RANGE = 2.5;
    

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;
        
        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);
        
        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (phase) {
            case CHARGING:
                if (phaseTicks < CHARGE_DURATION) {
                    // 激流冲锋：极速冲向目标，附带水粒子效果（仅视觉，由渲染器处理）
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    // 冲锋速度比普通近卫更快
                    doll.setDeltaMovement(dir.scale(2.5));
                    phaseTicks++;
                } else {
                    phase = Phase.STICKING;
                    phaseTicks = 0;
                }
                break;

            case STICKING:
                // 相同的黏住连击逻辑
                if (dist > STICK_RANGE) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(STICK_RANGE - 0.5));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 1.0);
                } else if (dist < 1.0) {
                    Vec3 away = doll.position().subtract(target.position()).normalize();
                    Vec3 targetPos = doll.position().add(away.scale(1.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
                }
                
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else if (canSee && dist <= 2.5) {
                    if (!doll.isSameOwner(target)) {
                        doll.doHurtTarget(target);
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