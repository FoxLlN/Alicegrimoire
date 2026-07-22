package org.aliceGrimoire.alicegrimoire.registry;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Alicegrimoire.MODID);

    public static final Supplier<AttachmentType<Set<Integer>>> MARKED_TARGETS =
            ATTACHMENT_TYPES.register("marked_targets",
                () -> AttachmentType.<Set<Integer>>builder(() -> new HashSet<>())
                    .serialize(Codec.INT.listOf().xmap(HashSet::new, ArrayList::new))
                    .build());
}