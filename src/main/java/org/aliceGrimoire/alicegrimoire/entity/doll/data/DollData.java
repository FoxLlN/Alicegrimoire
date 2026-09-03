package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import java.util.ArrayList;
import java.util.List;

import org.aliceGrimoire.alicegrimoire.modifier.ModifierManager;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 人偶属性数据
 * 所有可配置的人偶属性集中管理
 * 支持从模板创建、运行时修改、NBT持久化
 */
public class DollData {
    
    // ========== 基础属性 ==========
    private double maxHealth;              // 最大生命值
    private double damage;                // 基础攻击伤害
    private int armor;                    // 护甲值
    private double armorToughness;        // 护甲韧性
    private double knockbackResistance;   // 击退抗性
    
    // ========== 速度属性 ==========
    private double wanderSpeed;           // 游荡速度 (格/tick)
    private double followSpeedMultiplier; // 跟随速度倍率 (相对于玩家速度)
    private double strikeSpeedMultiplier; // 出击速度倍率 (相对于跟随速度)
    
    // ========== 移动属性 ==========
    private double flightSpeed;           // 飞行速度 (整体速度倍率)
    private double turnSpeed;             // 转向速度

    // ========== 距离/范围属性 ==========
    private double tetherRange;           // 活动半径（游荡/跟随范围）
    private double enrageRange;           // 激怒触发范围
    private double dragStartRange;        // 强制拖回起始距离（= enrageRange）
    private double dragForceRange;        // 强制解除激怒距离（= enrageRange * 1.5）
    
    // ========== 拴住名额控制（供改装使用） ==========
    private boolean occupiesSlot;         // 是否占用拴住名额
    
    // ========== 职业/武器 ==========
    private DollJobType jobType;          // 职业类型
    
    // ========== 状态标志 ==========
    private boolean tethered;             // 是否被拴住（持久）
    private boolean isBroken;             // 是否破损
    private boolean hasShield;            // 是否持盾 (守御人偶专用)
    
    // ========== 视觉属性 ==========
    private int hairColor;                // 头发颜色 (RGB)
    private int eyeColor;                 // 瞳色 (RGB)
    private int ribbonColor;              // 缎带颜色 (RGB)
    
    // ========== 自定义扩展 ==========
    private CompoundTag customData;       // 自定义数据 (未来扩展)

    // ========== 背包装备 ==========
    private int backpackSlots = 9;          // 当前背包可用格数（默认9）
    private double pickupRange = 2.0;       // 拾取掉落物范围（格）
    private ItemStack[] inventory = new ItemStack[DollSlots.INVENTORY_SIZE];

    // ========== 战斗参数 ==========
    private CombatParameters combatParams = new CombatParameters();
    
    // ========== 改装组件列表 ==========
    private List<ItemStack> components = new ArrayList<>();

    // ========== 构造函数 ==========
    public DollData() {
        this(DollDataTemplate.DEFAULT);
    }
    
    public DollData(DollDataTemplate template) {
        // 初始化所有槽位为空
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        this.customData = new CompoundTag();

        applyTemplate(template);
    }
    
    // ========== 模板应用（仅设置来源，不直接赋值战斗属性） ==========
    public void applyTemplate(DollDataTemplate template) {
        // 保存职业类型（用于重算时获取模板）
        this.jobType = template.jobType();
        // 视觉属性
        this.hairColor = template.hairColor();
        this.eyeColor = template.eyeColor();
        this.ribbonColor = template.ribbonColor();
        // 非战斗属性（如击退抗性、速度乘数等，这些可能不被改装影响，但为了统一，也在这里设）
        this.knockbackResistance = template.knockbackResistance();
        this.wanderSpeed = template.wanderSpeed();
        this.followSpeedMultiplier = template.followSpeedMultiplier();
        this.strikeSpeedMultiplier = template.strikeSpeedMultiplier();
        this.turnSpeed = template.turnSpeed();
        this.enrageRange = template.enrageRange();
        this.dragStartRange = template.dragStartRange();
        this.dragForceRange = template.dragForceRange();
        this.occupiesSlot = template.occupiesSlot();
        this.hasShield = template.hasShield();
        // 战斗参数（CombatParameters）也由模板提供，并独立于改装
        this.combatParams = template.combatParams().copy();

        // 战斗属性（maxHealth, damage, armor, etc）全部由 recalculate 计算
        recalculate(); // 此时 components 可能为空，会使用模板默认值
    }

