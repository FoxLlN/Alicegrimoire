package org.aliceGrimoire.alicegrimoire.entity.doll.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

public class DollCollisionHelper {
    public static boolean isPositionSafe(DollEntity doll) {
        BlockState state = doll.level().getBlockState(doll.blockPosition());
        return !state.isSolidRender(doll.level(), doll.blockPosition());
    }

    public static void tryEscapeFromBlock(DollEntity doll) {
        Vec3[] attempts = {
            new Vec3(0.5, 0, 0),
            new Vec3(-0.5, 0, 0),
            new Vec3(0, 0, 0.5),
            new Vec3(0, 0, -0.5),
            new Vec3(0, 0.5, 0),
            new Vec3(0, -0.5, 0),
            new Vec3(1.0, 0, 0),
            new Vec3(-1.0, 0, 0),
            new Vec3(0, 0, 1.0),
            new Vec3(0, 0, -1.0)
        };
        BlockPos current = doll.blockPosition();
        for (Vec3 dir : attempts) {
            BlockPos test = current.offset((int) dir.x, (int) dir.y, (int) dir.z);
            if (!doll.level().getBlockState(test).isSolidRender(doll.level(), test)) {
                Vec3 target = doll.position().add(dir);
                doll.moveTo(target.x, target.y, target.z);
                // 清除速度，避免卡墙后继续冲
                doll.setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        // 如果全失败，向上瞬移2格（最后方案）
        Vec3 up = doll.position().add(0, 2, 0);
        if (!doll.level().getBlockState(BlockPos.containing(up)).isSolidRender(doll.level(), BlockPos.containing(up))) {
            doll.moveTo(up.x, up.y, up.z);
            doll.setDeltaMovement(Vec3.ZERO);
        }
    }
}