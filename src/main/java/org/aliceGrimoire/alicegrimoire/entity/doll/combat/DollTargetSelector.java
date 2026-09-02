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
        // 获取指令指定的目标 ID（来自指挥棒）
        int assignedId = doll.getAssignedTargetId();
        
        // 如果没有指定目标，直接返回 false（不自动索敌）
        if (assignedId == -1) {
            return false;
        }

        // ===== 检查指定目标是否有效 =====
        Entity target = doll.level().getEntity(assignedId);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            // 指定目标无效（已死亡/被移除），清除 ID 并返回 false
            doll.setAssignedTargetId(-1);
            return false;
        }

        // ===== 检查是否可以攻击该目标 =====
        if (!doll.canAttack(living)) {
            // 无法攻击（如创造模式玩家、友方等），清除 ID 并返回 false
            doll.setAssignedTargetId(-1);
            return false;
        }

        // ===== 额外过滤：不能攻击同主人的其他人偶 =====
        if (target instanceof DollEntity otherDoll) {
            LivingEntity otherOwner = otherDoll.getOwner();
            LivingEntity owner = doll.getOwner();
            if (otherOwner != null && owner != null && otherOwner.equals(owner)) {
                // 目标是己方人偶，不能攻击
                doll.setAssignedTargetId(-1);
                return false;
            }
        }

        // ===== 允许覆盖已有目标 =====
        // 即使 doll.getTarget() 已存在（如守御自动索敌），
        // 只要 assignedId 指向一个不同的有效目标，就覆盖它
        LivingEntity currentTarget = doll.getTarget();
        if (currentTarget != null && currentTarget.getId() == assignedId) {
            // 如果当前目标已经是指定目标，且仍然有效，不需要重复设置
            // 但为了保险，仍然返回 true（保持锁定）
            this.targetMob = living;
            return true;
        }

        // 设置目标（覆盖已有的任何目标）
        this.targetMob = living;
        return true;
    }

    @Override
    public void start() {
        if (doll.isEnraged() && targetMob != null) {
            doll.setTarget(targetMob);
        }
        super.start();
    }
}