    // ========== 核心方法：全量重算战斗属性 ==========
    public void recalculate() {
        // 1. 根据当前职业获取模板的原始值
        DollDataTemplate template = DollDataTemplate.getTemplateForJob(this.jobType);
        double newHealth = template.maxHealth();
        double newDamage = template.damage();
        int newArmor = template.armor();
        double newToughness = template.armorToughness();
        double newFlightSpeed = template.flightSpeed();
        double newTetherRange = template.tetherRange();

        // 2. 应用所有组件的修正（堆叠数量影响效果）
        for (ItemStack stack : components) {
            if (stack.isEmpty()) continue;
            newHealth += ModifierManager.getModifiedValue(stack, "max_health");
            newDamage += ModifierManager.getModifiedValue(stack, "attack_damage");
            newArmor += (int) ModifierManager.getModifiedValue(stack, "armor");
            newToughness += ModifierManager.getModifiedValue(stack, "armor_toughness");
            newFlightSpeed += ModifierManager.getModifiedValue(stack, "flight_speed");
            newTetherRange += ModifierManager.getModifiedValue(stack, "tether_range");
        }

        // 3. 钳制并赋值（这就是唯一生效的当前值）
        this.maxHealth = Math.max(1, Math.min(200.0, newHealth));
        this.damage = Math.max(0, Math.min(50.0, newDamage));
        this.armor = (int) Math.max(0, Math.min(30, newArmor));
        this.armorToughness = Math.max(0, Math.min(10.0, newToughness));
        this.flightSpeed = Math.max(0.1, Math.min(2.0, newFlightSpeed));
        this.tetherRange = Math.max(1, Math.min(64.0, newTetherRange));
    }
    
    // ========== Getter / Setter ==========
    public double getMaxHealth() { return maxHealth; }
    public double getDamage() { return damage; }
    public int getArmor() { return armor; }
    public double getArmorToughness() { return armorToughness; }
    public double getKnockbackResistance() { return knockbackResistance; }
    
    public double getWanderSpeed() { return wanderSpeed; }
    public void setWanderSpeed(double speed) { this.wanderSpeed = speed; }
    public double getFollowSpeedMultiplier() { return followSpeedMultiplier; }
    public void setFollowSpeedMultiplier(double multiplier) { this.followSpeedMultiplier = multiplier; }
    public double getStrikeSpeedMultiplier() { return strikeSpeedMultiplier; }
    public void setStrikeSpeedMultiplier(double multiplier) { this.strikeSpeedMultiplier = multiplier; }
    
    public double getFlightSpeed() { return flightSpeed; }
    public double getTurnSpeed() { return turnSpeed; }
    
    public double getTetherRange() { return tetherRange; }
    public double getEnrageRange() { return enrageRange; }
    public void setEnrageRange(double range) { this.enrageRange = range; }
    public double getDragStartRange() { return dragStartRange; }
    public void setDragStartRange(double range) { this.dragStartRange = range; }
    public double getDragForceRange() { return dragForceRange; }
    public void setDragForceRange(double range) { this.dragForceRange = range; }

    public boolean getOccupiesSlot() { return occupiesSlot; }
    public void setOccupiesSlot(boolean occupiesSlot) { this.occupiesSlot = occupiesSlot; }

    public DollJobType getJobType() { return jobType; }
    public void setJobType(DollJobType jobType) {
        this.jobType = jobType;
        recalculate();
    }
    
    public void setComponents(List<ItemStack> components) {
        this.components = components != null ? new ArrayList<>(components) : new ArrayList<>();
        recalculate();
    }
    public List<ItemStack> getComponents() {
        return components;
    }
    
    public int getBackpackSlots() { return backpackSlots; }
    public void setBackpackSlots(int slots) { this.backpackSlots = Math.min(slots, DollSlots.MAX_BACKPACK_SLOTS); }

    public double getPickupRange() { return pickupRange; }
    public void setPickupRange(double range) { this.pickupRange = Math.max(0.5, range); }
    
    public ItemStack getItem(int slot) {
        if (!DollSlots.isValidSlot(slot)) return ItemStack.EMPTY;
        return inventory[slot];
    }

    public void setItem(int slot, ItemStack stack) {
        if (!DollSlots.isValidSlot(slot)) return;
        inventory[slot] = stack == null ? ItemStack.EMPTY : stack;
        // 如果设置的是主手，武器类型会自动更新（通过 getWeaponType()）
    }

    public ItemStack getWeapon() {
        return getItem(DollSlots.MAIN_HAND);
    }

    public void setWeapon(ItemStack weapon) {
        setItem(DollSlots.MAIN_HAND, weapon);
    }

    public ItemStack getOffHand() {
        return getItem(DollSlots.OFF_HAND);
    }
    
    public void setOffHand(ItemStack stack) {
        setItem(DollSlots.OFF_HAND, stack);
    }

    public WeaponType getWeaponType() {
        return WeaponType.fromItemStack(getWeapon());
    }
    
