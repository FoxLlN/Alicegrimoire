package org.aliceGrimoire.alicegrimoire.entity.doll.combat;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 战斗管理器
 * 根据人偶的职业类型（DollJobType）和武器类型动态选择战斗策略
 */
public class DollCombatManager {
    private final DollEntity doll;

    public DollCombatManager(DollEntity doll) {
        this.doll = doll;
    }

    public void tick() {
        LivingEntity target = doll.getTarget();
        if (target == null || !target.isAlive()) return;

        // 从数据管理器获取策略（已缓存）
        ICombatStrategy strategy = doll.getDataManager().getCurrentStrategy();
        if (strategy != null) {
            strategy.tick(doll, target, doll.getOwner());
        }
    }

    /**
     * 重置当前策略（当人偶重新进入战斗状态时调用）
     */
    public void resetStrategy() {
        ICombatStrategy strategy = doll.getDataManager().getCurrentStrategy();
        if (strategy != null) {
            strategy.reset();
        }
    }
}