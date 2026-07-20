package org.aliceGrimoire.alicegrimoire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.aliceGrimoire.alicegrimoire.block.DollBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DollBlock extends Block implements EntityBlock {
    public DollBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DollBlockEntity(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof DollBlockEntity) {
            for (ItemStack stack : drops) {
                if (stack.getItem() instanceof org.aliceGrimoire.alicegrimoire.item.DollItem) {
                    stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(be.saveCustomOnly(be.getLevel().registryAccess())));
                }
            }
        }
        return drops;
    }
}
