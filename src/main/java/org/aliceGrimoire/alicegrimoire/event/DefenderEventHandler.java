package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class DefenderEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenderEventHandler.class);
    private static final Map<UUID, Float> lastHealthMap = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        UUID playerId = player.getUUID();
        float currentHealth = player.getHealth();
        Float lastHealth = lastHealthMap.get(playerId);

        if (lastHealth != null && currentHealth < lastHealth) {
            LivingEntity attacker = getAttackerFromDamageSource(player);
            if (attacker != null) {
                triggerDefenderTeleport(player, attacker);
            }
        }

        lastHealthMap.put(playerId, currentHealth);
    }

    private static LivingEntity getAttackerFromDamageSource(Player player) {
        var lastDamageSource = player.getLastDamageSource();
        if (lastDamageSource == null) return null;

        LivingEntity attacker = null;

        // ===== 1. 尝试从直接实体获取 =====
        Entity directEntity = lastDamageSource.getDirectEntity();
        if (directEntity != null) {
            // 如果是投射物，获取发射者（所有者）
            if (directEntity instanceof Projectile projectile) {
                LOGGER.info("攻击物为投射物");
                if (projectile.getOwner() instanceof LivingEntity owner) {
                    attacker = owner;
                    LOGGER.info("从投射物提取发射者: {}", owner.getName().getString());
                }
            } else if (directEntity instanceof LivingEntity living) {
                // 直接实体就是攻击者（近战）
                attacker = living;
                LOGGER.info("直接攻击者: {}", living.getName().getString());
            }
        }

        // ===== 2. 如果上面没找到，尝试从真实实体获取 =====
        if (attacker == null) {
            Entity entity = lastDamageSource.getEntity();
            if (entity instanceof LivingEntity living) {
                attacker = living;
                LOGGER.info("从伤害源实体获取攻击者: {}", living.getName().getString());
            }
        }

        // ===== 3. 如果还没找到，尝试从周围最近的敌对生物获取 =====
        if (attacker == null) {
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(8.0),
                e -> e != player && e.isAlive() && player.canAttack(e)
            );
            if (!nearby.isEmpty()) {
                attacker = nearby.stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                    .orElse(null);
                if (attacker != null) {
                    LOGGER.info("从周围搜索到的攻击者: {}", attacker.getName().getString());
                }
            }
        }

        return attacker;
    }

    private static void triggerDefenderTeleport(Player player, LivingEntity attacker) {
        List<DollEntity> defenders = player.level().getEntitiesOfClass(
            DollEntity.class,
            player.getBoundingBox().inflate(16.0),
            doll -> doll.getOwner() != null &&
                    doll.getOwner().equals(player) &&
                    doll.getJobType() == DollJobType.DEFENDER &&
                    doll.isAlive() &&
                    doll.getShieldDisableTicks() <= 0
        );
        if (defenders.isEmpty()) return;

        DollEntity defender = defenders.get(0);

        // 瞬移位置
        Vec3 attackerPos = attacker.position();
        Vec3 playerPos = player.position();
        Vec3 direction = attackerPos.subtract(playerPos).normalize();
        Vec3 teleportPos = playerPos.add(direction.scale(1.0));

        defender.moveTo(teleportPos.x, player.getY() + 1.5, teleportPos.z,
                        defender.getYRot(), defender.getXRot());

        // 破盾（检测斧头延长）
        CombatParameters params = defender.getDollData().getCombatParams();
        int disableTime = params.getShieldDisableTime();
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.canDisableShield(ItemStack.EMPTY, defender, attacker)) {
            disableTime = (int)(disableTime * 1.5);
        }
        defender.setShieldDisableTicks(disableTime);
    }
}