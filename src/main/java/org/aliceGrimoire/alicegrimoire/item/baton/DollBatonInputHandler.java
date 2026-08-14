package org.aliceGrimoire.alicegrimoire.item.baton;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.network.DollBatonLeftClickPacket;
import org.aliceGrimoire.alicegrimoire.network.DollBatonRightClickPacket;
import org.aliceGrimoire.alicegrimoire.network.PacketHandler;

@EventBusSubscriber(modid = Alicegrimoire.MODID, value = Dist.CLIENT)
public class DollBatonInputHandler {

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean isBaton = mainHand.getItem() instanceof DollBatonItem || offHand.getItem() instanceof DollBatonItem;
        if (!isBaton) return;

        KeyMapping key = event.getKeyMapping();
        boolean shiftDown = player.isShiftKeyDown();

        if (key == Minecraft.getInstance().options.keyAttack) {
            // 左键始终拦截（指挥棒攻击指令）
            event.setCanceled(true);
            PacketHandler.sendToServer(new DollBatonLeftClickPacket(shiftDown));
        } else if (key == Minecraft.getInstance().options.keyUse) {
            if (shiftDown) {
                // Shift+右键：始终拦截（解除所有）
                event.setCanceled(true);
                PacketHandler.sendToServer(new DollBatonRightClickPacket(true));
            } else {
                boolean shouldIntercept = false;

                // 1. 检查是否精确点击到任何人偶（无论敌我，客户端拦截）
                var hitResult = Minecraft.getInstance().hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                    Entity target = ((EntityHitResult) hitResult).getEntity();
                    if (target instanceof DollEntity) {
                        shouldIntercept = true;
                    }
                }

                // 2. 如果没有精确点人偶，检查是否有吸附目标（用于攻击）
                if (!shouldIntercept && DollBatonHandler.getCurrentTarget() != null) {
                    shouldIntercept = true;
                }

                if (shouldIntercept) {
                    event.setCanceled(true);
                    PacketHandler.sendToServer(new DollBatonRightClickPacket(false));
                }
                // 否则放行（如打开箱子等）
            }
        }
    }
}
