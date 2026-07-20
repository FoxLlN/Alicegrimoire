package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.registry.ModItems;

public class DollNetItem extends Item {
    public DollNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof DollEntity doll) {
            if (!player.level().isClientSide) {
                ItemStack dollItem = new ItemStack(ModItems.DOLL.get());
                CompoundTag entityTag = new CompoundTag();
                doll.saveWithoutId(entityTag);
                entityTag.putString("id", doll.getEncodeId());
                entityTag.remove("UUID");
                dollItem.set(DataComponents.ENTITY_DATA, CustomData.of(entityTag));
                
                if (!player.getInventory().add(dollItem)) {
                    player.spawnAtLocation(dollItem);
                }
                target.discard();
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }
}
