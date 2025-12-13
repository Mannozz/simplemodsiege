package net.manno.simplemobsiege.network;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketStartSiege(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketStartSiege> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(SimpleMobSiege.MODID, "start_siege"));

    public static final StreamCodec<FriendlyByteBuf, PacketStartSiege> STREAM_CODEC = CustomPacketPayload.codec(
            PacketStartSiege::write,
            PacketStartSiege::new
    );

    public PacketStartSiege(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketStartSiege message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.level().getBlockEntity(message.pos()) instanceof SiegeBlockEntity be) {
                    be.startSiege(serverPlayer);
                }
            }
        });
    }
}

