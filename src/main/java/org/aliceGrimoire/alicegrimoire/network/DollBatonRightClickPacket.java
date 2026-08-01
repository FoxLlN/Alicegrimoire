package org.aliceGrimoire.alicegrimoire.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.item.baton.DollBatonItem;

public class DollBatonRightClickPacket implements CustomPacketPayload {
    public static final Type<DollBatonRightClickPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "baton_right"));

    private final boolean shiftDown;

    public DollBatonRightClickPacket(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public DollBatonRightClickPacket(FriendlyByteBuf buf) {
        this.shiftDown = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(shiftDown);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DollBatonRightClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DollBatonItem.handleRightClick(player, packet.shiftDown);
            }
        });
    }
}