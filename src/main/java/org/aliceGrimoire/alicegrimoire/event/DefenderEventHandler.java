package org.aliceGrimoire.alicegrimoire.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.CombatParameters;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@EventBusSubscriber(modid = Alicegrimoire.MODID)
public class DefenderEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenderEventHandler.class);

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        
        // 直接获取攻击者（比轮询准确得多）
        DamageSource source = event.getSource();
        Entity attacker = source.getDirectEntity();
        if (attacker == null) return;
        
        // 提取真正的攻击者实体
        LivingEntity realAttacker = null;
        if (attacker instanceof Projectile proj && proj.getOwner() instanceof LivingEntity owner) {
            realAttacker = owner;
        } else if (attacker instanceof LivingEntity living) {
            realAttacker = living;
        }
        if (realAttacker == null) return;
        
        // 触发守御瞬移（复用现有 triggerDefenderTeleport 逻辑）
        triggerDefenderTeleport(player, realAttacker);
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
        Vec3 direction = attackerPos.subtract(playerPos);
        if (direction.lengthSqr() < 0.0001) {
            // 若重合，使用玩家视线方向作为默认方向
            direction = player.getLookAngle();
        }
        Vec3 teleportPos = playerPos.add(direction.normalize().scale(1.0));

        BlockPos targetPos = BlockPos.containing(teleportPos);
        if (!player.level().getBlockState(targetPos).isAir()) {
            teleportPos = teleportPos.add(0, 1.0, 0);
        }
        
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