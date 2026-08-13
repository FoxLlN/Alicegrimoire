package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.item.string.StringHelper;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;

public class DollBasketItem extends Item {
    public DollBasketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack basket = player.getItemInHand(hand);
        List<ItemStack> dolls = basket.get(ModDataComponents.DOLLS.get());

        if (dolls == null || dolls.isEmpty()) {
            return InteractionResultHolder.pass(basket);
        }

        if (!level.isClientSide) {
            int totalInBasket = dolls.size();

            // 检查是否装备了丝线
            boolean hasString = StringHelper.hasStringEquipped(player);

            if (hasString) {
                // ===== 穿着丝线：检查上限 =====
                int availableSlots = StringHelper.getAvailableSlots(player);

                if (availableSlots <= 0) {
                    // 没有空位，直接提示，一个也不生成
                    player.displayClientMessage(
                        Component.translatable("message.alicegrimoire.doll_max_tethered_full"),
                        true
                    );
                    return InteractionResultHolder.fail(basket);
                }

                if (totalInBasket > availableSlots) {
                    // 篮子里的数量超过可用槽位，提示，一个也不生成
                    player.displayClientMessage(
                        Component.translatable("message.alicegrimoire.doll_max_tethered_batch_exceed",
                            availableSlots, totalInBasket),
                        true
                    );
                    return InteractionResultHolder.fail(basket);
                }

                // 数量足够，继续生成（全部生成后会被 tick 自动拴住）
            }

            // ===== 执行生成（不穿丝线 或 穿丝线且数量足够） =====
            for (ItemStack dollStack : dolls) {
                DollEntity doll = ModEntities.DOLL.get().create(level);
                if (doll == null) continue;

                CustomData entityData = dollStack.get(DataComponents.ENTITY_DATA);
                if (entityData != null) {
                    doll.load(entityData.copyTag());
                }
                doll.setOwnerUUID(player.getUUID());
                doll.moveTo(player.getX(), player.getEyeY(), player.getZ(), player.getYRot(), player.getXRot());
                    
                Vec3 look = player.getLookAngle();
                // Add random offset as requested
                double rx = (level.random.nextDouble() - 0.5) * 0.5;
                double ry = (level.random.nextDouble() - 0.5) * 0.5;
                double rz = (level.random.nextDouble() - 0.5) * 0.5;
                        
                doll.setDeltaMovement(look.scale(1.5D).add(rx, ry, rz));
                doll.setEvokeTime(level.getGameTime());
                level.addFreshEntity(doll);
            }

            // 清空篮子
            basket.set(ModDataComponents.DOLLS.get(), new ArrayList<>());
        }

        return InteractionResultHolder.sidedSuccess(basket, level.isClientSide());
    }
}
