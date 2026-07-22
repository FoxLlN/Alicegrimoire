package org.aliceGrimoire.alicegrimoire.entity.doll.combat;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 战斗策略接口，定义不同类型人偶的战斗行为（移动和攻击）。
 * 每个职业（DollType）对应一个实现类。
 */
public interface ICombatStrategy {

    /**
     * 每帧调用，执行战斗逻辑（移动、攻击等）。
     * 仅在 DollState.ENGAGING 状态下被调用。
     *
     * @param doll   人偶实体
     * @param target 当前攻击目标（非空）
     * @param owner  人偶主人
     */
    void tick(DollEntity doll, LivingEntity target, LivingEntity owner);

    /**
     * 是否正在攻击（用于动画、渲染等）。
     * 例如近战挥舞、远程拉弓状态。
     *
     * @return true 如果当前正在攻击动画中
     */
    boolean isAttacking();
}