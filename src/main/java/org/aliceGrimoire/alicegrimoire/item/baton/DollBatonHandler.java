package org.aliceGrimoire.alicegrimoire.item.baton;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 指挥棒预选目标处理
 * 手持指挥棒时持续高亮准星指向的目标
 * 优先：精确射线命中 → 备选：视线中心最近（16格内，严格偏差）
 */
@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class DollBatonHandler {

    private static final double REACH = 16.0;
    private static final double FOV_THRESHOLD = 0.99;

    // 使用 WeakHashMap，玩家下线后自动释放引用
    // 值使用 WeakReference，目标实体死亡后自动释放
    private static final Map<Player, WeakReference<LivingEntity>> currentTargets = new WeakHashMap<>();
    private static final Map<Player, WeakReference<LivingEntity>> lastTargets = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // 每2tick检测一次
        if (player.level().getGameTime() % 2 != 0) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean hasBaton = mainHand.getItem() instanceof DollBatonItem ||
                offHand.getItem() instanceof DollBatonItem;

        if (!hasBaton) {
            clearTargetGlow(player);
            return;
        }

        LivingEntity target = raycastTarget(player);
        LivingEntity lastTarget = getLastTarget(player);

        if (target != lastTarget) {
            clearTargetGlow(player);
            if (target != null) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                currentTargets.put(player, new WeakReference<>(target));
            }
            lastTargets.put(player, new WeakReference<>(target));
        }

        LivingEntity current = getCurrentTarget(player);
        if (current != null && !current.isAlive()) {
            clearTargetGlow(player);
        }
    }

    public static LivingEntity getCurrentTarget(Player player) {
        WeakReference<LivingEntity> ref = currentTargets.get(player);
        return ref != null ? ref.get() : null;
    }

    private static LivingEntity getLastTarget(Player player) {
        WeakReference<LivingEntity> ref = lastTargets.get(player);
        return ref != null ? ref.get() : null;
    }

    public static void clearTargetGlow(Player player) {
        LivingEntity current = getCurrentTarget(player);
        if (current != null) {
            current.removeEffect(MobEffects.GLOWING);
        }
        currentTargets.remove(player);
        lastTargets.remove(player);
    }

    // 玩家登出时清理（由 ModEvents 调用）
    public static void onPlayerLoggedOut(Player player) {
        clearTargetGlow(player);
    }

    private static LivingEntity raycastTarget(Player player) {
        Level level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.x * REACH, look.y * REACH, look.z * REACH);

        List<Entity> entities = level.getEntities(player,
                player.getBoundingBox().inflate(REACH),
                entity -> entity != player && entity.isAlive() && entity instanceof LivingEntity);

        double maxReachSq = REACH * REACH;
        entities = entities.stream()
            .filter(e -> e.distanceToSqr(player) <= maxReachSq)
            .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
            .collect(Collectors.toList());
        
        LivingEntity bestTarget = null;
        double bestDot = -1.0;

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) continue;

            // 跳过己方人偶，敌方人偶允许被选中
            if (entity instanceof DollEntity doll) {
                LivingEntity owner = doll.getOwner();
                if (owner != null && owner.equals(player)) {
                    continue; // 自己的不能攻击
                }
                // 敌方人偶继续执行
            }

            AABB aabb = entity.getBoundingBox();
            Optional<Vec3> hitPos = aabb.clip(start, end);
            if (hitPos.isPresent()) {
                return living;
            }

            if (player.canAttack(living)) {
                Vec3 toEntity = living.position().subtract(player.getEyePosition()).normalize();
                double dot = look.dot(toEntity);

                if (dot > FOV_THRESHOLD && dot > bestDot) {
                    bestDot = dot;
                    bestTarget = living;
                }
            }
        }

        return bestTarget;
    }
}