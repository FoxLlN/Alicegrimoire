package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import org.aliceGrimoire.alicegrimoire.client.AliceHandheldRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class MagiweaverItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MagiweaverItem(Block block, Properties properties) {
        super(block, properties);
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
        AliceHandheldRenderer.register(consumer, "geo/magiweaver.geo.json", "textures/block/magiweaver.png", "animations/magiweaver.animation.json");
    }
}
