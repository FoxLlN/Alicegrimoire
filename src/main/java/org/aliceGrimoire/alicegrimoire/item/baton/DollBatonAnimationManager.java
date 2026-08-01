package org.aliceGrimoire.alicegrimoire.item.baton;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 指挥棒动画状态管理器
 * 后续重新设计动画时，只需修改此类
 */
public class DollBatonAnimationManager {

    // ===== 动画定义 =====
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenPlay("idle");
    // 为后续预留（现在注释掉，方便以后启用）
    // private static final RawAnimation START_SPIN = RawAnimation.begin().thenPlay("startSpin");
    // private static final RawAnimation SPINNING = RawAnimation.begin().thenPlay("spinning");
    // private static final RawAnimation END_SPIN = RawAnimation.begin().thenPlay("endSpin");

    // ===== 状态控制（暂未使用，保留给后续） =====
    private static boolean isSpinning = false;
    private static boolean isEnding = false;

    /**
     * 核心动画处理逻辑
     * 目前只播放 idle，后续可在此扩展
     */
    public static PlayState handleAnimation(AnimationState<?> state) {
        // 当前只播放 idle 动画
        state.setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    /**
     * 开始旋转（占位，后续实现）
     */
    public static void startSpinning() {
        // 暂时不实现，为后续重做保留
    }

    /**
     * 停止旋转（占位，后续实现）
     */
    public static void stopSpinning() {
        // 暂时不实现，为后续重做保留
    }

    /**
     * 重置动画状态（占位）
     */
    public static void resetAnimation() {
        isSpinning = false;
        isEnding = false;
    }
}