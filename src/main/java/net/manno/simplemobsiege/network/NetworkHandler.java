package net.manno.simplemobsiege.network;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SimpleMobSiege.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                PacketStartSiege.TYPE,
                PacketStartSiege.STREAM_CODEC,
                PacketStartSiege::handle
        );
    }
}

