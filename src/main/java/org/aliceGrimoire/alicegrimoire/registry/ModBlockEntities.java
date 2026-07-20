package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.block.DollBlockEntity;
import org.aliceGrimoire.alicegrimoire.block.MagiweaverBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Alicegrimoire.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DollBlockEntity>> DOLL = BLOCK_ENTITIES.register("doll", 
        () -> BlockEntityType.Builder.of(DollBlockEntity::new, ModBlocks.DOLL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagiweaverBlockEntity>> MAGIWEAVER = BLOCK_ENTITIES.register("magiweaver", 
        () -> BlockEntityType.Builder.of(MagiweaverBlockEntity::new, ModBlocks.MAGIWEAVER.get()).build(null));
}
