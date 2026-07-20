package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.block.DollBlock;
import org.aliceGrimoire.alicegrimoire.block.MagiweaverBlock;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Alicegrimoire.MODID);

    // Placeholder blocks
    public static final DeferredBlock<Block> MAGIWEAVER = BLOCKS.register("magiweaver", 
        () -> new MagiweaverBlock(BlockBehaviour.Properties.of().strength(2.5f)));
    
    public static final DeferredBlock<Block> DOLL = BLOCKS.register("doll", 
        () -> new DollBlock(BlockBehaviour.Properties.of().strength(0.1f).noOcclusion()));
}
