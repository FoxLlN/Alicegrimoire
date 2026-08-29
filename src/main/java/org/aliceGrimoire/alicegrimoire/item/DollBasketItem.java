package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.aliceGrimoire.alicegrimoire.client.AliceGeoModel;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.event.PlayerMoveDetector;
import org.aliceGrimoire.alicegrimoire.item.string.StringHelper;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModEntities;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class DollBasketItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DollBasketItem(Properties properties) {
        super(properties);
    }

    // ========== GeoItem 接口实现 ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 若后续需要动画，可在此添加；目前无动画，留空
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<DollBasketItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new AliceGeoModel<>(
                            "geo/doll_basket.geo.json",          // 模型文件
                            "textures/item/doll_basket.png",     // 纹理文件
                            "animations/doll_basket.animation.json" // 动画文件（可空占位）
                    ));
                }
                return renderer;
            }
        });
    }

    // ========== 原有业务逻辑保持不变 ==========

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack basket = player.getItemInHand(hand);
        List<ItemStack> dolls = basket.get(ModDataComponents.DOLLS.get());

        if (dolls == null || dolls.isEmpty()) {
            return InteractionResultHolder.pass(basket);
        }

        if (!level.isClientSide) {
            boolean hasString = StringHelper.hasStringEquipped(player);

            // 遍历每个人偶，逐个生成
            for (ItemStack dollStack : dolls) {
                DollEntity doll = ModEntities.DOLL.get().create(level);
                if (doll == null) continue;

                // 加载NBT数据
                CustomData entityData = dollStack.get(DataComponents.ENTITY_DATA);
                if (entityData != null) {
                    doll.load(entityData.copyTag());
                }

                // 设置所有者
                doll.setOwnerUUID(player.getUUID());
                doll.setPlayerMoving(PlayerMoveDetector.getPlayerMoving(player.getUUID()));
                doll.moveTo(player.getX(), player.getEyeY(), player.getZ(), player.getYRot(), player.getXRot());

                // 判断是否拴住（仅在穿丝线且当前仍有空余名额时）
                boolean tethered = false;
                if (hasString) {
                    tethered = StringHelper.canTetherMore(player);  // 实时检查剩余名额
                }
                doll.setTethered(tethered);

                // 随机偏移 + 唤起动画
                Vec3 look = player.getLookAngle();
                double rx = (level.random.nextDouble() - 0.5) * 0.5;
                double ry = (level.random.nextDouble() - 0.5) * 0.5;
                double rz = (level.random.nextDouble() - 0.5) * 0.5;
                doll.setDeltaMovement(look.scale(1.5D).add(rx, ry, rz));
                doll.setEvokeTime(level.getGameTime());

                level.addFreshEntity(doll);
            }

            basket.set(ModDataComponents.DOLLS.get(), List.of()); // 清空篮子
        }

        return InteractionResultHolder.sidedSuccess(basket, level.isClientSide());
    }

}
