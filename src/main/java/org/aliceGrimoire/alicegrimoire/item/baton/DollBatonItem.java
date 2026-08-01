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
    // 业务逻辑方法（保持原样，只是调用管理类）
    // ============================================================

    /**
     * 左键点击处理
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

        // 检查目标是否有效（不能是其他人的人偶）
        if (target instanceof DollEntity doll) {
            LivingEntity owner = doll.getOwner();
            if (owner != null && owner.equals(player)) {
                player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_cannot_target_ally"), true);
                return;
            }
            player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_cannot_target_ally"), true);
            return;
        }

        List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class,
                player.getBoundingBox().inflate(64.0D),
                doll -> player.getUUID().equals(doll.getOwnerUUID()));

        if (dolls.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.alicegrimoire.doll_no_available"), true);
            return;
        }

        if (shiftDown) {
            // Shift + 左键：所有人偶攻击
            int count = 0;
            for (DollEntity doll : dolls) {
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
            // 普通左键：单个人偶攻击
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
     * 右键点击处理
     */
    public static void handleRightClick(Player player, boolean shiftDown) {
        if (player.level().isClientSide()) return;

        Level level = player.level();

        if (shiftDown) {
            // Shift + 右键：解除所有人偶激怒
            List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class,
                    player.getBoundingBox().inflate(64.0),
                    doll -> player.getUUID().equals(doll.getOwnerUUID()));
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
                if (player.getUUID().equals(doll.getOwnerUUID())) {
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
                }
            } else {
                // 指向生物：解除所有攻击该生物的人偶
                List<DollEntity> dolls = level.getEntitiesOfClass(DollEntity.class,
                        player.getBoundingBox().inflate(64.0),
                        doll -> player.getUUID().equals(doll.getOwnerUUID()) &&
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

    private static DollEntity selectDoll(List<DollEntity> dolls, LivingEntity target, boolean allowStuck) {
        dolls.sort(Comparator.comparingLong(DollEntity::getEvokeTime));
        for (DollEntity doll : dolls) {
            if (!doll.isEnraged() && doll.isAlive()) {
                if (!allowStuck && doll.isInsideBlock()) {
                    continue;
                }
                if (doll.canBeEnraged()) {
                    return doll;
                }
            }
        }
        return null;
    }
}