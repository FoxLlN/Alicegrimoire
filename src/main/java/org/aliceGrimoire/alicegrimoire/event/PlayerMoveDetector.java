package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家移动检测器
 * 监听 PlayerTickEvent，检测玩家位置变化，更新所有人偶的移动状态
 */
@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class PlayerMoveDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerMoveDetector.class);
    
    // 缓存每个玩家的上一次位置
    private static final Map<UUID, Vec3> lastPositions = new HashMap<>();
    
    // 缓存每个玩家的移动状态（避免频繁查询人偶）
    private static final Map<UUID, Boolean> playerMovingCache = new HashMap<>();

    // 缓存每个玩家的实际速度（格/tick）
    private static final Map<UUID, Double> playerSpeedCache = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        Vec3 currentPos = player.position();
        
        // 获取上一次位置
        Vec3 lastPos = lastPositions.get(playerId);
        
        boolean isMoving = false;
        double speed = 0.0;
        if (lastPos != null) {
            // 计算移动距离
            double dx = currentPos.x - lastPos.x;
            double dy = currentPos.y - lastPos.y;
            double dz = currentPos.z - lastPos.z;
            double distSq = dx*dx + dy*dy + dz*dz;
            
            // 阈值 0.0001（约 0.01 格），避免微小晃动触发
            isMoving = distSq > 0.0001;

            // 速度 = 距离 / 1 tick（因为每 tick 执行一次）
            speed = Math.sqrt(dx*dx + dy*dy + dz*dz);
        }
        
        // 更新缓存
        lastPositions.put(playerId, currentPos);
        playerSpeedCache.put(playerId, speed);
        
        // 如果移动状态发生变化，通知所有人偶
        Boolean cachedMoving = playerMovingCache.get(playerId);
        if (cachedMoving == null || cachedMoving != isMoving) {
            playerMovingCache.put(playerId, isMoving);
            
            // 更新该玩家拥有的所有人偶的移动状态
            updateDollsMovingState(player, isMoving);
            
            // 调试日志（可选，正常使用时可注释掉）
            if (isMoving) {
                LOGGER.debug("[PlayerMove] Player {} is moving", player.getName().getString());
            }
        }
    }

    /**
     * 更新玩家拥有的所有人偶的移动状态
     */
    private static void updateDollsMovingState(Player player, boolean isMoving) {
        // 获取玩家周围 64 格内的所有人偶
        List<DollEntity> dolls = player.level().getEntitiesOfClass(
            DollEntity.class,
            player.getBoundingBox().inflate(64.0D),
            doll -> player.getUUID().equals(doll.getOwnerUUID())
        );
        
        for (DollEntity doll : dolls) {
            doll.setPlayerMoving(isMoving);
        }
    }

    /**
     * 获取玩家的移动状态
     */
    public static boolean getPlayerMoving(UUID playerId) {
        return playerMovingCache.getOrDefault(playerId, false);
    }

    /**
     * 获取玩家最近的实际移动速度（格/tick）
     * 供 DollMovementHandler 调用
     */
    public static double getPlayerSpeed(UUID playerId) {
        return playerSpeedCache.getOrDefault(playerId, 0.0);
    }

    /**
     * 玩家登出时清理缓存
     */
    public static void onPlayerLoggedOut(UUID playerId) {
        lastPositions.remove(playerId);
        playerMovingCache.remove(playerId);
        playerSpeedCache.remove(playerId);
    }
}