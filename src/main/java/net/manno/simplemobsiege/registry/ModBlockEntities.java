package net.manno.simplemobsiege.registry;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.manno.simplemobsiege.block.entity.SpawnPointBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimpleMobSiege.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiegeBlockEntity>> SIEGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("siege_block",
                    () -> BlockEntityType.Builder.of(SiegeBlockEntity::new, ModBlocks.SIEGE_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpawnPointBlockEntity>> SPAWN_POINT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("spawn_point",
                    () -> BlockEntityType.Builder.of(SpawnPointBlockEntity::new, ModBlocks.SPAWN_POINT_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

