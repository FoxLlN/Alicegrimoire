package org.aliceGrimoire.alicegrimoire.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;

import java.util.HashSet;

@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 登出时清除标记目标
        event.getEntity().setData(ModAttachments.MARKED_TARGETS, new HashSet<>());
    }
}
