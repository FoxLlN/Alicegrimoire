package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Alicegrimoire.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DollEntity>> DOLL = ENTITIES.register("doll", 
        () -> EntityType.Builder.of(DollEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 2.0f)
            .build("doll"));
}
