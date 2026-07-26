package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/**
 * 战斗参数封装
 * 包含所有职业通用的和职业特定的战斗参数
 * 所有参数都可通过织魔台修改
 */
public class CombatParameters {
    
    // ========== 通用近战参数 ==========
    private double attackRange = 1.5;          // 攻击水平距离
    private double attackVerticalRange = 3.0;   // 攻击垂直容差
    private double stopDistance = 1.0;          // 停止距离（近战）
    private int attackCooldown = 10;            // 攻击冷却 (tick)
    
    // ========== 近卫专属参数 ==========
    private int chargeDuration = 15;            // 冲锋持续 tick
    private double stickRange = 2.0;            // 黏住连击保持距离
    private double stickAttackRange = 2.5;      // 黏住时攻击距离
    
    // ========== 近卫远程专属 ==========
    private double shootRange = 2.0;            // 抵近射击保持距离
    private int shootCooldown = 20;             // 射击冷却
    
    // ========== 射手专属 ==========
    private double minDistance = 8.0;           // 最小距离
    private double maxDistance = 16.0;          // 最大距离
    private int strafeInterval = 40;            // 走位切换间隔
    private int sharpshooterCooldown = 25;      // 射手射击冷却
    
    // ========== 射手三叉戟专属 ==========
    private double tridentMinDistance = 6.0;
    private double tridentMaxDistance = 14.0;
    private int tridentCooldown = 30;
    
    // ========== 游击专属 ==========
    private int vanguardChargeDuration = 12;
    private double attackDistance = 1.5;
    
    // ========== 骑枪专属（未来扩展） ==========
    private int lancerChargeDelay = 100;
    private int lancerChargeDuration = 30;
    private double lancerChargeSpeed = 1.5;
    
    // ========== 反应策略参数 ==========
    private DamageReactionType reactionType = DamageReactionType.DEFAULT;
    
