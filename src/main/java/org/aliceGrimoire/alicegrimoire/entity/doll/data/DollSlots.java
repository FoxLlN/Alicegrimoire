package org.aliceGrimoire.alicegrimoire.entity.doll.data;

/**
 * 人偶物品栏槽位定义
 * 所有槽位索引集中管理，便于后续扩展
 */
public final class DollSlots {

    // ===== 战斗槽位 =====
    public static final int MAIN_HAND = 0;          // 主手（武器）
    public static final int OFF_HAND = 1;           // 副手（盾牌/副武器）

    // ===== 预留扩展（后续可启用） =====
    // public static final int MOD_SLOT_1 = 2;       // 改装槽1
    // public static final int MOD_SLOT_2 = 3;       // 改装槽2
    // public static final int BACKPACK_START = 4;   // 背包起始
    // public static final int BACKPACK_END = 7;     // 背包结束（共4格）

    public static final int INVENTORY_SIZE = 2;     // 当前总槽位数

    private DollSlots() {} // 禁止实例化

    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < INVENTORY_SIZE;
    }
}