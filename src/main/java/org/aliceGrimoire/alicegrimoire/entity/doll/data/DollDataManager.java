package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.*;

/**
 * 人偶数据管理器
 * 负责数据的读写、应用和同步
 */
public class DollDataManager {
    
    private final DollEntity doll;
    private DollData data;
    private ICombatStrategy currentStrategy;
    
    public DollDataManager(DollEntity doll) {
        this.doll = doll;
        this.data = new DollData();
        this.currentStrategy = createStrategy();
    }
    
    // ========== 数据访问 ==========
    public DollData getData() { return data; }
    
    public void setData(DollData data) {
        this.data = data;
        applyDataToEntity();
        refreshStrategy();
    }
    
    // ========== 应用数据到实体 ==========
    public void applyDataToEntity() {
        doll.getAttribute(Attributes.MAX_HEALTH)
            .setBaseValue(data.getMaxHealth());
        doll.getAttribute(Attributes.ATTACK_DAMAGE)
            .setBaseValue(data.getDamage());
        doll.getAttribute(Attributes.ARMOR)
            .setBaseValue(data.getArmor());
        doll.getAttribute(Attributes.ARMOR_TOUGHNESS)
            .setBaseValue(data.getArmorToughness());
        doll.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
            .setBaseValue(data.getKnockbackResistance());
        doll.getAttribute(Attributes.FLYING_SPEED)
            .setBaseValue(data.getFlightSpeed());
        
        // 如果当前生命值超过新上限，调整
        if (doll.getHealth() > doll.getMaxHealth()) {
            doll.setHealth(doll.getMaxHealth());
        }
    }
    
    // ========== 策略工厂（核心） ==========
    private ICombatStrategy createStrategy() {
        DollJobType job = data.getJobType();
        WeaponType weapon = data.getWeaponType();
        
        return switch (job) {
            // === 标准AI ===
            case STANDARD -> new MeleeStrategy();
            
            // === 近卫AI（四步循环） ===
            case GUARD -> new GuardStrategy();
            
            // === 守御AI（举盾保护） ===
            case DEFENDER -> new DefenderStrategy();
            
            // === 射手AI（远离+走位+射击） ===
            case SHARPSHOOTER -> {
                if (weapon == WeaponType.TRIDENT) {
                    yield new SharpshooterTridentStrategy(); // 三叉戟投射（像溺尸）
                } else {
                    yield new SharpshooterStrategy();        // 弓/弩射击（不耗箭）
                }
            }
            
            // === 游击AI（一击脱离） ===
            case VANGUARD -> new VanguardStrategy();
            
            default -> new MeleeStrategy();
        };
    }
    
    private void refreshStrategy() {
        this.currentStrategy = createStrategy();
    }
    
    public ICombatStrategy getCurrentStrategy() {
        return currentStrategy;
    }
    
    // ========== 职业切换 ==========
    public void setJobType(DollJobType newJob) {
        DollDataTemplate template = DollDataTemplate.getTemplateForJob(newJob);
        // 保留当前武器和颜色，仅更新职业相关属性
        ItemStack currentWeapon = data.getWeapon();
        int hairColor = data.getHairColor();
        int eyeColor = data.getEyeColor();
        int ribbonColor = data.getRibbonColor();
        boolean isBroken = data.isBroken();
        
        data.applyTemplate(template);
        data.setWeapon(currentWeapon);
        data.setHairColor(hairColor);
        data.setEyeColor(eyeColor);
        data.setRibbonColor(ribbonColor);
        data.setBroken(isBroken);
        
        applyDataToEntity();
        refreshStrategy();
    }
    
    // ========== 属性修改器 ==========
    public void addHealth(double amount) {
        data.setMaxHealth(Math.max(1, data.getMaxHealth() + amount));
        applyDataToEntity();
    }
    
    public void addDamage(double amount) {
        data.setDamage(Math.max(0, data.getDamage() + amount));
        applyDataToEntity();
    }
    
    public void addArmor(int amount) {
        data.setArmor(Math.max(0, data.getArmor() + amount));
        applyDataToEntity();
    }
    
    public void addTetherRange(double amount) {
        data.setTetherRange(Math.max(1, data.getTetherRange() + amount));
    }
    
    public void addAttackSpeed(int amount) {
        data.setAttackCooldown(Math.max(1, data.getAttackCooldown() - amount));
    }
    
    // ========== 武器设置 ==========
    public void setWeapon(ItemStack weapon) {
        data.setWeapon(weapon);
        refreshStrategy(); // 武器改变可能影响策略
    }
    
    // ========== NBT 持久化 ==========
    public CompoundTag save(HolderLookup.Provider registries) {
        return data.save(registries);
    }
    
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        data.load(tag, registries);
        applyDataToEntity();
        refreshStrategy();
    }
}