    // ========== Getter / Setter ==========
    // 通用近战
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = attackRange; }
    public double getAttackVerticalRange() { return attackVerticalRange; }
    public void setAttackVerticalRange(double attackVerticalRange) { this.attackVerticalRange = attackVerticalRange; }
    public double getStopDistance() { return stopDistance; }
    public void setStopDistance(double stopDistance) { this.stopDistance = stopDistance; }
    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int attackCooldown) { this.attackCooldown = attackCooldown; }
    
    // 近卫
    public int getChargeDuration() { return chargeDuration; }
    public void setChargeDuration(int chargeDuration) { this.chargeDuration = chargeDuration; }
    public double getStickRange() { return stickRange; }
    public void setStickRange(double stickRange) { this.stickRange = stickRange; }
    public double getStickAttackRange() { return stickAttackRange; }
    public void setStickAttackRange(double stickAttackRange) { this.stickAttackRange = stickAttackRange; }
    public double getShootRange() { return shootRange; }
    public void setShootRange(double shootRange) { this.shootRange = shootRange; }
    public int getShootCooldown() { return shootCooldown; }
    public void setShootCooldown(int shootCooldown) { this.shootCooldown = shootCooldown; }
    
    // 射手
    public double getMinDistance() { return minDistance; }
    public void setMinDistance(double minDistance) { this.minDistance = minDistance; }
    public double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(double maxDistance) { this.maxDistance = maxDistance; }
    public int getStrafeInterval() { return strafeInterval; }
    public void setStrafeInterval(int strafeInterval) { this.strafeInterval = strafeInterval; }
    public int getSharpshooterCooldown() { return sharpshooterCooldown; }
    public void setSharpshooterCooldown(int sharpshooterCooldown) { this.sharpshooterCooldown = sharpshooterCooldown; }
    
    // 射手三叉戟
    public double getTridentMinDistance() { return tridentMinDistance; }
    public void setTridentMinDistance(double tridentMinDistance) { this.tridentMinDistance = tridentMinDistance; }
    public double getTridentMaxDistance() { return tridentMaxDistance; }
    public void setTridentMaxDistance(double tridentMaxDistance) { this.tridentMaxDistance = tridentMaxDistance; }
    public int getTridentCooldown() { return tridentCooldown; }
    public void setTridentCooldown(int tridentCooldown) { this.tridentCooldown = tridentCooldown; }
    
    // 游击
    public int getVanguardChargeDuration() { return vanguardChargeDuration; }
    public void setVanguardChargeDuration(int vanguardChargeDuration) { this.vanguardChargeDuration = vanguardChargeDuration; }
    public double getAttackDistance() { return attackDistance; }
    public void setAttackDistance(double attackDistance) { this.attackDistance = attackDistance; }
    
    // 骑枪
    public int getLancerChargeDelay() { return lancerChargeDelay; }
    public void setLancerChargeDelay(int lancerChargeDelay) { this.lancerChargeDelay = lancerChargeDelay; }
    public int getLancerChargeDuration() { return lancerChargeDuration; }
    public void setLancerChargeDuration(int lancerChargeDuration) { this.lancerChargeDuration = lancerChargeDuration; }
    public double getLancerChargeSpeed() { return lancerChargeSpeed; }
    public void setLancerChargeSpeed(double lancerChargeSpeed) { this.lancerChargeSpeed = lancerChargeSpeed; }
    
    // 反应策略
    public DamageReactionType getReactionType() { return reactionType; }
    public void setReactionType(DamageReactionType reactionType) { this.reactionType = reactionType; }
    
    // ========== NBT 持久化 ==========
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("AttackRange", attackRange);
        tag.putDouble("AttackVerticalRange", attackVerticalRange);
        tag.putDouble("StopDistance", stopDistance);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putInt("ChargeDuration", chargeDuration);
        tag.putDouble("StickRange", stickRange);
        tag.putDouble("StickAttackRange", stickAttackRange);
        tag.putDouble("ShootRange", shootRange);
        tag.putInt("ShootCooldown", shootCooldown);
        tag.putDouble("MinDistance", minDistance);
        tag.putDouble("MaxDistance", maxDistance);
        tag.putInt("StrafeInterval", strafeInterval);
        tag.putInt("SharpshooterCooldown", sharpshooterCooldown);
        tag.putDouble("TridentMinDistance", tridentMinDistance);
        tag.putDouble("TridentMaxDistance", tridentMaxDistance);
        tag.putInt("TridentCooldown", tridentCooldown);
        tag.putInt("VanguardChargeDuration", vanguardChargeDuration);
        tag.putDouble("AttackDistance", attackDistance);
        tag.putInt("LancerChargeDelay", lancerChargeDelay);
        tag.putInt("LancerChargeDuration", lancerChargeDuration);
        tag.putDouble("LancerChargeSpeed", lancerChargeSpeed);
        tag.putString("ReactionType", reactionType.name());
        return tag;
    }
    
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        this.attackRange = tag.getDouble("AttackRange");
        this.attackVerticalRange = tag.getDouble("AttackVerticalRange");
        this.stopDistance = tag.getDouble("StopDistance");
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.chargeDuration = tag.getInt("ChargeDuration");
        this.stickRange = tag.getDouble("StickRange");
        this.stickAttackRange = tag.getDouble("StickAttackRange");
        this.shootRange = tag.getDouble("ShootRange");
        this.shootCooldown = tag.getInt("ShootCooldown");
        this.minDistance = tag.getDouble("MinDistance");
        this.maxDistance = tag.getDouble("MaxDistance");
        this.strafeInterval = tag.getInt("StrafeInterval");
        this.sharpshooterCooldown = tag.getInt("SharpshooterCooldown");
        this.tridentMinDistance = tag.getDouble("TridentMinDistance");
        this.tridentMaxDistance = tag.getDouble("TridentMaxDistance");
        this.tridentCooldown = tag.getInt("TridentCooldown");
        this.vanguardChargeDuration = tag.getInt("VanguardChargeDuration");
        this.attackDistance = tag.getDouble("AttackDistance");
        this.lancerChargeDelay = tag.getInt("LancerChargeDelay");
        this.lancerChargeDuration = tag.getInt("LancerChargeDuration");
        this.lancerChargeSpeed = tag.getDouble("LancerChargeSpeed");
        if (tag.contains("ReactionType")) {
            try {
                this.reactionType = DamageReactionType.valueOf(tag.getString("ReactionType"));
            } catch (Exception e) {
                this.reactionType = DamageReactionType.DEFAULT;
            }
        }
    }
    
    // ========== 复制 ==========
    public CombatParameters copy() {
        CombatParameters copy = new CombatParameters();
        // 复制所有字段
        copy.attackRange = this.attackRange;
        copy.attackVerticalRange = this.attackVerticalRange;
        copy.stopDistance = this.stopDistance;
        copy.attackCooldown = this.attackCooldown;
        copy.chargeDuration = this.chargeDuration;
        copy.stickRange = this.stickRange;
        copy.stickAttackRange = this.stickAttackRange;
        copy.shootRange = this.shootRange;
        copy.shootCooldown = this.shootCooldown;
        copy.minDistance = this.minDistance;
        copy.maxDistance = this.maxDistance;
        copy.strafeInterval = this.strafeInterval;
        copy.sharpshooterCooldown = this.sharpshooterCooldown;
        copy.tridentMinDistance = this.tridentMinDistance;
        copy.tridentMaxDistance = this.tridentMaxDistance;
        copy.tridentCooldown = this.tridentCooldown;
        copy.vanguardChargeDuration = this.vanguardChargeDuration;
        copy.attackDistance = this.attackDistance;
        copy.lancerChargeDelay = this.lancerChargeDelay;
        copy.lancerChargeDuration = this.lancerChargeDuration;
        copy.lancerChargeSpeed = this.lancerChargeSpeed;
        copy.reactionType = this.reactionType;
        return copy;
    }
}