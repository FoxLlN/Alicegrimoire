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
                new Vec3(0, 0.5, 0),
                new Vec3(0.5, 0, 0),
                new Vec3(-0.5, 0, 0),
                new Vec3(0, 0, 0.5),
                new Vec3(0, 0, -0.5)
        };
        BlockPos current = doll.blockPosition();
        for (Vec3 dir : attempts) {
            BlockPos test = current.offset((int) dir.x, (int) dir.y, (int) dir.z);
            if (!doll.level().getBlockState(test).isSolidRender(doll.level(), test)) {
                Vec3 target = doll.position().add(dir);
                doll.moveTo(target.x, target.y, target.z);
                return;
            }
        }
    }
}