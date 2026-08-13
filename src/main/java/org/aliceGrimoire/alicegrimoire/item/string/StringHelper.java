package org.aliceGrimoire.alicegrimoire.item.string;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

import java.util.List;

public class StringHelper {
    
    /**
     * 获取玩家当前装备的丝线属性
     */
    public static IStringProperties getStringProperties(LivingEntity entity) {
        if (entity == null) return null;
        ItemStack leggings = entity.getItemBySlot(EquipmentSlot.LEGS);
        if (leggings.isEmpty()) return null;
        if (leggings.getItem() instanceof IStringProperties props) {
            return props;
        }
        return null;
    }
    
    /**
     * 获取玩家当前丝线的最大拴住数量
     */
    public static int getMaxTethered(LivingEntity entity) {
        IStringProperties props = getStringProperties(entity);
        if (props == null) return 0;
        return props.getMaxTethered();
    }
    
    /**
     * 检测玩家是否装备了丝线
     */
    public static boolean hasStringEquipped(LivingEntity entity) {
        return getStringProperties(entity) != null;
    }
    
    /**
     * 统计玩家当前拴住的人偶数量（仅计算占用名额的）
     */
    public static int countOccupiedSlots(Player player) {
        List<DollEntity> dolls = player.level().getEntitiesOfClass(DollEntity.class,
            player.getBoundingBox().inflate(64.0),
            doll -> player.getUUID().equals(doll.getOwnerUUID()) && 
                    doll.isTethered() && 
                    doll.getDollData().getOccupiesSlot()
        );
        return dolls.size();
    }
    
    /**
     * 统计玩家当前拴住的人偶总数
     */
    public static int countTetheredDolls(Player player) {
        List<DollEntity> dolls = player.level().getEntitiesOfClass(DollEntity.class,
            player.getBoundingBox().inflate(64.0),
            doll -> player.getUUID().equals(doll.getOwnerUUID()) && doll.isTethered()
        );
        return dolls.size();
    }
    
    /**
     * 检查玩家是否还能再拴住一个人偶（考虑占用名额）
     */
    public static boolean canTetherMore(Player player) {
        int max = getMaxTethered(player);
        int occupied = countOccupiedSlots(player);
        return occupied < max;
    }
    
    /**
     * 获取玩家剩余的拴住槽位
     */
    public static int getAvailableSlots(Player player) {
        int max = getMaxTethered(player);
        int occupied = countOccupiedSlots(player);
        return Math.max(0, max - occupied);
    }
}