package org.aliceGrimoire.alicegrimoire.entity.doll.state;

/**
 * 人偶的四种核心状态，决定碰撞箱和攻击行为。
 * 状态转换由 DollStateManager 控制。
 */
public enum DollState {
    /**
     * 空闲游荡：无碰撞箱，随机移动（等同于没有拴住或拴住但未跟随）
     */
    IDLE,

    /**
     * 跟随玩家：无碰撞箱，同步跟随玩家移动（玩家主动移动时）
     */
    FOLLOWING,

    /**
     * 战斗姿态：有碰撞箱，执行攻击策略（激怒且有目标时）
     */
    ENGAGING,

    /**
     * 战斗后恢复：有碰撞箱，但无攻击行为，等待冷却结束（3秒）后切回非战斗状态
     */
    RECOVERING
}