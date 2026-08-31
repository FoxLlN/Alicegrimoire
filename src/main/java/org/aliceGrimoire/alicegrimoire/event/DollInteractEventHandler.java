package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 人偶交互事件处理器
 * 当玩家空手右键人偶时，在聊天框打印人偶背包内容
 * 未来可替换为打开 GUI
 */
@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class DollInteractEventHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // 获取交互目标
        Entity target = event.getTarget();
        if (!(target instanceof DollEntity doll)) {
            return; // 不是人偶，不处理
        }

        Player player = event.getEntity();
        // 仅当玩家主手为空时才触发（空手右键）
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            return;
        }

        // 仅在服务端执行（客户端不处理逻辑）
        if (player.level().isClientSide) {
            return;
        }

        // 限制只有人偶主人才能查看
        if (!player.getUUID().equals(doll.getOwnerUUID())) {
             return;
        }

        // 打印背包内容（需要在 DollEntity 中实现 printInventory 方法）
        doll.printInventory(player);

        // 取消事件，阻止其他可能的交互（如喂食、拴绳等）
        event.setCanceled(true);
    }
}