package org.aliceGrimoire.alicegrimoire.entity.doll.equipment;

import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 装备策略接口
 * 决定某个物品应该装备到主手还是副手，以及是否允许替换
 */
public interface IEquipmentStrategy {

    /**
     * 判断此策略是否适用于该物品
     */
    boolean appliesTo(ItemStack stack);

    /**
     * 获取目标槽位
     * @return DollSlots.MAIN_HAND 或 DollSlots.OFF_HAND
     */
    int getTargetSlot(ItemStack stack);

    /**
     * 判断是否应该替换槽位中已有的物品
     * @param existing 当前槽位的物品
     * @param candidate 待装备的物品
     * @return true 表示替换
     */
    default boolean shouldReplace(ItemStack existing, ItemStack candidate) {
        // 默认：槽位为空则替换，否则比较优先级
        if (existing.isEmpty()) return true;
        return getPriority(candidate) > getPriority(existing);
    }

    /**
     * 获取物品优先级（数字越大越优先）
     * 用于处理同槽位多个可装备物品的冲突
     */
    default int getPriority(ItemStack stack) {
        return 0; // 子类覆盖
    }

    /**
     * 装备后的回调（可用于触发效果、音效等）
     */
    default void onEquip(DollEntity doll, ItemStack stack, int slot) {}

    /**
     * 策略名称（用于调试）
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}