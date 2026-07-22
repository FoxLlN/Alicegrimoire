package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

/**
 * 骑枪策略：先远离目标蓄力 4~6 秒，然后直线冲锋，造成高额伤害和击退。
 */
public class LancerChargeStrategy implements ICombatStrategy {
    private int chargeTicks = 0;
    private boolean isCharging = false;
    private static final int CHARGE_DELAY = 100; // 5 秒蓄力
    private static final int CHARGE_DURATION = 30; // 冲锋持续 1.5 秒

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        double dist = doll.distanceTo(target);

        if (!isCharging) {
            // 蓄力阶段：远离目标并面向它
            doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (dist < 10.0) {
                Vec3 away = doll.position().subtract(target.position()).normalize().scale(3.0);
                doll.getMoveControl().setWantedPosition(doll.getX() + away.x, doll.getY() + 1.0, doll.getZ() + away.z, 0.8);
            }
            chargeTicks++;
            if (chargeTicks >= CHARGE_DELAY) {
                isCharging = true;
                chargeTicks = 0;
            }
        } else {
            // 冲锋阶段：直线高速冲向目标
            Vec3 dir = target.position().subtract(doll.position()).normalize();
            // 冲锋速度较快
            doll.setDeltaMovement(dir.scale(1.5));
            // 碰撞检测（如果碰到目标）
            if (doll.getBoundingBox().inflate(1.2).intersects(target.getBoundingBox())) {
                if (!doll.isSameOwner(target)) {
                    doll.doHurtTarget(target);
                    // 额外击退（击退 II 等效于 1.0 力度）
                    target.knockback(1.0, -dir.x, -dir.z);
                }
                isCharging = false; // 冲锋结束
                chargeTicks = 0;
            }
            // 如果冲锋时间过长，自动结束
            chargeTicks++;
            if (chargeTicks > CHARGE_DURATION) {
                isCharging = false;
                chargeTicks = 0;
            }
        }
    }

    @Override
    public boolean isAttacking() {
        return isCharging;
    }
}