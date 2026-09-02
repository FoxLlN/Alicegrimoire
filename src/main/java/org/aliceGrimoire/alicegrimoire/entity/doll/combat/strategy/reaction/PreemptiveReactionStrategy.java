package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 预判闪避策略
 * 在目标攻击命中前主动闪避并撤回
 * 
 * 原理：检测目标是否正在执行攻击动作（如挥剑动画）
 * 在攻击命中的前一刻触发闪避
 */
public class PreemptiveReactionStrategy implements IDamageReactionStrategy {
    
    private static final int PREDICT_WINDOW = 5; // 提前 5 tick 预判
    private int attackDetectCounter = 0;
    
    @Override
    public boolean shouldRetreat(DollEntity doll, LivingEntity target, 
                                 LivingEntity owner, float currentHealth, 
                                 float lastHealth, int phaseTicks) {
        // 检测目标是否正在执行攻击
        // 方法1：检查目标的攻击动画（原版 LivingEntity 的 swingTime）
        // 方法2：检查目标是否刚刚造成了伤害（通过事件监听）
        // 方法3：检查目标是否正在使用物品（如弩蓄力）
        
        if (target != null && target.swinging) {
            // 目标正在攻击，预判闪避
            attackDetectCounter++;
            if (attackDetectCounter > PREDICT_WINDOW) {
                attackDetectCounter = 0;
                return true; // 预判到攻击，闪避撤回
            }
        } else {
            attackDetectCounter = 0;
        }
        
        // 如果实际受伤了，也触发撤回（兼容）
        if (currentHealth < lastHealth) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getName() {
        return "预判闪避（提前闪避攻击）";
    }
    
    /**
     * 重置状态
     */
    @Override
    public void reset() {
        this.attackDetectCounter = 0;
    }
}