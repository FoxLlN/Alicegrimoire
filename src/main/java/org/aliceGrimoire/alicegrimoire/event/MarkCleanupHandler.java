package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.registry.ModAttachments;

import java.util.HashSet;
import java.util.Set;

/**
 * 标记清理处理器类
 * 用于处理游戏中标记目标的清理工作，包括实体离开世界、玩家下线以及定期清理无效标记
 */
@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class MarkCleanupHandler {

    // 清理计数器，用于定期清理无效标记
    private static int cleanupTickCounter = 0;

    /**
     * 处理实体离开世界的事件
     * @param event 实体离开世界事件
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        // 如果是客户端 side 则直接返回，不处理
        if (event.getLevel().isClientSide()) return;

        Entity leavingEntity = event.getEntity();
        int leavingEntityId = leavingEntity.getId();

        // 如果离开的实体是玩家，则不处理
        if (leavingEntity instanceof Player) return;

        var server = leavingEntity.getServer();
        if (server == null) return;

        // 遍历所有在线玩家，检查并移除对离开实体的标记
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Set<Integer> marks = player.getData(ModAttachments.MARKED_TARGETS);
            if (marks.remove(leavingEntityId)) {
                player.setData(ModAttachments.MARKED_TARGETS, marks);
            }
        }
    }

    /**
     * 处理玩家下线事件
     * @param event 玩家下线事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        // 玩家下线时清空其所有标记
        player.setData(ModAttachments.MARKED_TARGETS, new HashSet<>());
    }

    /**
     * 处理服务器 tick 事件
     * 用于定期清理无效的实体标记
     * @param event 服务器 tick 事件
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server == null) return;

        // 更新清理计数器
        cleanupTickCounter++;
        // 每 6000 tick (约5分钟，假设20 tick/秒) 执行一次清理
        if (cleanupTickCounter % 6000 != 0) return;

        // 防止计数器溢出
        if (cleanupTickCounter >= Integer.MAX_VALUE) {
            cleanupTickCounter = 0;
        }

        // 遍历所有在线玩家，清理无效的实体标记
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Set<Integer> marks = player.getData(ModAttachments.MARKED_TARGETS);
            if (marks.isEmpty()) continue;

            // 收集需要移除的无效标记
            Set<Integer> toRemove = new HashSet<>();
            for (int id : marks) {
                Entity e = player.level().getEntity(id);
                if (e == null || !e.isAlive()) {
                    toRemove.add(id);
                }
            }
            if (!toRemove.isEmpty()) {
                marks.removeAll(toRemove);
                player.setData(ModAttachments.MARKED_TARGETS, marks);
            }
        }
    }
}