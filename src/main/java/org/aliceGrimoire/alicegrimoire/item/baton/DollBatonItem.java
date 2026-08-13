package org.aliceGrimoire.alicegrimoire.item.baton;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.aliceGrimoire.alicegrimoire.client.AliceGeoModel;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.registry.ModAttachments;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Consumer;

public class DollBatonItem extends SwordItem implements GeoItem {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DollBatonItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    // ========== GeoItem 接口实现 ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 使用独立管理类来处理动画
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> DollBatonAnimationManager.handleAnimation(state)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<DollBatonItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new AliceGeoModel<>(
                            "geo/doll_baton.geo.json",
                            "textures/item/doll_baton.png",
                            "animations/doll_baton.animation.json"
                    ));
                }
                return renderer;
            }
        });
    }

    // ============================================================
    // 业务逻辑方法
    // ============================================================

    /**
     * 左键点击处理（攻击指令）
     * 修改点：
     * 1. 允许攻击敌方人偶（仅阻止己方人偶）
     * 2. 所有指令仅对拴住的人偶生效
     */
    public static void handleLeftClick(Player player, boolean shiftDown) {
        if (player.level().isClientSide()) return;

        Level level = player.level();

        // 直接使用预选目标
        LivingEntity target = DollBatonHandler.getCurrentTarget();

        if (target == null) {
            player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_target"), true);
            return;
        }

        // 阻止攻击己方人偶，允许攻击敌方人偶
        if (target instanceof DollEntity doll) {
            LivingEntity owner = doll.getOwner();
            if (owner != null && owner.equals(player)) {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_cannot_target_ally"), true);
                return;
            }
            // 如果是敌方人偶，继续执行（不拦截）
        }

        // 只获取拴住的人偶
        List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class,
                player.getBoundingBox().inflate(64.0D),
                doll -> player.getUUID().equals(doll.getOwnerUUID()) && doll.isTethered());

        if (dolls.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_available"), true);
            return;
        }

        if (shiftDown) {
            // Shift + 左键：所有拴住的人偶攻击目标
            int count = 0;
            for (DollEntity doll : dolls) {
                // 防御性检查：确保拴住
                if (doll.isTethered() && doll.canBeEnraged() && !doll.isInsideBlock()) {
                    doll.setEnraged(true);
                    doll.setTarget(target);
                    doll.setAssignedTargetId(target.getId());
                    doll.setEnrageTime(level.getGameTime());
                    count++;
                }
            }
            if (count > 0) {
                Set<Integer> marked = new HashSet<>(player.getData(ModAttachments.MARKED_TARGETS));
                marked.add(target.getId());
                player.setData(ModAttachments.MARKED_TARGETS, marked);
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_all_attack", count), true);
            } else {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_available"), true);
            }
        } else {
            // 普通左键：选择一个人偶攻击
            DollEntity selectedDoll = selectDoll(dolls, target);
            if (selectedDoll != null) {
                if (target == selectedDoll) {
                    player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_cannot_target_self"), true);
                    return;
                }

                if (selectedDoll.isInsideBlock()) {
                    DollEntity fallback = selectDoll(dolls, target, true);
                    if (fallback != null) {
                        selectedDoll = fallback;
                    } else {
                        player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_all_stuck"), true);
                        return;
                    }
                }

                selectedDoll.setEnraged(true);
                selectedDoll.setTarget(target);
                selectedDoll.setAssignedTargetId(target.getId());
                selectedDoll.setEnrageTime(level.getGameTime());

                Set<Integer> marked = new HashSet<>(player.getData(ModAttachments.MARKED_TARGETS));
                marked.add(target.getId());
                player.setData(ModAttachments.MARKED_TARGETS, marked);

                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_locked_target"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_available"), true);
            }
        }
    }

    /**
     * 右键点击处理（解除指令）
     * 所有操作仅对拴住的人偶生效
     */
    public static void handleRightClick(Player player, boolean shiftDown) {
        if (player.level().isClientSide()) return;

        Level level = player.level();

        if (shiftDown) {
            // Shift + 右键：解除所有拴住人偶的激怒
            List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class,
                    player.getBoundingBox().inflate(64.0),
                    doll -> player.getUUID().equals(doll.getOwnerUUID()) && doll.isTethered());
            int count = 0;
            for (DollEntity doll : dolls) {
                if (doll.isEnraged()) {
                    doll.setEnraged(false);
                    doll.setTarget(null);
                    count++;
                }
            }
            player.setData(ModAttachments.MARKED_TARGETS, new HashSet<>());
            DollBatonHandler.clearTargetGlow();
            if (count > 0) {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_stop_attack_all", count), true);
            } else {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_attack"), true);
            }
        } else {
            // 普通右键：精准解除
            LivingEntity target = DollBatonHandler.getCurrentTarget();
            if (target == null) {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_target"), true);
                return;
            }

            if (target instanceof DollEntity doll) {
                // 目标是己方人偶，且需拴住
                if (player.getUUID().equals(doll.getOwnerUUID()) && doll.isTethered()) {
                    if (doll.isEnraged()) {
                        doll.setEnraged(false);
                        doll.setTarget(null);
                        Set<Integer> marked = new HashSet<>(player.getData(ModAttachments.MARKED_TARGETS));
                        marked.remove(doll.getId());
                        player.setData(ModAttachments.MARKED_TARGETS, marked);
                        player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_stop_single"), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_not_angry"), true);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_cannot_target_ally"), true);
                }
            } else {
                // 指向非人偶生物：解除所有拴住人偶对该目标的攻击
                List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class,
                        player.getBoundingBox().inflate(64.0),
                        doll -> player.getUUID().equals(doll.getOwnerUUID()) &&
                                doll.isTethered() &&
                                doll.getTarget() != null &&
                                doll.getTarget().equals(target));
                int count = 0;
                for (DollEntity doll : dolls) {
                    doll.setEnraged(false);
                    doll.setTarget(null);
                    count++;
                }
                Set<Integer> marked = new HashSet<>(player.getData(ModAttachments.MARKED_TARGETS));
                marked.remove(target.getId());
                player.setData(ModAttachments.MARKED_TARGETS, marked);
                if (count > 0) {
                    player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_stop_target", count), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_attack"), true);
                }
            }
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private static DollEntity selectDoll(List<DollEntity> dolls, LivingEntity target) {
        return selectDoll(dolls, target, false);
    }

    /**
     * 选择一个人偶进行攻击
     * 修改点：增加拴住检查（防御性）
     */
    private static DollEntity selectDoll(List<DollEntity> dolls, LivingEntity target, boolean allowStuck) {
        dolls.sort(Comparator.comparingLong(DollEntity::getEvokeTime));
        for (DollEntity doll : dolls) {
            // 确保拴住（传入列表已过滤，但防御）
            if (!doll.isTethered()) continue;
            if (!doll.isEnraged() && doll.isAlive()) {
                if (!allowStuck && doll.isInsideBlock()) continue;
                if (doll.canBeEnraged()) {
                    return doll;
                }
            }
        }
        return null;
    }
}