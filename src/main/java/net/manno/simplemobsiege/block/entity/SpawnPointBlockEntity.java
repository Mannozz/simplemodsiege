package net.manno.simplemobsiege.block.entity;

import net.manno.simplemobsiege.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnPointBlockEntity extends BlockEntity {
    public SpawnPointBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SPAWN_POINT_BLOCK_ENTITY.get(), pos, blockState);
    }
}

