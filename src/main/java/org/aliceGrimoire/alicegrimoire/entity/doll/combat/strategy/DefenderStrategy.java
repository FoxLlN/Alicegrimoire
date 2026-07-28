package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;

/**
 * 守御策略：持盾保护玩家
 * - 始终在玩家 guardRadius 半径内游荡
 * - 举盾面对目标
 * - 玩家受伤时瞬移至攻击者与玩家之间
 * - 受攻击后破盾 shieldDisableTime 秒（斧头攻击额外延长）
 */
public class DefenderStrategy implements ICombatStrategy {
    
    @Override
    public void tick(DollEntity doll, LivingEntity target, LivingEntity owner) {
        if (owner == null) return;

        CombatParameters params = doll.getDollData().getCombatParams();
        double guardSpeed = params.getGuardSpeed();
        double guardRadius = params.getGuardRadius();
        double holdDistance = params.getHoldDistance();
        int shieldDisableTime = params.getShieldDisableTime();

        // 1. 面向最近的目标（如果存在）
        if (target != null && doll.distanceTo(target) <= 16.0 && doll.getSensing().hasLineOfSight(target)) {
            doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // 2. 保持在玩家 guardRadius 半径内
        double distToOwner = doll.distanceTo(owner);
        if (distToOwner > guardRadius) {
            // 靠近玩家
            Vec3 dir = owner.position().subtract(doll.position()).normalize();
            Vec3 targetPos = owner.position().subtract(dir.scale(Math.min(holdDistance, distToOwner - guardRadius)));
            doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, guardSpeed);
        } else if (target != null) {
            // 在玩家身边时，调整到玩家与目标之间（面朝目标）
            Vec3 dir = target.position().subtract(owner.position()).normalize();
            Vec3 targetPos = owner.position().add(dir.scale(1.5));
            doll.getMoveControl().setWantedPosition(targetPos.x, owner.getY() + 1.5, targetPos.z, guardSpeed);
        }

        // 3. 始终举盾（由 DollEntity.isBlocking() 控制）
        // 状态由 DollEntity 中的 shieldDisableTicks 控制
    }

    @Override
    public boolean isAttacking() {
        return false; // 守御不主动攻击
    }
}