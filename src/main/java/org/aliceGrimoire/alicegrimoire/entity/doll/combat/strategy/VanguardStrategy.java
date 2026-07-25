package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

/**
 * 游击策略：一击脱离
 * 冲向目标 → 攻击一次 → 立即撤回玩家身边 → 等待 2-3 秒 → 重复
 * 与近卫策略类似，但不会在攻击后黏住目标
 */
public class VanguardStrategy implements ICombatStrategy {
    private enum Phase {
        CHARGING,   // 冲锋
        ATTACKING,  // 攻击（瞬发）
        RETREATING, // 撤回
        WAITING     // 等待
    }
    private static final java.util.Random RANDOM = new java.util.Random();
    
    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private boolean hasAttacked = false;
    private int waitDuration = 40 + RANDOM.nextInt(20);
    
    private static final int CHARGE_DURATION = 12;
    private static final int RETREAT_DURATION = 20;
    private static final double ATTACK_DISTANCE = 1.5;
    

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;
        
        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);
        
        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (phase) {
            case CHARGING:
                // 冲锋：快速冲向目标
                if (phaseTicks < CHARGE_DURATION) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(1.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 2.0);
                    phaseTicks++;
                } else {
                    // 冲锋结束，进入攻击阶段
                    phase = Phase.ATTACKING;
                    phaseTicks = 0;
                    hasAttacked = false;
                }
                break;

            case ATTACKING:
                // 攻击（仅在进入阶段时执行一次）
                if (!hasAttacked && canSee && dist <= ATTACK_DISTANCE + 1.0) {
                    if (!doll.isSameOwner(target)) {
                        doll.doHurtTarget(target);
                    }
                    hasAttacked = true;
                }
                phaseTicks++;
                // 无论是否攻击成功，0.3 秒后进入撤回阶段
                if (phaseTicks > 6) {
                    phase = Phase.RETREATING;
                    phaseTicks = 0;
                }
                break;

            case RETREATING:
                // 撤回玩家身边
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
                // 等待 2-3 秒
                if (phaseTicks < waitDuration) {
                    Vec3 dir = owner.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = owner.position().subtract(dir.scale(2.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, 0.3);
                    phaseTicks++;
                } else {
                    // 重置循环
                    phase = Phase.CHARGING;
                    phaseTicks = 0;
                }
                break;
        }
    }

    @Override
    public boolean isAttacking() {
        return phase == Phase.CHARGING || phase == Phase.ATTACKING;
    }
}