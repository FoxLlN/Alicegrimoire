package org.aliceGrimoire.alicegrimoire.modifier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 改装管理器
 * 存储所有物品的修正定义，提供查询接口
 */
public class ModifierManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifierManager.class);
    private static final Map<Item, ModifierDefinition> MODIFIERS = new ConcurrentHashMap<>();

    public static void register(Item item, ModifierDefinition definition) {
        MODIFIERS.put(item, definition);
        LOGGER.debug("Registered modifier for {}: {}", item.getDescriptionId(), definition.stats());
    }

    public static void clear() {
        MODIFIERS.clear();
    }

    /**
     * 获取某个物品对某个属性的修正值（已乘以堆叠数量）
     * @param stack 物品堆（包含数量）
     * @param attribute 属性名，如 "max_health", "attack_damage"
     * @return 修正后的总值（stack.count * 定义值）
     */
    public static double getModifiedValue(ItemStack stack, String attribute) {
        if (stack.isEmpty()) return 0.0;
        ModifierDefinition def = MODIFIERS.get(stack.getItem());
        if (def == null) return 0.0;
        Double base = def.stats().get(attribute);
        if (base == null) return 0.0;
        // 策划案：组件可以堆叠，数量影响效果
        return base * stack.getCount();
    }

    /**
     * 检查物品是否有任何修正属性
     */
    public static boolean hasModifiers(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ModifierDefinition def = MODIFIERS.get(stack.getItem());
        return def != null && !def.stats().isEmpty();
    }

    public static int getSize() { return MODIFIERS.size(); }
}