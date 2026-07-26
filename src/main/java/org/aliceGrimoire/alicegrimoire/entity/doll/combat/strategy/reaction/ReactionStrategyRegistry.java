package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction;

import org.aliceGrimoire.alicegrimoire.entity.doll.data.DamageReactionType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 伤害反应策略注册器
 * 管理所有可用的反应策略，支持通过枚举类型获取
 * 可通过织魔台改装切换
 */
public class ReactionStrategyRegistry {
    
    private static final Map<DamageReactionType, Supplier<IDamageReactionStrategy>> REGISTRY = new EnumMap<>(DamageReactionType.class);
    
    static {
        // 注册所有可用策略
        REGISTRY.put(DamageReactionType.DEFAULT, DefaultReactionStrategy::new);
        // 未来扩展：注册预判闪避
        REGISTRY.put(DamageReactionType.PREEMPTIVE, PreemptiveReactionStrategy::new);
        // REGISTRY.put(DamageReactionType.TANK, TankReactionStrategy::new);
    }
    
    /**
     * 根据类型创建策略实例
     */
    public static IDamageReactionStrategy create(DamageReactionType type) {
        Supplier<IDamageReactionStrategy> supplier = REGISTRY.get(type);
        if (supplier == null) {
            // 默认使用 DEFAULT
            return new DefaultReactionStrategy();
        }
        return supplier.get();
    }
    
    /**
     * 获取所有可用的策略类型（用于织魔台显示）
     */
    public static DamageReactionType[] getAvailableTypes() {
        return REGISTRY.keySet().toArray(new DamageReactionType[0]);
    }
    
    /**
     * 检查策略是否已注册
     */
    public static boolean isRegistered(DamageReactionType type) {
        return REGISTRY.containsKey(type);
    }
}