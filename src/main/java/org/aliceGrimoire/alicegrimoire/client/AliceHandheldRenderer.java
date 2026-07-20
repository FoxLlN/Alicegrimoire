package org.aliceGrimoire.alicegrimoire.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Consumer;

/**
 * 通用的 GeckoLib 手持物品渲染器辅助类
 */
public class AliceHandheldRenderer {

    public static <T extends Item & GeoItem> void register(Consumer<IClientItemExtensions> consumer, String model, String texture, String animation) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<T> renderer = null;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeoItemRenderer<>(new AliceGeoModel<>(model, texture, animation));
                }
                return this.renderer;
            }
        });
    }
}
