package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * 人偶战斗参数 - 精简版
 * 
 * 设计原则：
 * 1. 统一命名：相同语义使用相同字段名（如 holdDistance 统一了 stickRange/shootRange）
 * 2. 按职业分组：用注释标明哪些参数属于哪个职业，便于理解和维护
 * 3. 参数化硬编码值：所有速度、距离、时间等数值均可通过织魔台修改
 * 4. 轻量化：每个实例只持有一组参数，不存在无用字段
 * 
 * 参数层级：
 * - 通用参数（所有职业共有）
 * - 远程专用（射手/近卫远程）
 * - 守御专用
 */
public class CombatParameters {
    
    // ============================================================
    // 一、通用参数（所有职业共用）
    // ============================================================
    
    // ---------- 攻击判定 ----------
    private double attackRange = 1.5;           // 攻击水平距离（格）
    private double attackVerticalRange = 3.0;   // 攻击垂直容差（格）
    private int attackCooldown = 10;            // 攻击冷却（tick）
    private int attackDelay = 6;                // 攻击后进入撤回的延迟（tick）
    
    // ---------- 距离控制 ----------
    private double holdDistance = 1.0;          // 战斗时与目标保持的距离（格）
    private double retreatThreshold = 3.0;      // 撤回完成阈值（距离玩家 ≤ 此值即完成撤回）
    private double waitDistance = 2.0;          // 等待时与玩家的跟随距离（格）
    
    // ---------- 速度控制（倍率，相对于玩家速度） ----------
    private double chargeSpeed = 1.8;           // 冲锋速度倍率
    private double retreatSpeed = 1.2;          // 撤回速度倍率
    private double waitSpeed = 0.4;             // 等待时跟随速度倍率
    
    // ---------- 时间控制 ----------
    private int chargeDuration = 10;            // 冲锋持续（tick）
    private int waitDuration = 40;              // 等待基础时间（tick），实际会叠加随机偏移
    private int recoveryDuration = 60;          // 战斗后恢复冷却（tick）
    
    // ============================================================
    // 二、远程专用参数（射手职业 / 近卫使用远程武器时）
    // ============================================================
    
    private double rangedMinDistance = 8.0;     // 远程最小攻击距离（格）
    private double rangedMaxDistance = 16.0;    // 远程最大攻击距离（格）
    private int rangedCooldown = 25;            // 远程射击冷却（tick）
    private int strafeInterval = 40;            // 走位切换间隔（tick）
    
    // ============================================================
    // 三、守御专用参数
    // ============================================================
    
    private double guardSpeed = 0.8;            // 守护速度倍率
    private double guardRadius = 4.0;           // 保护半径（格）
    private int shieldDisableTime = 100;        // 破盾恢复时间（tick，5秒）
    
    // ============================================================
    // 四、反应策略参数（所有职业共用）
    // ============================================================
    
    private DamageReactionType reactionType = DamageReactionType.DEFAULT;
    
    // ============================================================
    // Getter / Setter
    // ============================================================
    
