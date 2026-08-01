package org.aliceGrimoire.alicegrimoire.item.baton;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
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
            event.setCanceled(true);
            PacketHandler.sendToServer(new DollBatonLeftClickPacket(shiftDown));
        } else if (key == Minecraft.getInstance().options.keyUse) {
            event.setCanceled(true);
            PacketHandler.sendToServer(new DollBatonRightClickPacket(shiftDown));
        }
    }
}