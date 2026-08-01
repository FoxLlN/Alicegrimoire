package org.aliceGrimoire.alicegrimoire.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;

@EventBusSubscriber(modid = Alicegrimoire.MODID, bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Alicegrimoire.MODID);

        // 左键包
        StreamCodec<RegistryFriendlyByteBuf, DollBatonLeftClickPacket> leftCodec =
            StreamCodec.ofMember(
                DollBatonLeftClickPacket::write,
                DollBatonLeftClickPacket::new
            );
        registrar.playToServer(DollBatonLeftClickPacket.TYPE, leftCodec, DollBatonLeftClickPacket::handle);

        // 右键包
        StreamCodec<RegistryFriendlyByteBuf, DollBatonRightClickPacket> rightCodec =
            StreamCodec.ofMember(
                DollBatonRightClickPacket::write,
                DollBatonRightClickPacket::new
            );
        registrar.playToServer(DollBatonRightClickPacket.TYPE, rightCodec, DollBatonRightClickPacket::handle);
    }
}