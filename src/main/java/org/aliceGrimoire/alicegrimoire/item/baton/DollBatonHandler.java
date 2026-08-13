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

/**
 * 指挥棒预选目标处理
 * 手持指挥棒时持续高亮准星指向的目标
 * 优先：精确射线命中 → 备选：视线中心最近（16格内，严格偏差）
 */
@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class DollBatonHandler {

    private static final double REACH = 16.0;
    private static final double FOV_THRESHOLD = 0.99;

    private static LivingEntity currentTarget = null;
    private static LivingEntity lastTarget = null;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean hasBaton = mainHand.getItem() instanceof DollBatonItem ||
                offHand.getItem() instanceof DollBatonItem;

        if (!hasBaton) {
            clearTargetGlow();
            // Todo 动画
            return;
        }

        // 查找目标
        LivingEntity target = raycastTarget(player);

        if (target != lastTarget) {
            clearTargetGlow();
            if (target != null) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                currentTarget = target;
                // Todo 动画
            } else {
                // Todo 动画
            }
            lastTarget = target;
        }

        if (currentTarget != null && !currentTarget.isAlive()) {
            clearTargetGlow();
            // Todo 动画
        }
    }

    public static LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public static void clearTargetGlow() {
        if (currentTarget != null) {
            currentTarget.removeEffect(MobEffects.GLOWING);
            currentTarget = null;
        }
        lastTarget = null;
    }

    private static LivingEntity raycastTarget(Player player) {
        Level level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.x * REACH, look.y * REACH, look.z * REACH);

        List<Entity> entities = level.getEntities(player,
                player.getBoundingBox().inflate(REACH),
                entity -> entity != player && entity.isAlive() && entity instanceof LivingEntity);

        entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

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