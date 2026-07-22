package org.aliceGrimoire.alicegrimoire.entity.doll.movement;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

import com.mojang.logging.LogUtils;

/**
 * 自定义飞行移动控制，支持三维移动。
 * 设置目标位置和速度后，每帧将人偶向目标推进。
 */
public class DollMoveControl extends MoveControl {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private final DollEntity doll;

    public DollMoveControl(DollEntity doll) {
        super(doll);
        this.doll = doll;
    }

    @Override
    public void tick() {
        if (this.operation == Operation.MOVE_TO) {
            // LOGGER.info("[MoveControl] Moving to " + wantedX + "," + wantedY + "," + wantedZ + " speed " + speedModifier);
            Vec3 target = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
            Vec3 delta = target.subtract(doll.position());
            double distance = delta.length();
            if (distance < 0.5) {
                // 已经很近，停止移动
                doll.setDeltaMovement(Vec3.ZERO);
                this.operation = Operation.WAIT;
            } else {
                // 计算速度向量，确保不超过设定速度
                double speed = this.speedModifier;
                Vec3 direction = delta.normalize();
                // 如果目标在方块内，稍微抬高避免卡墙
                if (doll.level().getBlockState(doll.blockPosition()).isSolidRender(doll.level(), doll.blockPosition())) {
                    // 卡墙，尝试向上脱困
                    direction = direction.add(0, 1, 0).normalize();
                }
                Vec3 velocity = direction.scale(Math.min(speed, distance));
                doll.setDeltaMovement(velocity);
                // 转向
                doll.setYRot(-((float)Math.atan2(velocity.x, velocity.z)) * (180F / (float)Math.PI));
                doll.yBodyRot = doll.getYRot();
            }
        } else {
            // 没有目标时，缓慢减速
            doll.setDeltaMovement(doll.getDeltaMovement().scale(0.8));
        }
    }
}