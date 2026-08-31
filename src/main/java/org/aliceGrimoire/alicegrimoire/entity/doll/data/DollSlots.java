package org.aliceGrimoire.alicegrimoire.entity.doll.data;

/**
 * 人偶物品栏槽位定义
 * 总槽位：固定6个（主手/副手/盔甲）+ 可配置背包（默认9格）
 */
public final class DollSlots {

    // ===== 固定槽位（索引0-5） =====
    public static final int MAIN_HAND = 0;
    public static final int OFF_HAND = 1;
    public static final int HELMET = 2;
    public static final int CHESTPLATE = 3;
    public static final int LEGGINGS = 4;
    public static final int BOOTS = 5;

    // ===== 背包槽位（索引6开始，数量由 DollData.backpackSlots 决定） =====
    public static final int BACKPACK_START = 6;
    // 最大背包格数（预留扩展）
    public static final int MAX_BACKPACK_SLOTS = 27; 
    // 总槽位数 = 固定6 + 最大背包27 = 33
    public static final int INVENTORY_SIZE = 6 + MAX_BACKPACK_SLOTS;

    private DollSlots() {}

    public static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < INVENTORY_SIZE;
    }

    public static boolean isArmorSlot(int slot) {
        return slot >= HELMET && slot <= BOOTS;
    }

    public static boolean isHandSlot(int slot) {
        return slot == MAIN_HAND || slot == OFF_HAND;
    }

    public static boolean isBackpackSlot(int slot) {
        return slot >= BACKPACK_START && slot < BACKPACK_START + MAX_BACKPACK_SLOTS;
    }

    /**
     * 判断槽位是否可用于自动拾取（只能放入背包，不能直接替换盔甲/武器）
     * 实际使用时，我们不会自动放入固定槽，仅当玩家手动放置或装备逻辑触发。
     */
    public static boolean isPickupSlot(int slot) {
        return isBackpackSlot(slot);
    }
}