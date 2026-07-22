package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;

/**
 * 近卫策略：持盾保护玩家，面向最近标记目标，保持在玩家 4 格内。
 * 攻击方式：盾牌反击（由 hurt 事件驱动），AI 不主动攻击。
 */
public class GuardStrategy implements ICombatStrategy {

    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (owner == null) return;

        // 1. 面向目标（仅当目标在 16 格内且可见）
        if (target != null && doll.distanceTo(target) <= 16.0 && doll.getSensing().hasLineOfSight(target)) {
            doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // 2. 保持在玩家 4 格半径内
        double distToOwner = doll.distanceTo(owner);
        if (distToOwner > 4.0) {
            Vec3 dir = owner.position().subtract(doll.position()).normalize();
            Vec3 targetPos = owner.position().subtract(dir.scale(2.0)); // 停在玩家身前 2 格
            doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, 1.0);
        } else if (target != null) {
            // 如果已经在玩家身边，可向目标方向稍微调整
            Vec3 dir = target.position().subtract(doll.position()).normalize();
            Vec3 targetPos = doll.position().add(dir.scale(0.5));
            doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, 0.8);
        }

        // 3. 更新盾牌禁用计时（由 hurt 事件设置）
        // 在 DollEntity 中通过 shieldDisableTicks 控制 isBlocking，这里无需额外操作
    }

    @Override
    public boolean isAttacking() {
        return false; // 近卫不主动攻击，只有反击
    }
}