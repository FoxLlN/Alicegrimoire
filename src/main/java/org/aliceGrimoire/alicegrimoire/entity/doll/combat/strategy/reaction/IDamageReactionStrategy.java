package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 伤害反应策略接口
 * 决定人偶在受到伤害后的行为
 * 可通过改装系统动态切换实现
 */
@FunctionalInterface
public interface IDamageReactionStrategy {
    
    /**
     * 判断是否应该撤回（退出战斗）
     * 
     * @param doll           人偶实体
     * @param target         当前攻击目标
     * @param owner          人偶主人
     * @param currentHealth  当前生命值
     * @param lastHealth     上一 tick 的生命值
     * @param phaseTicks     当前阶段已持续 tick 数
     * @return true 表示应该撤回，false 表示继续攻击
     */
    boolean shouldRetreat(DollEntity doll, LivingEntity target, 
                          LivingEntity owner, float currentHealth, 
                          float lastHealth, int phaseTicks);
    
    /**
     * 获取策略的名称（用于显示在织魔台）
     */
    default String getName() {
        return "default";
    }
    
    /**
     * 重置策略内部状态
     * 当人偶切换目标或重新进入 STICKING 阶段时调用
     * 默认实现为空，子类按需覆盖
     */
    default void reset() {
        // 默认无操作
    }
}