package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;

/**
 * 武器类型
 * 用于决定攻击方式和动画
 * 
 * 检测优先级：
 * 1. 模组自定义标签（整合包可配置）
 * 2. 原版物品标签（自动兼容其他模组武器）
 * 3. instanceof（兼容性保底）
 * 4. 硬编码特殊物品
 */
public enum WeaponType {
    NONE,          // 空手
    SWORD,         // 剑类 (铁剑/钻石剑等)
    SHIELD,        // 盾牌
    CROSSBOW,      // 弩
    TRIDENT,       // 三叉戟
    BOW,           // 弓
    SPECIAL;       // 特殊武器 (TNT/药水/风弹/打火石/尖啸体)

    // ========== 模组自定义标签 ==========
    // 整合包制作者可通过数据包修改这些标签
    private static final TagKey<Item> SWORD_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "weapons/sword")
    );
    private static final TagKey<Item> SHIELD_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "weapons/shield")
    );
    private static final TagKey<Item> BOW_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "weapons/bow")
    );
    private static final TagKey<Item> CROSSBOW_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "weapons/crossbow")
    );
    private static final TagKey<Item> TRIDENT_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "weapons/trident")
    );

    /**
     * 从 ItemStack 检测武器类型
     * 
     * @param stack 要检测的物品
     * @return 对应的武器类型
     */
    public static WeaponType fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return NONE;

        Item item = stack.getItem();

        // ===== 1. 优先使用模组自定义标签 =====
        // 整合包制作者可以通过数据包自由配置
        if (stack.is(SWORD_TAG)) return SWORD;
        if (stack.is(SHIELD_TAG)) return SHIELD;
        if (stack.is(BOW_TAG)) return BOW;
        if (stack.is(CROSSBOW_TAG)) return CROSSBOW;
        if (stack.is(TRIDENT_TAG)) return TRIDENT;

        // ===== 2. 使用原版标签（自动兼容其他模组） =====
        if (stack.is(ItemTags.SWORDS)) return SWORD;
        // 注意：ItemTags 中没有通用的 BOWS 标签，但有 INSTANT_DAMAGE 等
        // 原版没有统一的 BOWS 标签，但我们可以使用 ItemTags 中存在的其他标签

        // ===== 3. instanceof（兼容性保底） =====
        if (item instanceof SwordItem) return SWORD;
        if (item instanceof ShieldItem) return SHIELD;
        if (item instanceof BowItem) return BOW;
        if (item instanceof CrossbowItem) return CROSSBOW;
        if (item instanceof TridentItem) return TRIDENT;

        // ===== 4. 特殊武器硬编码 =====
        if (item == Items.TNT || item == Items.TNT_MINECART) return SPECIAL;
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return SPECIAL;
        if (item == Items.FIRE_CHARGE) return SPECIAL;
        if (item == Items.FLINT_AND_STEEL) return SPECIAL;
        if (item == Items.SCULK_SHRIEKER) return SPECIAL;

        return NONE;
    }
}