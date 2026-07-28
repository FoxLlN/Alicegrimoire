package org.aliceGrimoire.alicegrimoire.entity.doll.equipment.strategy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollSlots;
import org.aliceGrimoire.alicegrimoire.entity.doll.equipment.IEquipmentStrategy;

public class SpecialItemStrategy implements IEquipmentStrategy {

    @Override
    public boolean appliesTo(ItemStack stack) {
        Item item = stack.getItem();
        // 策划案中的特殊武器
        return item == Items.TNT ||
               item == Items.TNT_MINECART ||
               item == Items.FIRE_CHARGE ||
               item == Items.FLINT_AND_STEEL ||
               item == Items.SCULK_SHRIEKER ||
               // 药水（喷溅/滞留）
               item instanceof net.minecraft.world.item.SplashPotionItem ||
               item instanceof net.minecraft.world.item.LingeringPotionItem;
    }

    @Override
    public int getTargetSlot(ItemStack stack) {
        return DollSlots.MAIN_HAND;
    }

    @Override
    public int getPriority(ItemStack stack) {
        // 特殊武器优先级最高（TNT/尖啸体）
        Item item = stack.getItem();
        if (item == Items.SCULK_SHRIEKER) return 100;
        if (item == Items.TNT || item == Items.TNT_MINECART) return 90;
        if (item == Items.FIRE_CHARGE) return 80;
        return 60;
    }

    @Override
    public void onEquip(DollEntity doll, ItemStack stack, int slot) {
        // 特殊武器装备时触发：可添加警示效果
    }
}