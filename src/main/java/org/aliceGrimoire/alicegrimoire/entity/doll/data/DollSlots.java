package org.aliceGrimoire.alicegrimoire.entity.doll.data;

/**
 * 人偶物品栏槽位定义
 * 所有槽位索引集中管理，便于后续扩展
 */
public final class DollSlots {

    // ===== 战斗槽位 =====
    public static final int MAIN_HAND = 0;          // 主手（武器）
    public static final int OFF_HAND = 1;           // 副手（盾牌/副武器）

    // ===== 盔甲槽位 =====
    public static final int HELMET = 2;             // 头盔
    public static final int CHESTPLATE = 3;         // 胸甲
    public static final int LEGGINGS = 4;           // 护腿
    public static final int BOOTS = 5;              // 靴子

    // 扩展：额外的改装槽（可后续启用）
    // public static final int MOD_SLOT_1 = 6;
    // public static final int MOD_SLOT_2 = 7;

    public static final int INVENTORY_SIZE = 6;     // 当前总槽位数

    private DollSlots() {} // 禁止实例化

    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < INVENTORY_SIZE;
    }

    /**
     * 判断槽位是否为盔甲槽
     */
    public static boolean isArmorSlot(int slot) {
        return slot >= HELMET && slot <= BOOTS;
    }

    /**
     * 判断槽位是否为手持槽位
     */
    public static boolean isHandSlot(int slot) {
        return slot == MAIN_HAND || slot == OFF_HAND;
    }
}