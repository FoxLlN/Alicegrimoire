package org.aliceGrimoire.alicegrimoire.entity.doll.equipment.strategy;

import net.minecraft.world.item.*;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollSlots;
import org.aliceGrimoire.alicegrimoire.entity.doll.equipment.IEquipmentStrategy;

public class WeaponStrategy implements IEquipmentStrategy {

    @Override
    public boolean appliesTo(ItemStack stack) {
        Item item = stack.getItem();
        // 检测各种武器类型
        return item instanceof SwordItem ||
               item instanceof BowItem ||
               item instanceof CrossbowItem ||
               item instanceof TridentItem ||
               // 兼容其他模组的武器（通过标签检测）
               stack.is(net.minecraft.tags.ItemTags.SWORDS);
    }

    @Override
    public int getTargetSlot(ItemStack stack) {
        return DollSlots.MAIN_HAND;
    }

    @Override
    public int getPriority(ItemStack stack) {
        // 不同武器优先级略有不同，三叉戟 > 剑 > 弓
        Item item = stack.getItem();
        if (item instanceof TridentItem) return 40;
        if (item instanceof SwordItem) return 30;
        if (item instanceof CrossbowItem) return 25;
        if (item instanceof BowItem) return 20;
        return 10;
    }

    @Override
    public void onEquip(DollEntity doll, ItemStack stack, int slot) {
        // 武器装备时触发：刷新策略（已有）
        // 可添加音效
    }
}