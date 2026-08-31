package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

import java.util.Comparator;
import java.util.List;

public class DollWhistleItem extends Item {

    // 3秒不主动移动的 tick 数
    private static final int NO_MOVEMENT_TICKS = 60;

    public DollWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 获取所有拴住的人偶
            List<DollEntity> dolls = level.getEntitiesOfClass(
                DollEntity.class,
                player.getBoundingBox().inflate(64.0),
                doll -> player.getUUID().equals(doll.getOwnerUUID()) &&
                        doll.isTethered() &&
                        doll.isAlive()
            );

            if (dolls.isEmpty()) {
                player.displayClientMessage(
                    Component.translatable("message.alicegrimoire.doll_no_available"),
                    true
                );
                return InteractionResultHolder.pass(stack);
            }

            boolean shiftDown = player.isShiftKeyDown();

            if (shiftDown) {
                // ===== Shift + 右键：所有拴住的人偶 =====
                whistleAllDolls(player, dolls);
            } else {
                // ===== 普通右键：距离最远的人偶 =====
                whistleFarthestDoll(player, dolls);
            }

            // 播放哨声音效
            level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS,
                1.0F,
                1.2F + (level.random.nextFloat() - 0.5F) * 0.2F
            );

            // 物品冷却（防止连按）
            player.getCooldowns().addCooldown(this, 20);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 普通右键：对距离最远的人偶吹哨
     */
    private void whistleFarthestDoll(Player player, List<DollEntity> dolls) {
        // 找出距离最远的人偶
        DollEntity farthest = dolls.stream()
            .max(Comparator.comparingDouble(doll -> doll.distanceToSqr(player)))
            .orElse(null);

        if (farthest == null) {
            player.displayClientMessage(
                Component.translatable("message.alicegrimoire.doll_no_available"),
                true
            );
            return;
        }

        // 执行传送
        teleportDollToPlayer(player, farthest);

        player.displayClientMessage(
            Component.translatable(
                "message.alicegrimoire.doll_whistle_farthest",
                farthest.getDisplayName()
            ),
            true
        );
    }

    /**
     * Shift + 右键：对所有拴住的人偶吹哨
     */
    private void whistleAllDolls(Player player, List<DollEntity> dolls) {
        int count = 0;
        for (DollEntity doll : dolls) {
            // 传送每一个人偶
            teleportDollToPlayer(player, doll);
            count++;
        }

        player.displayClientMessage(
            Component.translatable(
                "message.alicegrimoire.doll_whistle_all",
                count
            ),
            true
        );
    }

    /**
     * 传送单个人偶到玩家面前，并设置3秒不主动移动
     */
    private void teleportDollToPlayer(Player player, DollEntity doll) {
        // ===== 1. 强制解除激怒状态 =====
        if (doll.isEnraged()) {
            doll.setEnraged(false);
        }

        // ===== 2. 计算传送位置：玩家面前 1.5 格 =====
        Vec3 lookVec = player.getLookAngle();
        Vec3 teleportPos = player.position()
            .add(0, 0.5, 0)  // 抬高一点
            .add(lookVec.x * 1.5, 0, lookVec.z * 1.5);

        // 检查传送位置是否在方块内部，如果是则略微抬高
        if (player.level().getBlockState(BlockPos.containing(teleportPos)).isSolid()) {
            teleportPos = teleportPos.add(0, 1.0, 0);
        }

        // ===== 3. 执行传送 =====
        doll.moveTo(teleportPos.x, teleportPos.y, teleportPos.z, player.getYRot(), player.getXRot());
        doll.setDeltaMovement(Vec3.ZERO);

        // ===== 4. 设置3秒不主动移动 =====
        doll.setNoMovementTicks(NO_MOVEMENT_TICKS);

        // ===== 5. 设置返回模式（策划案：依旧可以被拴住的最大移动范围所拖动） =====
        doll.setReturning(true);

        // 生成传送粒子效果
        player.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.POOF,
            doll.getX(), doll.getY() + 0.5, doll.getZ(),
            0.1, 0.1, 0.1
        );
        player.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.PORTAL,
            doll.getX(), doll.getY() + 0.5, doll.getZ(),
            0.2, 0.1, 0.2
        );
    }
}