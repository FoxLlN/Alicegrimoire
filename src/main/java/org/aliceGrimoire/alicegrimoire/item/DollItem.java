package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollType;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModEntities;
import org.aliceGrimoire.alicegrimoire.client.DollModel;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class DollItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DollItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<DollItem> renderer = null;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<>(new DollModel<>());
                }
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            InteractionResult result = super.useOn(context);
            if (result == InteractionResult.SUCCESS) {
                // Set type to block entity
                BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
                if (be instanceof org.aliceGrimoire.alicegrimoire.block.DollBlockEntity dollBe) {
                    dollBe.setDollType(context.getItemInHand().getOrDefault(ModDataComponents.DOLL_TYPE, DollType.STANDARD));
                }
            }
            return result;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                DollEntity doll = ModEntities.DOLL.get().create(level);
                if (doll != null) {
                    CustomData entityData = itemstack.get(DataComponents.ENTITY_DATA);
                    if (entityData != null) {
                        doll.load(entityData.copyTag());
                    }
                    
                    // Set type from component
                    doll.setDollType(itemstack.getOrDefault(ModDataComponents.DOLL_TYPE, DollType.STANDARD));
                    
                    doll.setOwnerUUID(player.getUUID());
                    doll.moveTo(player.getX(), player.getEyeY(), player.getZ(), player.getYRot(), player.getXRot());
                    
                    Vec3 look = player.getLookAngle();
                    doll.setDeltaMovement(look.scale(1.5D));
                    doll.setEvokeTime(level.getGameTime());
                    
                    level.addFreshEntity(doll);
                    if (!player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
        }
        return InteractionResultHolder.pass(itemstack);
    }
}
