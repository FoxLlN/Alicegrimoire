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
                return false;
            }
        }

        // 如果没有指定目标，则从全局标记中选择（为兼容保留，但实际不会走到这里）
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