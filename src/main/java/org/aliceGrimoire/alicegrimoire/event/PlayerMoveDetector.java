package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

/**
 * 玩家移动检测器
 * 监听 PlayerTickEvent，检测玩家位置变化，更新所有人偶的移动状态
 */
@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class PlayerMoveDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerMoveDetector.class);
    
    // 缓存每个玩家的上一次位置
     private static final Cache<UUID, Vec3> lastPositions = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();
    
    // 缓存每个玩家的移动状态（避免频繁查询人偶）
    private static final Cache<UUID, Boolean> playerMovingCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    // 缓存每个玩家的实际速度（格/tick）
    private static final Cache<UUID, Double> playerSpeedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

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
        Vec3 lastPos = lastPositions.getIfPresent(playerId);
        
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
        Boolean cachedMoving = playerMovingCache.getIfPresent(playerId);
        if (cachedMoving == null || cachedMoving != isMoving) {
            playerMovingCache.put(playerId, isMoving);
                        
            // 调试日志（可选，正常使用时可注释掉）
            if (isMoving) {
                LOGGER.debug("[PlayerMove] Player {} is moving", player.getName().getString());
            }
        }
    }

    /**
     * 获取玩家的移动状态
     */
    public static boolean getPlayerMoving(UUID playerId) {
        Boolean val = playerMovingCache.getIfPresent(playerId);
        return val != null && val;
    }

    /**
     * 获取玩家最近的实际移动速度（格/tick）
     * 供 DollMovementHandler 调用
     */
    public static double getPlayerSpeed(UUID playerId) {
        Double val = playerSpeedCache.getIfPresent(playerId);
        return val == null ? 0.0 : val;
    }

    /**
     * 玩家登出时清理缓存
     */
    public static void onPlayerLoggedOut(UUID playerId) {
        lastPositions.invalidate(playerId);
        playerMovingCache.invalidate(playerId);
        playerSpeedCache.invalidate(playerId);
    }
}