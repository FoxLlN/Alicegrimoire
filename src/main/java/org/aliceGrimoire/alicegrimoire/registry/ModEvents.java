package org.aliceGrimoire.alicegrimoire.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.event.PlayerMoveDetector;

import java.util.HashSet;

@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 清除标记目标
        event.getEntity().setData(ModAttachments.MARKED_TARGETS, new HashSet<>());
        // 清除移动检测缓存
        PlayerMoveDetector.onPlayerLoggedOut(event.getEntity().getUUID());
    }
}
