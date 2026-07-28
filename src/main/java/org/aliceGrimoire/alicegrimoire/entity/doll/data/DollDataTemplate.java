package org.aliceGrimoire.alicegrimoire.entity.doll.data;

/**
 * 人偶数据模板
 * 用于创建不同职业的初始数据
 */
public record DollDataTemplate(
    // 基础属性
    double maxHealth,
    double damage,
    int armor,
    double armorToughness,
    double knockbackResistance,
    
    // 速度属性
    double wanderSpeed,
    double followSpeedMultiplier,
    double strikeSpeedMultiplier,
    
    // 移动属性
    double tetherRange,
    double flightSpeed,
    double turnSpeed,
    
    // 职业/状态
    DollJobType jobType,
    boolean hasShield,
    
    // 视觉属性
    int hairColor,
    int eyeColor,
    int ribbonColor,

    // 战斗参数
    CombatParameters combatParams
) {
    
    // ========== 默认模板 ==========
    // 标准人偶
    public static final DollDataTemplate DEFAULT = new DollDataTemplate(
        20.0, 2.0, 0, 0, 0,
        0.1, 1.1, 1.5,
        8.0, 1.0, 1.0,
        DollJobType.STANDARD, false,
        0xE8C8A0, 0x4A7A9C, 0xCC2233,
        createDefault()
    );
    
    // ========== 各职业预设模板 ==========
    // 近卫人偶
    public static final DollDataTemplate GUARD_TEMPLATE = new DollDataTemplate(
        32.0, 4.0, 4, 0, 0,
        0.1, 1.1, 1.5,
        4.0, 1.0, 1.0,
        DollJobType.GUARD, true,
        0xE8C8A0, 0x4A7A9C, 0x4488CC,
        createGuard()
    );
    
    // 守御人偶
    public static final DollDataTemplate DEFENDER_TEMPLATE = new DollDataTemplate(
        28.0, 2.0, 6, 2, 0.2,
        0.08, 1.0, 1.2,
        4.0, 0.8, 1.2,
        DollJobType.DEFENDER, true,
        0xE8C8A0, 0x4A7A9C, 0x88AA44,
        createDefender()
    );
    
    // 射手人偶
    public static final DollDataTemplate SHARPSHOOTER_TEMPLATE = new DollDataTemplate(
        16.0, 2.0, 0, 0, 0,
        0.12, 1.2, 1.8,
        8.0, 1.2, 1.5,
        DollJobType.SHARPSHOOTER, false,
        0xE8C8A0, 0x4A7A9C, 0xCC8844,
        createSharpshooter()
    );
    
    // 游击人偶
    public static final DollDataTemplate VANGUARD_TEMPLATE = new DollDataTemplate(
        24.0, 4.0, 2, 0, 0,
        0.12, 1.3, 2.0,
        8.0, 1.2, 1.2,
        DollJobType.VANGUARD, false,
        0xE8C8A0, 0x4A7A9C, 0xCC4488,
        createVanguard()
    );
    
    // ========== 工具方法 ==========
    public static DollDataTemplate getTemplateForJob(DollJobType jobType) {
        return switch (jobType) {
            case STANDARD -> DEFAULT;
            case GUARD -> GUARD_TEMPLATE;
            case DEFENDER -> DEFENDER_TEMPLATE;
            case SHARPSHOOTER -> SHARPSHOOTER_TEMPLATE;
            case VANGUARD -> VANGUARD_TEMPLATE;
            default -> DEFAULT;
        };
    }
    
    // ========== 战斗参数工厂方法 ==========
    
    // ============================================================
    // 工厂方法：创建各职业默认配置
    // ============================================================
    
    /**
     * 标准人偶默认配置
     */
    public static CombatParameters createDefault() {
        CombatParameters p = new CombatParameters();
        p.setAttackRange(2.0);
        p.setAttackVerticalRange(3.0);
        p.setAttackCooldown(10);
        p.setHoldDistance(1.0);
        p.setRetreatThreshold(3.0);
        p.setWaitDistance(2.0);
        p.setChargeSpeed(1.2);
        p.setRetreatSpeed(1.2);
        p.setWaitSpeed(0.4);
        p.setChargeDuration(15);
        p.setWaitDuration(40);
        p.setRecoveryDuration(60);
        p.setRangedMinDistance(8.0);
        p.setRangedMaxDistance(16.0);
        p.setRangedCooldown(25);
        p.setStrafeInterval(40);
        p.setGuardRadius(4.0);
        p.setShieldDisableTime(100);
        return p;
    }
    
    /**
     * 近卫人偶默认配置
     */
    public static CombatParameters createGuard() {
        CombatParameters p = new CombatParameters();
        p.setAttackRange(2.0);
        p.setAttackCooldown(8);
        p.setHoldDistance(2.0);
        p.setChargeSpeed(1.8);
        p.setRetreatSpeed(1.2);
        p.setWaitSpeed(0.8);
        p.setChargeDuration(15);
        p.setWaitDuration(40);
        p.setRangedMinDistance(2.0);
        p.setRangedMaxDistance(8.0);
        return p;
    }
    
    /**
     * 守御人偶默认配置
     */
    public static CombatParameters createDefender() {
        CombatParameters p = new CombatParameters();
        p.setAttackRange(1.5);
        p.setAttackCooldown(12);
        p.setHoldDistance(2.0);
        p.setWaitSpeed(0.3);
        p.setRecoveryDuration(80);
        p.setGuardSpeed(0.8);
        p.setGuardRadius(4.0);
        p.setShieldDisableTime(100);
        return p;
    }
    
    /**
     * 射手人偶默认配置
     */
    public static CombatParameters createSharpshooter() {
        CombatParameters p = new CombatParameters();
        p.setAttackRange(6.0);
        p.setAttackVerticalRange(4.0);
        p.setAttackCooldown(30);
        p.setHoldDistance(2.0);
        p.setChargeSpeed(1.0);
        p.setRetreatSpeed(1.5);
        p.setWaitSpeed(0.3);
        p.setChargeDuration(10);
        p.setWaitDuration(40);
        p.setRangedMinDistance(8.0);
        p.setRangedMaxDistance(16.0);
        p.setRangedCooldown(25);
        p.setStrafeInterval(40);
        return p;
    }
    
    /**
     * 游击人偶默认配置
     */
    public static CombatParameters createVanguard() {
        CombatParameters p = new CombatParameters();
        p.setAttackRange(2.0);
        p.setAttackCooldown(6);
        p.setAttackDelay(6);
        p.setHoldDistance(1.5);
        p.setChargeSpeed(2.0);
        p.setRetreatSpeed(1.5);
        p.setWaitSpeed(0.8);
        p.setChargeDuration(12);
        p.setWaitDuration(40);
        p.setRangedMinDistance(2.0);
        p.setRangedMaxDistance(8.0);
        return p;
    }
}