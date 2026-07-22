package org.aliceGrimoire.alicegrimoire.entity.doll.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.registry.ModAttachments;

import java.util.Set;

public class DollTargetSelector extends TargetGoal {
    private final DollEntity doll;

    public DollTargetSelector(DollEntity doll) {
        super(doll, false);
        this.doll = doll;
    }

    @Override
    public boolean canUse() {
        // 如果已有有效目标，不再切换
        if (doll.getTarget() != null && doll.getTarget().isAlive()) {
            return false;
        }
        
        if (!doll.isEnraged()) return false;
        LivingEntity owner = doll.getOwner();
        if (!(owner instanceof Player player)) return false;

        Set<Integer> marked = player.getData(ModAttachments.MARKED_TARGETS);
        if (marked.isEmpty()) return false;

        for (int id : marked) {
            Entity target = player.level().getEntity(id);
            // 使用 doll.canAttack() 进行基础检查（存活、队伍、非自己等）
            if (target instanceof LivingEntity living && living.isAlive() && doll.canAttack(living)) {
                // 🔥 额外过滤：跳过同主人的人偶
                if (target instanceof DollEntity otherDoll) {
                    LivingEntity otherOwner = otherDoll.getOwner();
                    if (otherOwner != null && otherOwner.equals(owner)) {
                        continue;
                    }
                }
                this.targetMob = living;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (doll.isEnraged() && targetMob != null) {
            doll.setTarget(targetMob);
        }
        super.start();
    }
}