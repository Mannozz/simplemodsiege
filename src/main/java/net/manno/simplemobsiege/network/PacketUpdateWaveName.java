package net.manno.simplemobsiege.network;

import io.netty.buffer.ByteBuf;
import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketUpdateWaveName(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<PacketUpdateWaveName> TYPE = new Type<>(new ResourceLocation(SimpleMobSiege.MODID, "update_wave_name"));
    
    public static final StreamCodec<ByteBuf, PacketUpdateWaveName> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketUpdateWaveName::pos,
            ByteBufCodecs.STRING_UTF8, PacketUpdateWaveName::name,
            PacketUpdateWaveName::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketUpdateWaveName message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.level().getBlockEntity(message.pos()) instanceof SiegeBlockEntity be) {
                    be.setWaveName(message.name());
                }
            }
        });
    }
}

