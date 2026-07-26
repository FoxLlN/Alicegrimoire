package org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.reaction;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 默认伤害反应策略
 * 只有生命值实际减少时才触发撤回
 * 这是当前近卫人偶的默认行为
 */
public class DefaultReactionStrategy implements IDamageReactionStrategy {
    
    private float lastHealth = -1;
    
    @Override
    public boolean shouldRetreat(DollEntity doll, LivingEntity target, 
                                 LivingEntity owner, float currentHealth, 
                                 float lastHealth, int phaseTicks) {
        // 首次进入阶段，初始化生命值记录
        if (this.lastHealth < 0) {
            this.lastHealth = currentHealth;
            return false;
        }
        
        // 检查生命值是否实际减少
        if (currentHealth < this.lastHealth) {
            this.lastHealth = currentHealth;
            return true; // 生命值减少，触发撤回
        }
        
        this.lastHealth = currentHealth;
        return false;
    }
    
    @Override
    public String getName() {
        return "默认（受伤后撤回）";
    }
    
    /**
     * 重置状态（每次进入 STICKING 阶段时调用）
     */
    public void reset() {
        this.lastHealth = -1;
    }
}