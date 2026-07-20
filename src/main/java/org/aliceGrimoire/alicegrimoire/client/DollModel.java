package org.aliceGrimoire.alicegrimoire.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.block.DollBlockEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollType;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
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

    private DollType getDollType(T animatable) {
        if (animatable instanceof DollEntity doll) {
            return doll.getDollType();
        }
        if (animatable instanceof DollBlockEntity be) {
            return be.getDollType();
        }
        // 对于物品，GeckoLib 4.x 会将 ItemStack 关联到渲染上下文中
        // 这里暂时返回标准类型，稍后我们会处理物品的特殊逻辑
        return DollType.STANDARD;
    }
}