    public boolean isBroken() { return isBroken; }
    public void setBroken(boolean broken) { this.isBroken = broken; }
    public boolean hasShield() { return hasShield; }
    public void setHasShield(boolean hasShield) { this.hasShield = hasShield; }
    public boolean isTethered() { return tethered; }
    public void setTethered(boolean tethered) { this.tethered = tethered; }

    public int getHairColor() { return hairColor; }
    public void setHairColor(int hairColor) { this.hairColor = hairColor; }
    public int getEyeColor() { return eyeColor; }
    public void setEyeColor(int eyeColor) { this.eyeColor = eyeColor; }
    public int getRibbonColor() { return ribbonColor; }
    public void setRibbonColor(int ribbonColor) { this.ribbonColor = ribbonColor; }
    
    public CombatParameters getCombatParams() { return combatParams; }
    public void setCombatParams(CombatParameters combatParams) { 
        this.combatParams = combatParams; 
    }
    
    public ItemStack[] getInventory() {
        return inventory;
    }

    // ========== NBT 持久化 ==========
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        // 保存职业类型（用于恢复时重新计算）
        tag.putString("JobType", jobType.name());

        // 保存组件列表（用于恢复时重新计算修正）
        CompoundTag compTag = new CompoundTag();
        for (int i = 0; i < components.size(); i++) {
            if (!components.get(i).isEmpty()) {
                compTag.put("Comp" + i, components.get(i).save(registries));
            }
        }
        tag.put("Components", compTag);

        // 保存非战斗属性（直接从字段读取）
        tag.putDouble("KnockbackResistance", knockbackResistance);
        tag.putDouble("WanderSpeed", wanderSpeed);
        tag.putDouble("FollowSpeedMultiplier", followSpeedMultiplier);
        tag.putDouble("StrikeSpeedMultiplier", strikeSpeedMultiplier);
        tag.putDouble("TurnSpeed", turnSpeed);
        tag.putDouble("EnrageRange", enrageRange);
        tag.putDouble("DragStartRange", dragStartRange);
        tag.putDouble("DragForceRange", dragForceRange);
        tag.putBoolean("OccupiesSlot", occupiesSlot);
        tag.putBoolean("HasShield", hasShield);
        tag.putBoolean("Tethered", tethered);
        tag.putBoolean("IsBroken", isBroken);
        tag.putInt("HairColor", hairColor);
        tag.putInt("EyeColor", eyeColor);
        tag.putInt("RibbonColor", ribbonColor);
        tag.put("CustomData", customData);
        tag.putInt("BackpackSlots", backpackSlots);
        tag.putDouble("PickupRange", pickupRange);

