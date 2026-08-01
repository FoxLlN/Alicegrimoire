package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType;

import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Alicegrimoire.MODID);

    // 人偶篮存储的人偶列表
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> DOLLS =
            DATA_COMPONENT_TYPES.register("dolls",
                () -> DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .cacheEncoding()
                    .build());

    // 人偶类型（用于物品形式存储）
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DollJobType>> DOLL_TYPE =
            DATA_COMPONENT_TYPES.register("doll_type",
                () -> DataComponentType.<DollJobType>builder()
                    .persistent(DollJobType.CODEC)
                    .networkSynchronized(DollJobType.STREAM_CODEC)
                    .build());

    // 织魔台改装组件列表
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> COMPONENTS =
            DATA_COMPONENT_TYPES.register("components",
                () -> DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());
}