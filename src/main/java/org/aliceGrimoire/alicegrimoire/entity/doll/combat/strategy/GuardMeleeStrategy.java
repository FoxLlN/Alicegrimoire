package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

import java.util.Random;

/**
 * 近卫近战策略：四步循环
 * 步骤1: 冲锋（冲向目标并发起第一次攻击）
 * 步骤2: 黏住连击（持续攻击，直到受伤）
 * 步骤3: 撤回玩家身边
 * 步骤4: 等待 2-3 秒后重复
 * 
 * 只有在步骤2受到伤害才会被打断
 */
public class GuardMeleeStrategy implements ICombatStrategy {
    private static final Random RANDOM = new Random();
    private enum Phase {
        CHARGING,      // 冲锋中
        STICKING,      // 黏住连击
        RETREATING,    // 撤回
        WAITING        // 等待
    }
    
    private Phase phase = Phase.CHARGING;
    private int phaseTicks = 0;
    private int attackCooldown = 0;
    private int waitDuration = 40 + RANDOM.nextInt(20);  // 等待 2-3 秒
    
    private static final int CHARGE_DURATION = 15;      // 冲锋持续 0.75 秒
    private static final int RETREAT_DURATION = 20;     // 撤回持续 1 秒
    private static final int ATTACK_COOLDOWN = 8;       // 攻击间隔 0.4 秒
    private static final double STICK_RANGE = 2.0;       // 黏住时保持的距离

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (target == null || owner == null) return;
        
        double dist = doll.distanceTo(target);
        boolean canSee = doll.getSensing().hasLineOfSight(target);
        
        // 面向目标
        doll.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (phase) {
            case CHARGING:
                // 冲锋：快速冲向目标
                if (phaseTicks < CHARGE_DURATION) {
                    // 高速冲向目标
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(1.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 1.8);
                    phaseTicks++;
                } else {
                    // 冲锋结束，切换为黏住连击
                    phase = Phase.STICKING;
                    phaseTicks = 0;
                }
                break;

            case STICKING:
                // 黏住连击：保持在目标周围 2 格内持续攻击
                if (dist > STICK_RANGE) {
                    Vec3 dir = target.position().subtract(doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(STICK_RANGE - 0.5));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 1.0);
                } else if (dist < 1.0) {
                    // 太近则稍微后退
                    Vec3 away = doll.position().subtract(target.position()).normalize();
                    Vec3 targetPos = doll.position().add(away.scale(1.0));
                    doll.getMoveControl().setWantedPosition(targetPos.x, target.getY() + 0.5, targetPos.z, 0.8);
                }
                
                // 攻击判定
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else if (canSee && dist <= 2.5) {
                    if (!doll.isSameOwner(target)) {
                        doll.doHurtTarget(target);
                    }
                    attackCooldown = ATTACK_COOLDOWN;
                }
                
                // 检查是否受到伤害（由外部标记）
                // 如果受伤，切换到撤回阶段
                if (doll.hurtTime > 0) {
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
                    waitDuration = 40 + RANDOM.nextInt(20); // 重置等待时间
                }
                break;

            case WAITING:
                // 等待 2-3 秒
                if (phaseTicks < waitDuration) {
                    // 在玩家身边小幅度移动
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
        return phase == Phase.CHARGING || phase == Phase.STICKING;
    }
}