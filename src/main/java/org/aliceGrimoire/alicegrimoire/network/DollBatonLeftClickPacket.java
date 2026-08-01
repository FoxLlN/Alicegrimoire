package org.aliceGrimoire.alicegrimoire.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.item.baton.DollBatonItem;

public class DollBatonLeftClickPacket implements CustomPacketPayload {
    public static final Type<DollBatonLeftClickPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "baton_left"));

    private final boolean shiftDown;

    public DollBatonLeftClickPacket(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public DollBatonLeftClickPacket(FriendlyByteBuf buf) {
        this.shiftDown = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(shiftDown);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DollBatonLeftClickPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DollBatonItem.handleLeftClick(player, packet.shiftDown);
            }
        });
    }
}