    // ---------- 攻击判定 ----------
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = attackRange; }
    
    public double getAttackVerticalRange() { return attackVerticalRange; }
    public void setAttackVerticalRange(double attackVerticalRange) { this.attackVerticalRange = attackVerticalRange; }
    
    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int attackCooldown) { this.attackCooldown = attackCooldown; }
      
    public int getAttackDelay() { return attackDelay; }
    public void setAttackDelay(int attackDelay) { this.attackDelay = attackDelay; }
      
    // ---------- 距离控制 ----------
    public double getHoldDistance() { return holdDistance; }
    public void setHoldDistance(double holdDistance) { this.holdDistance = holdDistance; }
    
    public double getRetreatThreshold() { return retreatThreshold; }
    public void setRetreatThreshold(double retreatThreshold) { this.retreatThreshold = retreatThreshold; }
    
    public double getWaitDistance() { return waitDistance; }
    public void setWaitDistance(double waitDistance) { this.waitDistance = waitDistance; }
    
    // ---------- 速度控制 ----------
    public double getChargeSpeed() { return chargeSpeed; }
    public void setChargeSpeed(double chargeSpeed) { this.chargeSpeed = chargeSpeed; }
    
    public double getRetreatSpeed() { return retreatSpeed; }
    public void setRetreatSpeed(double retreatSpeed) { this.retreatSpeed = retreatSpeed; }
    
    public double getWaitSpeed() { return waitSpeed; }
    public void setWaitSpeed(double waitSpeed) { this.waitSpeed = waitSpeed; }
    
    // ---------- 时间控制 ----------
    public int getChargeDuration() { return chargeDuration; }
    public void setChargeDuration(int chargeDuration) { this.chargeDuration = chargeDuration; }
    
    public int getWaitDuration() { return waitDuration; }
    public void setWaitDuration(int waitDuration) { this.waitDuration = waitDuration; }
    
    public int getRecoveryDuration() { return recoveryDuration; }
    public void setRecoveryDuration(int recoveryDuration) { this.recoveryDuration = recoveryDuration; }
    
    // ---------- 远程 ----------
    public double getRangedMinDistance() { return rangedMinDistance; }
    public void setRangedMinDistance(double rangedMinDistance) { this.rangedMinDistance = rangedMinDistance; }
    
    public double getRangedMaxDistance() { return rangedMaxDistance; }
    public void setRangedMaxDistance(double rangedMaxDistance) { this.rangedMaxDistance = rangedMaxDistance; }
    
    public int getRangedCooldown() { return rangedCooldown; }
    public void setRangedCooldown(int rangedCooldown) { this.rangedCooldown = rangedCooldown; }
    
    public int getStrafeInterval() { return strafeInterval; }
    public void setStrafeInterval(int strafeInterval) { this.strafeInterval = strafeInterval; }
    
    // ---------- 守御 ----------
    public double getGuardSpeed() { return guardSpeed; }
    public void setGuardSpeed(double guardSpeed) { this.guardSpeed = guardSpeed; }
    
    public double getGuardRadius() { return guardRadius; }
    public void setGuardRadius(double guardRadius) { this.guardRadius = guardRadius; }
    
    public int getShieldDisableTime() { return shieldDisableTime; }
    public void setShieldDisableTime(int shieldDisableTime) { this.shieldDisableTime = shieldDisableTime; }
    
    // ---------- 反应策略 ----------
    public DamageReactionType getReactionType() { return reactionType; }
    public void setReactionType(DamageReactionType reactionType) { this.reactionType = reactionType; }
    
    // ============================================================
    // NBT 持久化
    // ============================================================
    
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        
        // 通用
        tag.putDouble("AttackRange", attackRange);
        tag.putDouble("AttackVerticalRange", attackVerticalRange);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putDouble("HoldDistance", holdDistance);
        tag.putDouble("RetreatThreshold", retreatThreshold);
        tag.putDouble("WaitDistance", waitDistance);
        tag.putDouble("ChargeSpeed", chargeSpeed);
        tag.putDouble("RetreatSpeed", retreatSpeed);
        tag.putDouble("WaitSpeed", waitSpeed);
        tag.putInt("ChargeDuration", chargeDuration);
        tag.putInt("WaitDuration", waitDuration);
        tag.putInt("RecoveryDuration", recoveryDuration);
        
        // 远程
        tag.putDouble("RangedMinDistance", rangedMinDistance);
        tag.putDouble("RangedMaxDistance", rangedMaxDistance);
        tag.putInt("RangedCooldown", rangedCooldown);
        tag.putInt("StrafeInterval", strafeInterval);
        
        // 守御
        tag.putDouble("GuardRadius", guardRadius);
        tag.putInt("ShieldDisableTime", shieldDisableTime);
        
        // 反应策略
        tag.putString("ReactionType", reactionType.name());
        
        return tag;
    }
    
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        // 通用
        this.attackRange = tag.getDouble("AttackRange");
        this.attackVerticalRange = tag.getDouble("AttackVerticalRange");
        this.attackCooldown = tag.getInt("AttackCooldown");
        this.holdDistance = tag.getDouble("HoldDistance");
        this.retreatThreshold = tag.getDouble("RetreatThreshold");
        this.waitDistance = tag.getDouble("WaitDistance");
        this.chargeSpeed = tag.getDouble("ChargeSpeed");
        this.retreatSpeed = tag.getDouble("RetreatSpeed");
        this.waitSpeed = tag.getDouble("WaitSpeed");
        this.chargeDuration = tag.getInt("ChargeDuration");
        this.waitDuration = tag.getInt("WaitDuration");
        this.recoveryDuration = tag.getInt("RecoveryDuration");
        
        // 远程
        this.rangedMinDistance = tag.getDouble("RangedMinDistance");
        this.rangedMaxDistance = tag.getDouble("RangedMaxDistance");
        this.rangedCooldown = tag.getInt("RangedCooldown");
        this.strafeInterval = tag.getInt("StrafeInterval");
        
        // 守御
        this.guardRadius = tag.getDouble("GuardRadius");
        this.shieldDisableTime = tag.getInt("ShieldDisableTime");
        
        // 反应策略
        if (tag.contains("ReactionType")) {
            try {
                this.reactionType = DamageReactionType.valueOf(tag.getString("ReactionType"));
            } catch (Exception e) {
                this.reactionType = DamageReactionType.DEFAULT;
            }
        }
    }
    
    // ============================================================
    // 复制
    // ============================================================
    
    public CombatParameters copy() {
        CombatParameters copy = new CombatParameters();
        // 通用
        copy.attackRange = this.attackRange;
        copy.attackVerticalRange = this.attackVerticalRange;
        copy.attackCooldown = this.attackCooldown;
        copy.holdDistance = this.holdDistance;
        copy.retreatThreshold = this.retreatThreshold;
        copy.waitDistance = this.waitDistance;
        copy.chargeSpeed = this.chargeSpeed;
        copy.retreatSpeed = this.retreatSpeed;
        copy.waitSpeed = this.waitSpeed;
        copy.chargeDuration = this.chargeDuration;
        copy.waitDuration = this.waitDuration;
        copy.recoveryDuration = this.recoveryDuration;
        // 远程
        copy.rangedMinDistance = this.rangedMinDistance;
        copy.rangedMaxDistance = this.rangedMaxDistance;
        copy.rangedCooldown = this.rangedCooldown;
        copy.strafeInterval = this.strafeInterval;
        // 守御
        copy.guardRadius = this.guardRadius;
        copy.shieldDisableTime = this.shieldDisableTime;
        // 反应策略
        copy.reactionType = this.reactionType;
        return copy;
    }
}