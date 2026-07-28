package org.aliceGrimoire.alicegrimoire.entity.doll.equipment;

import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.entity.doll.equipment.strategy.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备策略注册中心
 * 管理所有策略，按优先级排序
 * 新增策略只需在这里注册即可
 */
public class EquipmentRegistry {

    private static final List<IEquipmentStrategy> STRATEGIES = new ArrayList<>();

    static {
        // 注册所有策略（顺序决定匹配优先级，先注册的先匹配）
        // 注意：特殊武器应优先匹配，因为其判断条件可能较宽泛
        register(new ShieldStrategy());      // 盾牌 → 副手
        register(new SpecialItemStrategy()); // 特殊武器 → 主手
        register(new WeaponStrategy());      // 普通武器 → 主手
        // 未来新增策略在此注册：
        // register(new ToolStrategy());     // 工具 → 主手
        // register(new FoodStrategy());     // 食物 → 副手（待定）
    }

    public static void register(IEquipmentStrategy strategy) {
        STRATEGIES.add(strategy);
    }

    /**
     * 根据物品查找匹配的策略（按注册顺序）
     */
    public static IEquipmentStrategy findStrategy(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (IEquipmentStrategy strategy : STRATEGIES) {
            if (strategy.appliesTo(stack)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * 获取所有策略（用于调试）
     */
    public static List<IEquipmentStrategy> getStrategies() {
        return new ArrayList<>(STRATEGIES);
    }
}