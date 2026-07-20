package org.aliceGrimoire.alicegrimoire.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.aliceGrimoire.alicegrimoire.block.DollBlockEntity;
import org.aliceGrimoire.alicegrimoire.block.MagiweaverBlockEntity;
import org.aliceGrimoire.alicegrimoire.registry.ModBlockEntities;
import org.aliceGrimoire.alicegrimoire.registry.ModEntities;
import org.aliceGrimoire.alicegrimoire.registry.ModMenuTypes;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);

    private static int calculateAmbientLight(Level level, BlockPos center) {
        BlockPos[] samplePositions = {
            center,
            center.above(),
            center.north(),
            center.south(),
            center.west(),
            center.east()
        };
        
        int maxSky = 0;
        int maxBlock = 0;
        
        for (BlockPos pos : samplePositions) {
            int sky = level.getBrightness(LightLayer.SKY, pos);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            if (sky > maxSky) maxSky = sky;
            if (block > maxBlock) maxBlock = block;
        }
        
        if (maxSky == 0 && maxBlock == 0) {
            maxSky = 8;
            maxBlock = 8;
        }
        
        return (maxSky << 20) | (maxBlock << 4);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DOLL.get(), (context) -> new DollRenderer(context));

        // ==================== Doll 方块渲染器 ====================
        event.registerBlockEntityRenderer(ModBlockEntities.DOLL.get(), context -> new GeoBlockRenderer<DollBlockEntity>(new DollModel<>()) {
            @Override
            public void render(DollBlockEntity entity, float partialTick, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int overlay) {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    var pos = entity.getBlockPos();
                    int sky = level.getBrightness(LightLayer.SKY, pos);
                    int block = level.getBrightness(LightLayer.BLOCK, pos);
                    packedLight = (sky << 20) | (block << 4);
                }
                super.render(entity, partialTick, poseStack, bufferSource, packedLight, overlay);
            }

            @Override
            public RenderType getRenderType(DollBlockEntity animatable, ResourceLocation texture,
                                            MultiBufferSource bufferSource, float partialTick) {
                // 修复透视问题：使用 entityCutout（支持深度测试和剔除）
                return RenderType.entityCutout(texture);
            }
        });

        // ==================== Magiweaver 方块渲染器 ====================
        event.registerBlockEntityRenderer(ModBlockEntities.MAGIWEAVER.get(), context -> new GeoBlockRenderer<MagiweaverBlockEntity>(new AliceGeoModel<>(
                "geo/magiweaver.geo.json",
                "textures/block/magiweaver.png",
                "animations/magiweaver.animation.json"
        )) {
            @Override
            public void render(MagiweaverBlockEntity entity, float partialTick, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int overlay) {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    var pos = entity.getBlockPos();
                    packedLight = calculateAmbientLight(level, pos);
                }
                super.render(entity, partialTick, poseStack, bufferSource, packedLight, overlay);
            }

            @Override
            public RenderType getRenderType(MagiweaverBlockEntity animatable, ResourceLocation texture,
                                            MultiBufferSource bufferSource, float partialTick) {
                // 修复透视问题：使用 entitySolid（不透明实体模型）
                return RenderType.entityCutout(texture);
            }
        });
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MAGIWEAVER.get(), MagiweaverScreen::new);
    }
}