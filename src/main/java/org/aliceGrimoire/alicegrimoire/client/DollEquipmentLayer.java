package org.aliceGrimoire.alicegrimoire.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DollEquipmentLayer extends GeoRenderLayer<DollEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DollEquipmentLayer.class);

    private final ItemRenderer itemRenderer;

    public DollEquipmentLayer(GeoRenderer<DollEntity> renderer, ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, DollEntity animatable, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        // 从同步数据获取物品
        ItemStack mainHand = animatable.getSyncMainHand();
        ItemStack offHand = animatable.getSyncOffHand();

        if (!mainHand.isEmpty()) {
            renderItemOnBone(poseStack, bufferSource, packedLight, model, "rightHand", mainHand, animatable);
        }

        if (!offHand.isEmpty()) {
            renderItemOnBone(poseStack, bufferSource, packedLight, model, "leftHand", offHand, animatable);
        }
    }

    /**
     * 递归累加父级骨骼的 pivot（位置）和旋转（角度）
     */
    private void accumulateBoneTransform(GeoBone bone, PoseStack poseStack) {
        if (bone == null) return;

        // 递归处理父级（先父后子，确保变换顺序正确）
        if (bone.getParent() != null) {
            accumulateBoneTransform(bone.getParent(), poseStack);
        }

        // 应用当前骨骼的变换（位置 + 旋转）
        float px = bone.getPivotX();
        float py = bone.getPivotY();
        float pz = bone.getPivotZ();
        float rx = bone.getRotX();
        float ry = bone.getRotY();
        float rz = bone.getRotZ();

        poseStack.translate(px / 16.0, py / 16.0, pz / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(rx));
        poseStack.mulPose(Axis.YP.rotationDegrees(ry));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
    }

    private void renderItemOnBone(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                  BakedGeoModel model, String boneName,
                                  ItemStack stack, DollEntity entity) {

        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone == null) {
            LOGGER.warn("Bone '{}' not found in model!", boneName);
            return;
        }

        poseStack.pushPose();

        // === 累加所有父级骨骼的变换（包括位置和旋转） ===
        accumulateBoneTransform(bone, poseStack);

        // === 偏移到手持位置（相对于骨骼的局部偏移） ===
        float offsetX = boneName.equals("rightHand") ? 0.25f : -0.25f;
        float offsetZ = -0.3f;
        poseStack.translate(offsetX / 16.0, 0, offsetZ / 16.0);

        // === 物品自身缩放和旋转 ===
        poseStack.scale(0.4f, 0.4f, 0.4f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));

        // === 渲染物品 ===
        itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            entity.level(),
            entity.getId()
        );

        poseStack.popPose();
    }
}