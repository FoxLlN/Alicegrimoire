package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.registry.ModBlocks;
import org.aliceGrimoire.alicegrimoire.registry.ModEntities;

import java.util.List;

/**
 * 破损人偶物品
 * 人偶死亡时掉落此物品，包含人偶所有数据
 * 可在织魔台修复为正常人偶
 */
public class BrokenDollItem extends DollItem {

    public BrokenDollItem(Properties properties) {
        super(ModBlocks.DOLL.get(), properties);
    }

    // ===== 重写物品名称 =====
    @Override
    public String getDescriptionId() {
        return "item.alicegrimoire.broken_doll";
    }

    /**
     * 从破损人偶物品恢复为正常 DollEntity
     * 放在织魔台中间时调用
     */
    public static DollEntity restoreFromItem(Level level, ItemStack stack) {
        if (!(stack.getItem() instanceof BrokenDollItem)) return null;

        CustomData entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData == null) return null;

        DollEntity doll = ModEntities.DOLL.get().create(level);
        if (doll == null) return null;

        // 加载保存的 NBT 数据
        CompoundTag tag = entityData.copyTag();
        doll.load(tag);

        // 清除破损标记（修复完成）
        doll.setBroken(false);

        // 恢复生命值
        doll.setHealth(doll.getMaxHealth());

        return doll;
    }

    /**
     * 检查物品是否为破损状态
     */
    public static boolean isBroken(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof BrokenDollItem) return true;

        // 如果是普通 DollItem，检查其 NBT 中是否有破损标记
        if (stack.getItem() instanceof DollItem) {
            CustomData data = stack.get(DataComponents.ENTITY_DATA);
            if (data != null) {
                return data.copyTag().getBoolean("IsBroken");
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 破损人偶不能直接召唤，提示玩家需要修复
        if (!level.isClientSide) {
            player.displayClientMessage(
                Component.translatable("message.alicegrimoire.broken_doll_cannot_summon"),
                true
            );
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // 显示破损提示
        tooltipComponents.add(Component.translatable("tooltip.alicegrimoire.broken_doll"));

        // 尝试显示原人偶信息
        CustomData entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData != null) {
            CompoundTag tag = entityData.copyTag();
            if (tag.contains("DollData")) {
                CompoundTag dollDataTag = tag.getCompound("DollData");
                if (dollDataTag.contains("JobType")) {
                    String jobType = dollDataTag.getString("JobType");
                    tooltipComponents.add(Component.translatable(
                        "tooltip.alicegrimoire.broken_doll.former_job",
                        Component.translatable("doll_type.alicegrimoire." + jobType.toLowerCase())
                    ));
                }
                if (dollDataTag.contains("MaxHealth")) {
                    double health = dollDataTag.getDouble("MaxHealth");
                    tooltipComponents.add(Component.translatable(
                        "tooltip.alicegrimoire.broken_doll.former_health",
                        String.format("%.1f", health)
                    ));
                }
            }
        }

        tooltipComponents.add(Component.translatable("tooltip.alicegrimoire.broken_doll.repair_hint"));
    }
}