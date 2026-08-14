package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;

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
    private ItemStack[] inventory = new ItemStack[DollSlots.INVENTORY_SIZE];

    // ========== 战斗参数 ==========
    private CombatParameters combatParams = new CombatParameters();
    
    // ========== 构造函数 ==========
    public DollData() {
        this(DollDataTemplate.DEFAULT);
    }
    
    public DollData(DollDataTemplate template) {
        applyTemplate(template);
        // 初始化所有槽位为空
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        this.customData = new CompoundTag();
    }
    
    // ========== 模板应用 ==========
    public void applyTemplate(DollDataTemplate template) {
        this.maxHealth = template.maxHealth();
        this.damage = template.damage();
        this.armor = template.armor();
        this.armorToughness = template.armorToughness();
        this.knockbackResistance = template.knockbackResistance();
        this.wanderSpeed = template.wanderSpeed();
        this.followSpeedMultiplier = template.followSpeedMultiplier();
        this.strikeSpeedMultiplier = template.strikeSpeedMultiplier();
        this.flightSpeed = template.flightSpeed();
        this.turnSpeed = template.turnSpeed();
        this.tetherRange = template.tetherRange();
        this.enrageRange = template.enrageRange();
        this.dragStartRange = template.dragStartRange();
        this.dragForceRange = template.dragForceRange();
        this.occupiesSlot = template.occupiesSlot();
        this.jobType = template.jobType();
        this.hasShield = template.hasShield();
        this.hairColor = template.hairColor();
        this.eyeColor = template.eyeColor();
        this.ribbonColor = template.ribbonColor();
        this.tethered = false;
        this.isBroken = false;
        // ===== 复制战斗参数 =====
        this.combatParams = template.combatParams().copy();
    }
    
    // ========== Getter / Setter ==========
    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public int getArmor() { return armor; }
    public void setArmor(int armor) { this.armor = armor; }
    public double getArmorToughness() { return armorToughness; }
    public void setArmorToughness(double armorToughness) { this.armorToughness = armorToughness; }
    public double getKnockbackResistance() { return knockbackResistance; }
    
    public double getWanderSpeed() { return wanderSpeed; }
    public void setWanderSpeed(double speed) { this.wanderSpeed = speed; }
    public double getFollowSpeedMultiplier() { return followSpeedMultiplier; }
    public void setFollowSpeedMultiplier(double multiplier) { this.followSpeedMultiplier = multiplier; }
    public double getStrikeSpeedMultiplier() { return strikeSpeedMultiplier; }
    public void setStrikeSpeedMultiplier(double multiplier) { this.strikeSpeedMultiplier = multiplier; }
    
    public double getFlightSpeed() { return flightSpeed; }
    public void setFlightSpeed(double speed) { this.flightSpeed = speed; }
    public double getTurnSpeed() { return turnSpeed; }
    
    public double getTetherRange() { return tetherRange; }
    public void setTetherRange(double range) { this.tetherRange = range; }
    public double getEnrageRange() { return enrageRange; }
    public void setEnrageRange(double range) { this.enrageRange = range; }
    public double getDragStartRange() { return dragStartRange; }
    public void setDragStartRange(double range) { this.dragStartRange = range; }
    public double getDragForceRange() { return dragForceRange; }
    public void setDragForceRange(double range) { this.dragForceRange = range; }

    public boolean getOccupiesSlot() { return occupiesSlot; }
    public void setOccupiesSlot(boolean occupiesSlot) { this.occupiesSlot = occupiesSlot; }

    public DollJobType getJobType() { return jobType; }
    public void setJobType(DollJobType jobType) { this.jobType = jobType; }
    
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
        tag.putDouble("MaxHealth", maxHealth);
        tag.putDouble("Damage", damage);
        tag.putInt("Armor", armor);
        tag.putDouble("ArmorToughness", armorToughness);
        tag.putDouble("KnockbackResistance", knockbackResistance);
        tag.putDouble("WanderSpeed", wanderSpeed);
        tag.putDouble("FollowSpeedMultiplier", followSpeedMultiplier);
        tag.putDouble("StrikeSpeedMultiplier", strikeSpeedMultiplier);
        tag.putDouble("FlightSpeed", flightSpeed);
        tag.putDouble("TurnSpeed", turnSpeed);
        tag.putDouble("TetherRange", tetherRange);
        tag.putDouble("EnrageRange", enrageRange);
        tag.putDouble("DragStartRange", dragStartRange);
        tag.putDouble("DragForceRange", dragForceRange);
        tag.putBoolean("OccupiesSlot", occupiesSlot);
        tag.putString("JobType", jobType.name());
        tag.putBoolean("IsBroken", isBroken);
        tag.putBoolean("HasShield", hasShield);
        tag.putBoolean("Tethered", tethered);
        tag.putInt("HairColor", hairColor);
        tag.putInt("EyeColor", eyeColor);
        tag.putInt("RibbonColor", ribbonColor);
        tag.put("CustomData", customData);

        // 保存物品栏
        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                invTag.put("Slot" + i, inventory[i].save(registries));
            }
        }
        tag.put("Inventory", invTag);

        // 保存战斗参数
        tag.put("CombatParams", combatParams.save(registries));

        return tag;
    }
    
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        this.maxHealth = tag.getDouble("MaxHealth");
        this.damage = tag.getDouble("Damage");
        this.armor = tag.getInt("Armor");
        this.armorToughness = tag.getDouble("ArmorToughness");
        this.knockbackResistance = tag.getDouble("KnockbackResistance");
        this.wanderSpeed = tag.getDouble("WanderSpeed");
        this.followSpeedMultiplier = tag.getDouble("FollowSpeedMultiplier");
        this.strikeSpeedMultiplier = tag.getDouble("StrikeSpeedMultiplier");
        this.flightSpeed = tag.getDouble("FlightSpeed");
        this.turnSpeed = tag.getDouble("TurnSpeed");
        this.tetherRange = tag.getDouble("TetherRange");
        this.enrageRange = tag.getDouble("EnrageRange");
        this.dragStartRange = tag.getDouble("DragStartRange");
        this.dragForceRange = tag.getDouble("DragForceRange");
        this.occupiesSlot = tag.getBoolean("OccupiesSlot");
        this.jobType = DollJobType.valueOf(tag.getString("JobType"));
        this.tethered = tag.getBoolean("Tethered");
        this.isBroken = tag.getBoolean("IsBroken");
        this.hasShield = tag.getBoolean("HasShield");
        this.hairColor = tag.getInt("HairColor");
        this.eyeColor = tag.getInt("EyeColor");
        this.ribbonColor = tag.getInt("RibbonColor");
        this.customData = tag.getCompound("CustomData");

        // 加载物品栏
        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            for (int i = 0; i < inventory.length; i++) {
                if (invTag.contains("Slot" + i)) {
                    inventory[i] = ItemStack.parse(registries, invTag.getCompound("Slot" + i))
                            .orElse(ItemStack.EMPTY);
                }
            }
        }

        // 加载战斗参数
        if (tag.contains("CombatParams")) {
            combatParams.load(tag.getCompound("CombatParams"), registries);
        }
    }
    
    // ========== 复制 ==========
    public DollData copy() {
        DollData copy = new DollData();
        copy.applyTemplate(new DollDataTemplate(
            maxHealth, damage, armor, armorToughness, knockbackResistance,
            wanderSpeed, followSpeedMultiplier, strikeSpeedMultiplier,
            flightSpeed, turnSpeed, 
            tetherRange, enrageRange, dragStartRange, dragForceRange,
            occupiesSlot,
            jobType, hasShield,
            hairColor, eyeColor, ribbonColor,
            combatParams.copy() // 传递副本
        ));
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                copy.inventory[i] = inventory[i].copy();
            }
        }
        copy.isBroken = this.isBroken;
        copy.customData = this.customData.copy();
        copy.tethered = this.tethered;
        return copy;
    }
}