        // 保存物品栏
        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                invTag.put("Slot" + i, inventory[i].save(registries));
            }
        }
        tag.put("Inventory", invTag);

        // 保存战斗参数（独立于改装）
        tag.put("CombatParams", combatParams.save(registries));

        return tag;
    }
    
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        // 加载职业类型（
        if (tag.contains("JobType")) {
            try {
                this.jobType = DollJobType.valueOf(tag.getString("JobType"));
            } catch (Exception e) {
                this.jobType = DollJobType.STANDARD; // 容错：未知类型回退为标准
            }
        } else {
            this.jobType = DollJobType.STANDARD; // 旧存档兼容
        }

        // 加载组件列表（recalculate 需要它来计算修正）
        this.components.clear();
        if (tag.contains("Components")) {
            CompoundTag compTag = tag.getCompound("Components");
            for (int i = 0; i < 8; i++) { // 最多 8 个组件（织魔台周围格子数）
                if (compTag.contains("Comp" + i)) {
                    ItemStack stack = ItemStack.parse(registries, compTag.getCompound("Comp" + i))
                            .orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty()) {
                        this.components.add(stack);
                    }
                }
            }
        }

        // 加载非战斗属性
        // 击退抗性
        this.knockbackResistance = tag.getDouble("KnockbackResistance");
        // 速度属性（游荡/跟随/出击的乘数）
        this.wanderSpeed = tag.getDouble("WanderSpeed");
        this.followSpeedMultiplier = tag.getDouble("FollowSpeedMultiplier");
        this.strikeSpeedMultiplier = tag.getDouble("StrikeSpeedMultiplier");
        // 转向速度
        this.turnSpeed = tag.getDouble("TurnSpeed");
        // 范围属性（激怒范围、拖拽范围等）
        this.enrageRange = tag.getDouble("EnrageRange");
        this.dragStartRange = tag.getDouble("DragStartRange");
        this.dragForceRange = tag.getDouble("DragForceRange");
        // 名额控制
        this.occupiesSlot = tag.getBoolean("OccupiesSlot");
        // 持盾标志
        this.hasShield = tag.getBoolean("HasShield");
        // 状态标志
        this.tethered = tag.getBoolean("Tethered");
        this.isBroken = tag.getBoolean("IsBroken");
        // 视觉属性
        this.hairColor = tag.getInt("HairColor");
        this.eyeColor = tag.getInt("EyeColor");
        this.ribbonColor = tag.getInt("RibbonColor");
        // 背包配置
        this.backpackSlots = tag.getInt("BackpackSlots");
        this.pickupRange = tag.getDouble("PickupRange");

        // 加载自定义数据（未来扩展）
        this.customData = tag.getCompound("CustomData").copy();

        // 加载物品栏（盔甲、武器、背包物品）
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            for (int i = 0; i < inventory.length; i++) {
                if (invTag.contains("Slot" + i)) {
                    inventory[i] = ItemStack.parse(registries, invTag.getCompound("Slot" + i))
                            .orElse(ItemStack.EMPTY);
                } else {
                    inventory[i] = ItemStack.EMPTY; // 确保未命中的槽位为空
                }
            }
        } else {
            // 旧存档兼容：如果没有 Inventory 字段，全部置空
            for (int i = 0; i < inventory.length; i++) {
                inventory[i] = ItemStack.EMPTY;
            }
        }

        // 加载战斗参数（CombatParameters - 独立于改装之外）
        if (tag.contains("CombatParams")) {
            combatParams.load(tag.getCompound("CombatParams"), registries);
        }

        // 根据职业 + 组件重新计算所有战斗属性
        recalculate();

        // 确保数据在合理范围内（防御性编程）
        this.backpackSlots = Math.max(0, Math.min(DollSlots.MAX_BACKPACK_SLOTS, this.backpackSlots));
        this.pickupRange = Math.max(0.5, Math.min(8.0, this.pickupRange));
    }
    
    // ========== 复制 ==========
    public DollData copy() {
        // 基于当前职业的纯净模板创建新对象
        DollDataTemplate template = DollDataTemplate.getTemplateForJob(this.jobType);
        DollData copy = new DollData(template);
        
        // 复制组件列表
        copy.components = new ArrayList<>(this.components);
        
        // 复制非战斗属性（这些不受模板影响）
        copy.knockbackResistance = this.knockbackResistance;
        copy.wanderSpeed = this.wanderSpeed;
        copy.followSpeedMultiplier = this.followSpeedMultiplier;
        copy.strikeSpeedMultiplier = this.strikeSpeedMultiplier;
        copy.turnSpeed = this.turnSpeed;
        copy.enrageRange = this.enrageRange;
        copy.dragStartRange = this.dragStartRange;
        copy.dragForceRange = this.dragForceRange;
        copy.occupiesSlot = this.occupiesSlot;
        copy.hasShield = this.hasShield;
        copy.tethered = this.tethered;
        copy.isBroken = this.isBroken;
        copy.hairColor = this.hairColor;
        copy.eyeColor = this.eyeColor;
        copy.ribbonColor = this.ribbonColor;
        copy.backpackSlots = this.backpackSlots;
        copy.pickupRange = this.pickupRange;
        copy.customData = this.customData.copy();
        
        // 复制物品栏
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                copy.inventory[i] = inventory[i].copy();
            }
        }
        
        // 复制战斗参数
        copy.combatParams = this.combatParams.copy();
        
        // 最后确保属性一致
        copy.recalculate();
        return copy;
    }

    // ========== 人偶背包相关辅助方法 ==========
    /**
     * 计算所有盔甲槽位的总护甲值
     */
    public int getTotalArmor() {
        int total = 0;
        for (int slot = DollSlots.HELMET; slot <= DollSlots.BOOTS; slot++) {
            ItemStack stack = inventory[slot];
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armorItem) {
                total += armorItem.getDefense();
            }
        }
        return total;
    }

    /**
     * 计算所有盔甲槽位的总护甲韧性
     */
    public float getTotalArmorToughness() {
        float total = 0;
        for (int slot = DollSlots.HELMET; slot <= DollSlots.BOOTS; slot++) {
            ItemStack stack = inventory[slot];
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armorItem) {
                total += armorItem.getToughness();
            }
        }
        return total;
    }

    /**
     * 获取主手武器的攻击力加成（基础伤害 + 武器攻击力）
     */
    public float getWeaponAttackDamage() {
        ItemStack weapon = getWeapon();
        if (weapon.isEmpty()) return (float) this.damage; // 空手使用基础伤害
        
        // 尝试从 AttributeModifiers 中读取 ATTACK_DAMAGE
        var modifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                    float bonus = (float) entry.modifier().amount();
                    return (float) this.damage + bonus;
                }
            }
        }
        
        // 如果武器没有 ATTACK_DAMAGE 修饰符，返回基础伤害
        return (float) this.damage;
    }
}