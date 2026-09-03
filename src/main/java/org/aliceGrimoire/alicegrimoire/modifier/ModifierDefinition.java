package org.aliceGrimoire.alicegrimoire.modifier;

import java.util.Map;

/**
 * 单个物品的修正定义
 * 例如：泥土 -> {"max_health": 2.0}
 * 数据包 JSON 中，key 为物品 ID，value 为属性-数值映射
 */
public record ModifierDefinition(Map<String, Double> stats) {
    // 空的修正（返回 0）
    public static final ModifierDefinition EMPTY = new ModifierDefinition(Map.of());
}