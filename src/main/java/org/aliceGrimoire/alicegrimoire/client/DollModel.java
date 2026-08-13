package org.aliceGrimoire.alicegrimoire.client;

import net.minecraft.resources.ResourceLocation;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class DollModel<T extends GeoAnimatable> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        // 所有人偶物品模型全部都使用dolls.geo.json模型，也就是说物品、实体、方块共用一个模型
        return ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "geo/dolls.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        // 默认使用 textures/doll.png
        return ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "textures/doll.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "animations/doll.animation.json");
    }
}
