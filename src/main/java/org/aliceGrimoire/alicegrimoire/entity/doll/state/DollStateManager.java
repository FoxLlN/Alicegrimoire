package org.aliceGrimoire.alicegrimoire.entity.doll.state;

import net.minecraft.world.entity.LivingEntity;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.util.DollCollisionHelper;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * 管理 DollEntity 的状态转换，以及碰撞箱的开启/关闭。
 * 状态转换规则：
 * - IDLE ↔ FOLLOWING：取决于是否拴住且玩家主动移动。
 * - FOLLOWING → ENGAGING：当 isEnraged=true 且有有效目标。
 * - ENGAGING → RECOVERING：当目标死亡或 isEnraged=false。
 * - RECOVERING → FOLLOWING/IDLE：冷却计时结束（60 ticks）。
 * 碰撞箱仅在 ENGAGING 和 RECOVERING 时开启，但必须在位置安全的前提下。
 */
public class DollStateManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final DollEntity doll;
    private DollState currentState = DollState.IDLE;
    private int recoveryTicks = 0;          // 冷却剩余 tick 数
    private int obstructedTicks = 0;        // 拴绳被阻挡累计时间

    public DollStateManager(DollEntity doll) {
        this.doll = doll;
    }

    public DollState getCurrentState() {
        return currentState;
    }

    /**
     * 每帧调用，执行状态转换和碰撞箱控制。
     */
    public void tick() {
        // 添加日志
        // LOGGER.info("[DollState] Current State: " + currentState + 
        //                 ", isEnraged: " + doll.isEnraged() + 
        //                 ", hasTarget: " + (doll.getTarget() != null) +
        //                 ", isTethered: " + doll.isTethered() +
        //                 ", isPlayerMoving: " + doll.isPlayerActivelyMoving());
                    
        // 1. 获取基础条件
        boolean isEnraged = doll.isEnraged();
        boolean hasValidTarget = doll.getTarget() != null && doll.getTarget().isAlive();
        boolean isTethered = doll.isTethered();          // 由主 tick 更新
        boolean isPlayerMoving = doll.isPlayerActivelyMoving();

        // 2. 检测拴绳是否被方块阻挡（用于颜色和激怒自动解除）
        updateObstructedStatus();

        // 3. 根据当前状态进行转换
        switch (currentState) {
            case IDLE:
                // 如果拴住且玩家在移动，进入跟随
                if (isTethered && isPlayerMoving) {
                    setState(DollState.FOLLOWING);
                }
                // 如果被激怒且有目标，直接进入战斗（即使玩家不动）
                if (isEnraged && hasValidTarget) {
                    setState(DollState.ENGAGING);
                }
                break;

            case FOLLOWING:
                // 解除拴住 -> 回到空闲
                if (!isTethered) {
                    setState(DollState.IDLE);
                }
                // 玩家停止移动 -> 空闲
                else if (!isPlayerMoving) {
                    setState(DollState.IDLE);
                }
                // 激怒且有目标 -> 战斗
                else if (isEnraged && hasValidTarget) {
                    setState(DollState.ENGAGING);
                }
                break;

            case ENGAGING:
                // 如果失去激怒或目标死亡 -> 进入冷却恢复
                if (!isEnraged || !hasValidTarget) {
                    setState(DollState.RECOVERING);
                    recoveryTicks = 60;   // 3秒冷却
                }
                // 额外：如果拴绳被阻挡超过3秒，自动解除激怒（由外部触发 setEnraged(false)）
                // 此处检测到阻塞时间过长，主动解除激怒（但由外部调用，我们在这里仅标记）
                if (obstructedTicks > 60) {
                    doll.setEnraged(false);  // 会触发状态切换
                }
                break;

            case RECOVERING:
                // 冷却倒计时
                if (recoveryTicks > 0) {
                    recoveryTicks--;
                } else {
                    // 冷却结束，回到跟随或空闲
                    if (isTethered) {
                        setState(DollState.FOLLOWING);
                    } else {
                        setState(DollState.IDLE);
                    }
                }
                break;
        }

        // 4. 应用碰撞箱（由状态决定，并确保位置安全）
        boolean shouldHavePhysics = (currentState == DollState.ENGAGING || 
                                    currentState == DollState.RECOVERING || 
                                    doll.isReturning()); // 返回模式也开启碰撞箱

        if (shouldHavePhysics) {
            // 必须位置安全才开启碰撞箱，否则保持无碰撞并尝试脱困
            if (doll.isPositionSafe()) {
                doll.noPhysics = false;
            } else {
                doll.noPhysics = true;
                // 尝试脱困（瞬移）
                DollCollisionHelper.tryEscapeFromBlock(doll);
            }
        } else {
            doll.noPhysics = true;
        }
        
        doll.refreshDimensions(); // 更新碰撞箱 
    }

    /**
     * 强制设置状态（供外部事件触发，例如当人偶被强制解除激怒时）
     */
    public void setState(DollState newState) {
        if (this.currentState == newState) return;
        this.currentState = newState;

        // 状态进入时的特殊处理
        if (newState == DollState.RECOVERING) {
            recoveryTicks = 60;      // 进入恢复状态即开始冷却
        }
    }

    /**
     * 更新栓绳被阻挡的累计时间（用于颜色和自动解除激怒）
     */
    private void updateObstructedStatus() {
        LivingEntity owner = doll.getOwner();
        if (owner == null) {
            obstructedTicks = 0;
            return;
        }
        // 简单射线检测（从人偶眼睛到主人眼睛）
        var start = doll.position().add(0, 1.0, 0);
        var end = owner.getEyePosition();
        var clip = doll.level().clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                doll
        ));
        boolean blocked = clip.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
        if (blocked) {
            obstructedTicks++;
        } else {
            obstructedTicks = 0;
        }
    }

    // ========== 供渲染器使用的方法 ==========
    public int getObstructedTicks() {
        return obstructedTicks;
    }

    public boolean isInsideBlock() {
        return doll.level().getBlockState(doll.blockPosition()).isSolidRender(doll.level(), doll.blockPosition());
    }
}