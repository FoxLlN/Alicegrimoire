package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.aliceGrimoire.alicegrimoire.client.AliceGeoModel;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModItems;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class DollNetItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DollNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 网兜目前无动画
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<DollNetItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new AliceGeoModel<>(
                            "geo/doll_net.geo.json",
                            "textures/item/doll_net.png",
                            "animations/doll_net.animation.json"  // ← 空占位符
                    ));
                }
                return renderer;
            }
        });
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof DollEntity doll) {
            if (!player.level().isClientSide) {
                // 1. 创建人偶物品
                ItemStack dollItem = new ItemStack(ModItems.DOLL.get());

                // 2. 保存完整实体 NBT（包含血量、护甲、装备、颜色、战斗参数等一切）
                CompoundTag entityTag = new CompoundTag();
                doll.saveWithoutId(entityTag);
                entityTag.putString("id", doll.getEncodeId());
                entityTag.remove("UUID");
                dollItem.set(DataComponents.ENTITY_DATA, CustomData.of(entityTag));

                // 3. 同步保存职业（让 DollItem.use 召唤时不会覆盖为标准）
                DollJobType job = doll.getDollData().getJobType();
                dollItem.set(ModDataComponents.DOLL_TYPE.get(), job);

                // 4. 同步保存改装组件列表（让织魔台 GUI 能正确显示周围 8 格）
                List<ItemStack> comps = doll.getDollData().getComponents();
                if (comps != null && !comps.isEmpty()) {
                    dollItem.set(ModDataComponents.COMPONENTS.get(), comps);
                } else {
                    // 如果没有组件，设为空列表（避免残留旧数据）
                    dollItem.set(ModDataComponents.COMPONENTS.get(), List.of());
                }

                // 5. 放入玩家背包或掉落
                if (!player.getInventory().add(dollItem)) {
                    player.spawnAtLocation(dollItem);
                }
                
                // 6. 销毁原实体
                target.discard();
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }
}