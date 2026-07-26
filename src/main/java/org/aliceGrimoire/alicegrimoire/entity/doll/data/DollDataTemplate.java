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
    double attackRange,
    double attackVerticalRange,
    int attackCooldown,
    
    // 飞行属性
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
        8.0, 1.5, 3.0, 10,
        1.0, 1.0,
        DollJobType.STANDARD, false,
        0xE8C8A0, 0x4A7A9C, 0xCC2233,
        createDefaultCombatParams()
    );
    
    // ========== 各职业预设模板 ==========
    // 近卫人偶
    public static final DollDataTemplate GUARD_TEMPLATE = new DollDataTemplate(
        32.0, 4.0, 4, 0, 0,
        0.1, 1.1, 1.5,
        4.0, 2.0, 3.0, 8,
        1.0, 1.0,
        DollJobType.GUARD, true,
        0xE8C8A0, 0x4A7A9C, 0x4488CC,
        createGuardCombatParams()
    );
    
    // 守御人偶
    public static final DollDataTemplate DEFENDER_TEMPLATE = new DollDataTemplate(
        28.0, 2.0, 6, 2, 0.2,
        0.08, 1.0, 1.2,
        4.0, 1.5, 2.0, 12,
        0.8, 1.2,
        DollJobType.DEFENDER, true,
        0xE8C8A0, 0x4A7A9C, 0x88AA44,
        createDefaultCombatParams()
    );
    
    // 射手人偶
    public static final DollDataTemplate SHARPSHOOTER_TEMPLATE = new DollDataTemplate(
        16.0, 2.0, 0, 0, 0,
        0.12, 1.2, 1.8,
        8.0, 6.0, 4.0, 30,
        1.2, 1.5,
        DollJobType.SHARPSHOOTER, false,
        0xE8C8A0, 0x4A7A9C, 0xCC8844,
        createSharpshooterCombatParams()
    );
    
    // 游击人偶
    public static final DollDataTemplate VANGUARD_TEMPLATE = new DollDataTemplate(
        24.0, 4.0, 2, 0, 0,
        0.12, 1.3, 2.0,
        8.0, 2.0, 3.0, 6,
        1.2, 1.2,
        DollJobType.VANGUARD, false,
        0xE8C8A0, 0x4A7A9C, 0xCC4488,
        createVanguardCombatParams()
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
    
    // 默认人偶战斗参数
    private static CombatParameters createDefaultCombatParams() {
        return new CombatParameters();
    }
    
    // 近卫人偶战斗参数
    private static CombatParameters createGuardCombatParams() {
        CombatParameters params = new CombatParameters();
        params.setChargeDuration(15);
        params.setStickRange(2.0);
        params.setStickAttackRange(2.5);
        params.setAttackCooldown(8);
        params.setAttackRange(2.0);
        return params;
    }
    
    // 射手人偶战斗参数
    private static CombatParameters createSharpshooterCombatParams() {
        CombatParameters params = new CombatParameters();
        params.setMinDistance(8.0);
        params.setMaxDistance(16.0);
        params.setStrafeInterval(40);
        params.setSharpshooterCooldown(25);
        params.setTridentMinDistance(6.0);
        params.setTridentMaxDistance(14.0);
        params.setTridentCooldown(30);
        return params;
    }
    
    // 游击人偶战斗参数
    private static CombatParameters createVanguardCombatParams() {
        CombatParameters params = new CombatParameters();
        params.setVanguardChargeDuration(12);
        params.setAttackDistance(1.5);
        return params;
    }
}