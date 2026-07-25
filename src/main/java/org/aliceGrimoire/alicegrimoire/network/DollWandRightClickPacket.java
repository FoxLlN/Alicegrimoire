package org.aliceGrimoire.alicegrimoire.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.item.DollWandItem;

public class DollWandRightClickPacket implements CustomPacketPayload {
    public static final Type<DollWandRightClickPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "wand_right"));

    public DollWandRightClickPacket() {}

    public DollWandRightClickPacket(FriendlyByteBuf buf) {}

    public void write(FriendlyByteBuf buf) {}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DollWandRightClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DollWandItem.handleRightClick(player);
            }
        });
    }
}