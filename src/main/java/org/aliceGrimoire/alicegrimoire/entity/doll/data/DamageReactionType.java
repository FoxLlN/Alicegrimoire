package org.aliceGrimoire.alicegrimoire.entity.doll.data;

/**
 * 伤害反应类型
 * 定义人偶在战斗中对伤害的反应方式
 * 可通过织魔台改装切换
 */
public enum DamageReactionType {
    /**
     * 默认：生命值实际减少后撤回
     */
    DEFAULT,
    
    /**
     * 预判闪避：在伤害命中前闪避（未来实现）
     */
    PREEMPTIVE,
    
    /**
     * 硬抗：受伤后不撤回，继续攻击（未来实现）
     */
    TANK,
    
    /**
     * 反击：受伤后立即反击而非撤回（未来实现）
     */
    COUNTER_ATTACK
}