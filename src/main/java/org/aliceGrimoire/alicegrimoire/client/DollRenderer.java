package org.aliceGrimoire.alicegrimoire.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DollRenderer extends GeoEntityRenderer<DollEntity> {
    public DollRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DollModel<>());
    }

    @Override
    public void render(DollEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // 5、唤起时快速自转三圈
        long evokeTime = entity.getEvokeTime();
        if (evokeTime > 0) {
            long age = entity.level().getGameTime() - evokeTime;
            if (age < 20) { // 持续1秒
                float progress = (age + partialTick) / 20.0f;
                float rotation = progress * 360.0f * 3.0f;
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            }
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        // 7、视觉上的表现形式类似于原版的拴绳
        LivingEntity owner = entity.getOwner();
        if (owner != null && isTethered(owner)) {
            // 指令 9：高亮的视觉效果只对玩家自己有效，其他玩家看不见
            net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (localPlayer != null && localPlayer.getUUID().equals(owner.getUUID())) {
                renderLeash(entity, partialTick, poseStack, bufferSource, owner, packedLight);
            }
        }
        poseStack.popPose();
    }

    private boolean isTethered(LivingEntity owner) {
        return !owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).isEmpty() && 
               owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).getItem() instanceof org.aliceGrimoire.alicegrimoire.item.DollStringItem;
    }

    private <E extends Entity> void renderLeash(DollEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, E owner, int packedLight) {
        poseStack.pushPose();
        double d0 = (double)(Mth.lerp(partialTick, owner.yRotO, owner.getYRot()) * ((float)Math.PI / 180F));
        double d1 = (double)(Mth.lerp(partialTick, owner.xRotO, owner.getXRot()) * ((float)Math.PI / 180F));
        double d2 = Math.cos(d0);
        double d3 = Math.sin(d0);
        double d4 = Math.sin(d1);
        double d5 = Math.cos(d1);
        
        double ownerX = Mth.lerp(partialTick, owner.xo, owner.getX()) - d3 * 0.7D - d2 * 0.5D * d5;
        double ownerY = Mth.lerp(partialTick, owner.yo + (double)owner.getEyeHeight() * 0.7D, owner.getY() + (double)owner.getEyeHeight() * 0.7D) - d4 * 0.5D - 0.25D;
        double ownerZ = Mth.lerp(partialTick, owner.zo, owner.getZ()) + d2 * 0.7D - d3 * 0.5D * d5;
        
        double entityX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double entityZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        
        float dx = (float)(ownerX - entityX);
        float dy = (float)(ownerY - entityY);
        float dz = (float)(ownerZ - entityZ);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.leash());
        var matrix4f = poseStack.last().pose();

        // 指令 9/10：高亮颜色逻辑
        int r = 255, g = 255, b = 255; // 默认白色

        if (entity.isInsideBlock()) {
            r = 255; g = 0; b = 0; // 红色（卡在方块内）
        } else if (entity.getObstructedTicks() > 0) {
            // 根据阻塞程度渐变：黄色 -> 橙色 -> 红色
            int ticks = entity.getObstructedTicks();
            float progress = Math.min(ticks / 60.0f, 1.0f); // 60 tick = 3秒
            r = 255;
            g = (int)(255 * (1 - progress));
            b = (int)(255 * (1 - progress * 0.8f));
        } else {
            r = 255; g = 255; b = 255; // 白色（畅通）
        }

        for(int i = 0; i <= 24; ++i) {
            float f = (float)i / 24.0F;
            vertexConsumer.addVertex(matrix4f, dx * f, dy * (f * f + f) * 0.5F + 0.1F, dz * f)
                          .setColor(r, g, b, 255)
                          .setLight(packedLight);
        }

        poseStack.popPose();
    }
}
