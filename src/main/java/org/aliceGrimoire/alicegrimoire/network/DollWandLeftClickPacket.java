package org.aliceGrimoire.alicegrimoire.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.item.DollWandItem;

public class DollWandLeftClickPacket implements CustomPacketPayload {
    public static final Type<DollWandLeftClickPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "wand_left"));

    public DollWandLeftClickPacket() {}

    public DollWandLeftClickPacket(FriendlyByteBuf buf) {}

    public void write(FriendlyByteBuf buf) {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DollWandLeftClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DollWandItem.handleLeftClick(player);
            }
        });
    }
}