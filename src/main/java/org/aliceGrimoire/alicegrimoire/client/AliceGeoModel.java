package org.aliceGrimoire.alicegrimoire.client;

import net.minecraft.resources.ResourceLocation;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class AliceGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final ResourceLocation modelResource;
    private final ResourceLocation textureResource;
    private final ResourceLocation animationResource;

    public AliceGeoModel(String modelPath, String texturePath, String animationPath) {
        this.modelResource = ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, modelPath);
        this.textureResource = ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, texturePath);
        this.animationResource = ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, animationPath);
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return modelResource;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animationResource;
    }
}
