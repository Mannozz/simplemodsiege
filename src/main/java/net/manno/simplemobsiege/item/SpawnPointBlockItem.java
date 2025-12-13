package net.manno.simplemobsiege.item;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.block.SiegeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnPointBlockItem extends BlockItem {
    public SpawnPointBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            BlockPos pos = context.getClickedPos();
            BlockState state = context.getLevel().getBlockState(pos);
            if (state.getBlock() instanceof SiegeBlock) {
                // Link mode / Selection mode
                SimpleMobSiege.SELECTION_CACHE.put(context.getPlayer().getUUID(), pos);
                context.getPlayer().displayClientMessage(Component.translatable("message.simplemobsiege.siege_selected", pos.toShortString()), true);
                return InteractionResult.SUCCESS;
            }
        }
        // If not targeting SiegeBlock, behave normally (place block)
        return super.useOn(context);
    }
}

