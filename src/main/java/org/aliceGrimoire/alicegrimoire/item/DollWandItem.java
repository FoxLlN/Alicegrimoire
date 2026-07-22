package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.registry.ModAttachments;

import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DollWandItem extends SwordItem {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    public DollWandItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            // 无论什么情况下右键都解除激怒并回到游荡状态
            List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class, player.getBoundingBox().inflate(64.0D), 
                doll -> player.getUUID().equals(doll.getOwnerUUID()));
            for (DollEntity doll : dolls) {
                doll.setEnraged(false);
                doll.setTarget(null);
            }
            
            // 清除所有标记的目标
            player.setData(ModAttachments.MARKED_TARGETS, new HashSet<>());
            
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.alicegrimoire.doll_stop_attack"), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Player player && !player.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            // 进行射线检测
            double reach = 16.0; 
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            Vec3 end = start.add(look.x * reach, look.y * reach, look.z * reach);
            
            EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player.level(), player, start, end, 
                player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D), 
                target -> target instanceof LivingEntity && target.isAlive() && target != player);

            if (hitResult != null && hitResult.getEntity() instanceof LivingEntity target) {
                Level level = player.level();
                
                // 标记为敌人
                Set<Integer> marked = new HashSet<>(player.getData(ModAttachments.MARKED_TARGETS));
                marked.add(target.getId());
                player.setData(ModAttachments.MARKED_TARGETS, marked);
                
                List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class, 
                    player.getBoundingBox().inflate(64.0D), 
                    doll -> player.getUUID().equals(doll.getOwnerUUID()));
                
                // 激怒人偶的顺序为唤起人偶的顺序
                DollEntity selectedDoll = selectDoll(dolls, target);
                
                if (selectedDoll != null) {
                    // LOGGER.info("[Wand] Selected doll: " + selectedDoll);
                    // 检查是否卡墙（优先选择没卡墙的）
                    if (selectedDoll.isInsideBlock()) {
                        // 如果选中的卡墙了，尝试找下一个
                        DollEntity fallback = selectDoll(dolls, target, true);
                        if (fallback != null) {
                            selectedDoll = fallback;
                        } else {
                            // 全都卡墙了，提示玩家
                            player.displayClientMessage(
                                Component.translatable("message.alicegrimoire.doll_all_stuck"), true
                            );
                            return super.onEntitySwing(stack, entity, hand);
                        }
                    }
                    
                    // 激怒这个人偶
                    selectedDoll.setEnraged(true);
                    selectedDoll.setTarget(target);
                    
                    // 记录激怒时间（用于发光计时）
                    selectedDoll.setEnrageTime(level.getGameTime());
                    
                    player.displayClientMessage(
                        Component.translatable("message.alicegrimoire.doll_locked_target"), true
                    );
                } else {
                    // 没有可激怒的人偶
                    player.displayClientMessage(
                        Component.translatable("message.alicegrimoire.doll_no_available"), true
                    );
                }
            }
        }
        return super.onEntitySwing(stack, entity, hand);
    }

    /**
     * 选择一个人偶（核心选择方法）
     * @param dolls 所有人偶列表
     * @param target 攻击目标
     * @return 选中的最佳人偶
     */
    private DollEntity selectDoll(List<DollEntity> dolls, LivingEntity target) {
        return selectDoll(dolls, target, false);
    }

    /**
     * 选择一个人偶，可指定是否接受卡墙的
     * @param dolls 所有人偶列表
     * @param target 攻击目标
     * @param allowStuck 是否允许选择卡墙的人偶
     * @return 选中的最佳人偶
     */
    private DollEntity selectDoll(List<DollEntity> dolls, LivingEntity target, boolean allowStuck) {
        // 1. 按唤起时间排序（唤起顺序）
        dolls.sort(Comparator.comparingLong(DollEntity::getEvokeTime));
        
        // 2. 过滤条件：未激怒 + 未卡墙（除非允许）+ 未被标记为"无法使用"
        for (DollEntity doll : dolls) {
            // LOGGER.info("[Wand] Checking doll: " + doll + ", enraged: " + doll.isEnraged() + 
            //                ", insideBlock: " + doll.isInsideBlock() + ", canBeEnraged: " + doll.canBeEnraged());
            if (!doll.isEnraged() && doll.isAlive()) {
                if (!allowStuck && doll.isInsideBlock()) {
                    continue; // 跳过卡墙的
                }
                // 检查这个玩偶是否还能战斗（未被禁用等）
                if (doll.canBeEnraged()) {
                    return doll;
                }
            }
        }
        return null;
    }

    /**
     * 人偶是否可以被激怒
     */
    public boolean canBeEnraged() {
        // 未来可以扩展：检查是否破损、是否被禁用等
        return true;
    }
}
