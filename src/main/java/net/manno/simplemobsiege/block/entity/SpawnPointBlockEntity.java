package net.manno.simplemobsiege.block.entity;

import net.manno.simplemobsiege.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnPointBlockEntity extends BlockEntity {
    private BlockPos siegeBlockPos;

    public SpawnPointBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SPAWN_POINT_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setSiegeBlockPos(BlockPos pos) {
        this.siegeBlockPos = pos;
        setChanged();
    }

    public BlockPos getSiegeBlockPos() {
        return siegeBlockPos;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (siegeBlockPos != null) {
            tag.put("SiegeBlockPos", NbtUtils.writeBlockPos(siegeBlockPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SiegeBlockPos")) {
            siegeBlockPos = NbtUtils.readBlockPos(tag, "SiegeBlockPos").orElse(null);
        }
    }
}
