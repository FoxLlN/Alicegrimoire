package org.aliceGrimoire.alicegrimoire.entity.doll.equipment;

import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollSlots;

/**
 * 人偶装备处理器
 * 负责自动装备逻辑，主副手物品的管理和交互
 */
public class DollEquipmentHandler {

    private final DollEntity doll;

    public DollEquipmentHandler(DollEntity doll) {
        this.doll = doll;
    }

    /**
     * 尝试自动装备一个物品
     * 根据策略决定放入主手还是副手
     */
    public boolean tryEquip(ItemStack stack) {
        if (stack.isEmpty()) return false;

        IEquipmentStrategy strategy = EquipmentRegistry.findStrategy(stack);
        if (strategy == null) return false;

        int targetSlot = strategy.getTargetSlot(stack);
        ItemStack current = doll.getDollData().getItem(targetSlot);

        if (!strategy.shouldReplace(current, stack)) {
            return false;
        }

        // 执行装备（只装备一个）
        ItemStack equipped = stack.copy();
        equipped.setCount(1);
        doll.getDataManager().setItem(targetSlot, equipped);

        strategy.onEquip(doll, equipped, targetSlot);
        doll.syncEquipmentToClient();

        return true;
    }

    /**
     * 强制装备到指定槽位
     */
    public void equipToSlot(int slot, ItemStack stack) {
        if (!DollSlots.isValidSlot(slot)) return;
        ItemStack toEquip = stack.copy();
        toEquip.setCount(1);
        doll.getDataManager().setItem(slot, toEquip);
        doll.syncEquipmentToClient();
    }

    public void clearSlot(int slot) {
        if (!DollSlots.isValidSlot(slot)) return;
        doll.getDataManager().setItem(slot, ItemStack.EMPTY);
        doll.syncEquipmentToClient();
    }

    public ItemStack getMainHand() {
        return doll.getDollData().getItem(DollSlots.MAIN_HAND);
    }

    public ItemStack getOffHand() {
        return doll.getDollData().getItem(DollSlots.OFF_HAND);
    }

    public boolean hasWeapon() {
        return !getMainHand().isEmpty();
    }

    public boolean hasShield() {
        return !getOffHand().isEmpty() &&
               getOffHand().getItem() instanceof net.minecraft.world.item.ShieldItem;
    }
}