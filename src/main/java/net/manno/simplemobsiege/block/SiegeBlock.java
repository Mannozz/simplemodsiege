package net.manno.simplemobsiege.block;

import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.manno.simplemobsiege.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

public class SiegeBlock extends BaseEntityBlock {
    public SiegeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SiegeBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SiegeBlockEntity(pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean hasSignal = level.hasNeighborSignal(pos);
            if (hasSignal) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SiegeBlockEntity siegeBe) {
                    siegeBe.startSiege((Player)null); // Pass null player for redstone activation
                }
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                // Link mode
                net.manno.simplemobsiege.SimpleMobSiege.SELECTION_CACHE.put(player.getUUID(), pos);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.simplemobsiege.siege_selected", pos.toShortString()), true);
                return InteractionResult.SUCCESS;
            }
            
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SiegeBlockEntity) {
                player.openMenu((MenuProvider) be, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SIEGE_BLOCK_ENTITY.get(), SiegeBlockEntity::tick);
    }
}

