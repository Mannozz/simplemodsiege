package net.manno.simplemobsiege.block;

import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.manno.simplemobsiege.block.entity.SpawnPointBlockEntity;
import net.manno.simplemobsiege.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

public class SpawnPointBlock extends BaseEntityBlock {
    public SpawnPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SpawnPointBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpawnPointBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockPos siegePos = net.manno.simplemobsiege.SimpleMobSiege.SELECTION_CACHE.get(player.getUUID());
            if (siegePos != null) {
                // Validate siege block exists
                if (level.getBlockEntity(siegePos) instanceof SiegeBlockEntity siegeBe) {
                    siegeBe.addSpawnPoint(pos);
                    player.displayClientMessage(Component.translatable("message.simplemobsiege.linked", siegePos.toShortString()), true);
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(Component.translatable("message.simplemobsiege.link_failed"), true);
                    net.manno.simplemobsiege.SimpleMobSiege.SELECTION_CACHE.remove(player.getUUID());
                }
            } else {
                player.displayClientMessage(Component.translatable("message.simplemobsiege.no_selection"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

