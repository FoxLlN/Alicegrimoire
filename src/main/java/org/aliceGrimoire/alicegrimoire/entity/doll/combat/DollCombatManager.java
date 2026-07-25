package org.aliceGrimoire.alicegrimoire.entity.doll.combat;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType;

import java.util.HashMap;
import java.util.Map;

/**
 * 战斗管理器
 * 根据人偶的职业类型（DollJobType）和武器类型动态选择战斗策略
 */
public class DollCombatManager {
    private final DollEntity doll;
    private final Map<DollJobType, ICombatStrategy> strategyCache = new HashMap<>();

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
     * 强制刷新策略（当职业或武器变更时调用）
     */
    public void refreshStrategy() {
        // 由 DollDataManager 负责刷新
    }
}