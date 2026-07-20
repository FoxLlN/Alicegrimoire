package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
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
        
        if (dolls != null && !dolls.isEmpty()) {
            if (!level.isClientSide) {
                for (ItemStack dollStack : dolls) {
                    DollEntity doll = ModEntities.DOLL.get().create(level);
                    if (doll != null) {
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
                }
                // Empty the basket
                basket.set(ModDataComponents.DOLLS.get(), new ArrayList<>());
            }
            return InteractionResultHolder.sidedSuccess(basket, level.isClientSide());
        }
        
        return InteractionResultHolder.pass(basket);
    }
}
