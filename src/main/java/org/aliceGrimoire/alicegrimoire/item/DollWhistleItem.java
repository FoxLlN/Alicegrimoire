package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

import java.util.List;

public class DollWhistleItem extends Item {
    public DollWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class, player.getBoundingBox().inflate(32.0D),
                doll -> player.getUUID().equals(doll.getOwnerUUID()) && doll.isTethered(player));
            
            if (dolls.isEmpty()) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            // 检查当前状态，如果有一个是返回状态，就全部切回工作状态；否则全部切为返回状态
            boolean anyReturning = false;
            for (DollEntity doll : dolls) {
                if (doll.isReturning()) {
                    anyReturning = true;
                    break;
                }
            }

            boolean newState = !anyReturning;
            for (DollEntity doll : dolls) {
                doll.setReturning(newState);
            }

            if (newState) {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_returning"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_working"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
