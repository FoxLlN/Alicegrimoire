package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.item.DollWandItem;
import org.aliceGrimoire.alicegrimoire.network.DollWandLeftClickPacket;
import org.aliceGrimoire.alicegrimoire.network.DollWandRightClickPacket;
import org.aliceGrimoire.alicegrimoire.network.PacketHandler;

@EventBusSubscriber(modid = Alicegrimoire.MODID, value = Dist.CLIENT)
public class DollWandInputHandler {

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean isWand = mainHand.getItem() instanceof DollWandItem || offHand.getItem() instanceof DollWandItem;
        if (!isWand) return;

        KeyMapping key = event.getKeyMapping();
        if (key == Minecraft.getInstance().options.keyAttack) {
            event.setCanceled(true);
            // 发送左键网络包到服务端
            PacketHandler.sendToServer(new DollWandLeftClickPacket());
        } else if (key == Minecraft.getInstance().options.keyUse) {
            event.setCanceled(true);
            // 发送右键网络包到服务端
            PacketHandler.sendToServer(new DollWandRightClickPacket());
        }
    }
}