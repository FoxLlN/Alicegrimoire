package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
        doll.syncEquipmentToClient();
    }
    
    // ========== 应用数据到实体 ==========
    public void applyDataToEntity() {
        // ----- 基础属性 -----
        // 最大生命值（来自模板 + 改装）
        doll.getAttribute(Attributes.MAX_HEALTH)
            .setBaseValue(data.getMaxHealth());
        
        // 攻击力 = 基础攻击（模板+改装） + 武器加成
        float attackDamage = data.getWeaponAttackDamage();
        doll.getAttribute(Attributes.ATTACK_DAMAGE)
            .setBaseValue(attackDamage);
        
        // 总护甲 = 基础护甲（模板+改装） + 盔甲护甲
        int baseArmor = data.getArmor();
        int armorFromEquipment = data.getTotalArmor();
        doll.getAttribute(Attributes.ARMOR)
            .setBaseValue(baseArmor + armorFromEquipment);
        
        // 总护甲韧性 = 基础韧性（模板+改装） + 盔甲韧性
        double baseToughness = data.getArmorToughness();
        float toughnessFromEquipment = data.getTotalArmorToughness();
        doll.getAttribute(Attributes.ARMOR_TOUGHNESS)
            .setBaseValue(baseToughness + toughnessFromEquipment);
        
        // 其他属性
        doll.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
            .setBaseValue(data.getKnockbackResistance());
        doll.getAttribute(Attributes.FLYING_SPEED)
            .setBaseValue(data.getFlightSpeed());
        
        // 如果当前生命值超过新上限，调整
        if (doll.getHealth() > doll.getMaxHealth()) {
            doll.setHealth(doll.getMaxHealth());
        }
    }
    
    // ========== 策略工厂 ==========
    private ICombatStrategy createStrategy() {
        DollJobType job = data.getJobType();
        
        return switch (job) {
            // === 标准AI ===
            case STANDARD -> new MeleeStrategy();
            
            // === 近卫AI（四步循环） ===
            case GUARD -> new GuardStrategy();
            
            // === 守御AI（举盾保护） ===
            case DEFENDER -> new DefenderStrategy();

            // === 狙击AI（远程攻击） ===
            case SHARPSHOOTER -> new SharpshooterStrategy();

            // === 游走AI（灵活移动） ===
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
    
    // ========== 职业切换（保留组件，仅更换模板） ==========
    public void setJobType(DollJobType newJob) {
        // 1. 保存当前状态（组件、武器、视觉等）
        List<ItemStack> currentComponents = new ArrayList<>(data.getComponents());
        ItemStack currentWeapon = data.getWeapon();
        ItemStack currentOffHand = data.getOffHand();
        int hairColor = data.getHairColor();
        int eyeColor = data.getEyeColor();
        int ribbonColor = data.getRibbonColor();
        boolean isBroken = data.isBroken();
        boolean isTethered = data.isTethered();
        
        // 2. 切换职业（这会清空 components 并重置所有属性）
        data.setJobType(newJob); // DollData.setJobType 内部调用 recalculate()
        
        // 3. 恢复组件和状态（重新触发 recalculate）
        data.setComponents(currentComponents);
        data.setWeapon(currentWeapon);
        data.setOffHand(currentOffHand);
        data.setHairColor(hairColor);
        data.setEyeColor(eyeColor);
        data.setRibbonColor(ribbonColor);
        data.setBroken(isBroken);
        data.setTethered(isTethered);
        
        // 4. 应用到实体
        applyDataToEntity();
        refreshStrategy();
        doll.syncEquipmentToClient();
    }
    
    // ========== 组件管理（推荐的外部修改入口） ==========
    public void setComponents(List<ItemStack> components) {
        data.setComponents(components); // 内部触发 recalculate
        applyDataToEntity();
        refreshStrategy(); // 如果组件包含武器，可能影响策略
        doll.syncEquipmentToClient();
    }
    
    public void addComponent(ItemStack component) {
        List<ItemStack> comps = new ArrayList<>(data.getComponents());
        comps.add(component);
        data.setComponents(comps);
        applyDataToEntity();
        refreshStrategy();
        doll.syncEquipmentToClient();
    }

    // ---------- 物品栏操作 ----------
    public ItemStack getItem(int slot) {
        return data.getItem(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        if (!DollSlots.isValidSlot(slot)) return;
        // ===== 检查背包槽位是否超出当前可用容量 =====
        if (DollSlots.isBackpackSlot(slot)) {
            int backpackIndex = slot - DollSlots.BACKPACK_START;
            if (backpackIndex >= data.getBackpackSlots()) {
                return; // 超出可用背包格数，拒绝放入
            }
        }
        
        data.setItem(slot, stack);
        
        // ===== 刷新属性（无论什么槽位变化，护甲/攻击力都可能改变） =====
        applyDataToEntity();
        
        // 如果主手变化，需要刷新战斗策略
        if (slot == DollSlots.MAIN_HAND) {
            refreshStrategy();
        }
        
        // 同步到客户端
        doll.syncEquipmentToClient();
    }

    // ---------- 武器设置 ----------
    public void setWeapon(ItemStack weapon) {
        setItem(DollSlots.MAIN_HAND, weapon);
    }

    // 新增获取副手的方法
    public ItemStack getOffHand() {
        return data.getOffHand();
    }

    // ---------- 武器类型获取（直接透传） ----------
    public WeaponType getWeaponType() {
        return data.getWeaponType();
    }
    
    // ========== NBT 持久化 ==========
    public CompoundTag save(HolderLookup.Provider registries) {
        return data.save(registries);
    }
    
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        data.load(tag, registries);
        applyDataToEntity();
        refreshStrategy();
        doll.syncEquipmentToClient();
    }
}