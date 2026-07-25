package org.aliceGrimoire.alicegrimoire.entity.doll.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

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

        // 如果人偶有指定的目标 ID，直接检查该目标
        int assignedId = doll.getAssignedTargetId();
        if (assignedId != -1) {
            Entity target = doll.level().getEntity(assignedId);
            if (target instanceof LivingEntity living && living.isAlive() && doll.canAttack(living)) {
                // 额外过滤同主人
                if (target instanceof DollEntity otherDoll) {
                    LivingEntity otherOwner = otherDoll.getOwner();
                    LivingEntity owner = doll.getOwner();
                    if (otherOwner != null && owner != null && otherOwner.equals(owner)) {
                        return false;
                    }
                }
                this.targetMob = living;
                return true;
            } else {
                // 指定目标无效，返回 false，不切换
                doll.setAssignedTargetId(-1); // 清除无效ID
                return false;
            }
        }

        // 如果没有指定
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