package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;

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
    private double tetherRange;           // 拴绳范围 (活动半径)
    private double attackRange;           // 攻击距离 (近战)
    private double attackVerticalRange;   // 攻击垂直容差
    private int attackCooldown;           // 攻击冷却 (tick)
    
    // ========== 飞行属性 ==========
    private double flightSpeed;           // 飞行速度 (整体速度倍率)
    private double turnSpeed;             // 转向速度
    
    // ========== 职业/武器 ==========
    private DollJobType jobType;          // 职业类型
    private ItemStack weapon;             // 手持武器
    private WeaponType weaponType;        // 武器类型 (根据武器自动判断)
    
    // ========== 状态标志 ==========
    private boolean isBroken;             // 是否破损
    private boolean hasShield;            // 是否持盾 (守御人偶专用)
    
    // ========== 视觉属性 ==========
    private int hairColor;                // 头发颜色 (RGB)
    private int eyeColor;                 // 瞳色 (RGB)
    private int ribbonColor;              // 缎带颜色 (RGB)
    
    // ========== 自定义扩展 ==========
    private CompoundTag customData;       // 自定义数据 (未来扩展)
    
    // ========== 构造函数 ==========
    public DollData() {
        this(DollDataTemplate.DEFAULT);
    }
    
    public DollData(DollDataTemplate template) {
        applyTemplate(template);
        this.weapon = ItemStack.EMPTY;
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
        this.tetherRange = template.tetherRange();
        this.attackRange = template.attackRange();
        this.attackVerticalRange = template.attackVerticalRange();
        this.attackCooldown = template.attackCooldown();
        this.flightSpeed = template.flightSpeed();
        this.turnSpeed = template.turnSpeed();
        this.jobType = template.jobType();
        this.hasShield = template.hasShield();
        this.hairColor = template.hairColor();
        this.eyeColor = template.eyeColor();
        this.ribbonColor = template.ribbonColor();
        this.isBroken = false;
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
    
    public double getTetherRange() { return tetherRange; }
    public void setTetherRange(double range) { this.tetherRange = range; }
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double range) { this.attackRange = range; }
    public double getAttackVerticalRange() { return attackVerticalRange; }
    public void setAttackVerticalRange(double range) { this.attackVerticalRange = range; }
    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int cooldown) { this.attackCooldown = cooldown; }
    
    public double getFlightSpeed() { return flightSpeed; }
    public void setFlightSpeed(double speed) { this.flightSpeed = speed; }
    public double getTurnSpeed() { return turnSpeed; }
    
    public DollJobType getJobType() { return jobType; }
    public void setJobType(DollJobType jobType) { this.jobType = jobType; }
    public ItemStack getWeapon() { return weapon; }
    public void setWeapon(ItemStack weapon) { 
        this.weapon = weapon;
        this.weaponType = detectWeaponType(weapon);
    }
    public WeaponType getWeaponType() { return weaponType; }
    
    public boolean isBroken() { return isBroken; }
    public void setBroken(boolean broken) { this.isBroken = broken; }
    public boolean hasShield() { return hasShield; }
    public void setHasShield(boolean hasShield) { this.hasShield = hasShield; }
    
    public int getHairColor() { return hairColor; }
    public void setHairColor(int hairColor) { this.hairColor = hairColor; }
    public int getEyeColor() { return eyeColor; }
    public void setEyeColor(int eyeColor) { this.eyeColor = eyeColor; }
    public int getRibbonColor() { return ribbonColor; }
    public void setRibbonColor(int ribbonColor) { this.ribbonColor = ribbonColor; }
    
    // ========== 辅助方法 ==========
    private WeaponType detectWeaponType(ItemStack stack) {
        return WeaponType.fromItemStack(stack);
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
        tag.putDouble("TetherRange", tetherRange);
        tag.putDouble("AttackRange", attackRange);
        tag.putDouble("AttackVerticalRange", attackVerticalRange);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putDouble("FlightSpeed", flightSpeed);
        tag.putDouble("TurnSpeed", turnSpeed);
        tag.putString("JobType", jobType.name());
        tag.putBoolean("IsBroken", isBroken);
        tag.putBoolean("HasShield", hasShield);
        tag.putInt("HairColor", hairColor);
        tag.putInt("EyeColor", eyeColor);
        tag.putInt("RibbonColor", ribbonColor);
        if (!weapon.isEmpty()) {
            tag.put("Weapon", weapon.save(registries));
        }
        tag.put("CustomData", customData);
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
        this.tetherRange = tag.getDouble("TetherRange");
        this.attackRange = tag.getDouble("AttackRange");
        this.attackVerticalRange = tag.getDouble("AttackVerticalRange");
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.flightSpeed = tag.getDouble("FlightSpeed");
        this.turnSpeed = tag.getDouble("TurnSpeed");
        this.jobType = DollJobType.valueOf(tag.getString("JobType"));
        this.isBroken = tag.getBoolean("IsBroken");
        this.hasShield = tag.getBoolean("HasShield");
        this.hairColor = tag.getInt("HairColor");
        this.eyeColor = tag.getInt("EyeColor");
        this.ribbonColor = tag.getInt("RibbonColor");
        if (tag.contains("Weapon")) {
            this.weapon = ItemStack.parse(registries, tag.getCompound("Weapon")).orElse(ItemStack.EMPTY);
            this.weaponType = detectWeaponType(this.weapon);
        }
        this.customData = tag.getCompound("CustomData");
    }
    
    // ========== 复制 ==========
    public DollData copy() {
        DollData copy = new DollData();
        copy.applyTemplate(new DollDataTemplate(
            maxHealth, damage, armor, armorToughness, knockbackResistance,
            wanderSpeed, followSpeedMultiplier, strikeSpeedMultiplier,
            tetherRange, attackRange, attackVerticalRange, attackCooldown,
            flightSpeed, turnSpeed, jobType, hasShield,
            hairColor, eyeColor, ribbonColor
        ));
        if (!weapon.isEmpty()) {
            copy.setWeapon(weapon.copy());
        }
        copy.isBroken = this.isBroken;
        copy.customData = this.customData.copy();
        return copy;
    }
}