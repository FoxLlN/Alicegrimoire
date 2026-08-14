package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.event.PlayerMoveDetector;
import org.aliceGrimoire.alicegrimoire.item.string.StringHelper;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModEntities;

import java.util.ArrayList;
import java.util.List;

public class DollBasketItem extends Item {
    public DollBasketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack basket = player.getItemInHand(hand);
        List<ItemStack> dolls = basket.get(ModDataComponents.DOLLS.get());

        if (dolls == null || dolls.isEmpty()) {
            return InteractionResultHolder.pass(basket);
        }

        if (!level.isClientSide) {
            boolean hasString = StringHelper.hasStringEquipped(player);

            // 遍历每个人偶，逐个生成
            for (ItemStack dollStack : dolls) {
                DollEntity doll = ModEntities.DOLL.get().create(level);
                if (doll == null) continue;

                // 加载NBT数据
                CustomData entityData = dollStack.get(DataComponents.ENTITY_DATA);
                if (entityData != null) {
                    doll.load(entityData.copyTag());
                }

                // 设置所有者
                doll.setOwnerUUID(player.getUUID());
                doll.setPlayerMoving(PlayerMoveDetector.getPlayerMoving(player.getUUID()));
                doll.moveTo(player.getX(), player.getEyeY(), player.getZ(), player.getYRot(), player.getXRot());

                // 判断是否拴住（仅在穿丝线且当前仍有空余名额时）
                boolean tethered = false;
                if (hasString) {
                    tethered = StringHelper.canTetherMore(player);  // 实时检查剩余名额
                }
                doll.setTethered(tethered);

                // 随机偏移 + 唤起动画
                Vec3 look = player.getLookAngle();
                double rx = (level.random.nextDouble() - 0.5) * 0.5;
                double ry = (level.random.nextDouble() - 0.5) * 0.5;
                double rz = (level.random.nextDouble() - 0.5) * 0.5;
                doll.setDeltaMovement(look.scale(1.5D).add(rx, ry, rz));
                doll.setEvokeTime(level.getGameTime());

                level.addFreshEntity(doll);
            }

            // 清空篮子
            basket.set(ModDataComponents.DOLLS.get(), new ArrayList<>());
        }

        return InteractionResultHolder.sidedSuccess(basket, level.isClientSide());
    }

}
