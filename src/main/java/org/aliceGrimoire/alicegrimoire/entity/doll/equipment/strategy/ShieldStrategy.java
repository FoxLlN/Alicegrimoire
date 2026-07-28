package org.aliceGrimoire.alicegrimoire.entity.doll.equipment.strategy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollSlots;
import org.aliceGrimoire.alicegrimoire.entity.doll.equipment.IEquipmentStrategy;

public class ShieldStrategy implements IEquipmentStrategy {

    @Override
    public boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem;
    }

    @Override
    public int getTargetSlot(ItemStack stack) {
        return DollSlots.OFF_HAND;
    }

    @Override
    public int getPriority(ItemStack stack) {
        // 盾牌优先级较高，但低于特殊武器
        return 50;
    }

    @Override
    public void onEquip(DollEntity doll, ItemStack stack, int slot) {
        // 盾牌装备时触发：可添加音效或粒子效果
        // 后续守御人偶可自动举盾
    }
}