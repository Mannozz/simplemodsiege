package net.manno.simplemobsiege.registry;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.block.SiegeBlock;
import net.manno.simplemobsiege.block.SpawnPointBlock;
import net.manno.simplemobsiege.item.SpawnPointBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimpleMobSiege.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleMobSiege.MODID);

    public static final DeferredBlock<Block> SIEGE_BLOCK = BLOCKS.register("siege_block",
            () -> new SiegeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f).requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> SIEGE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("siege_block", SIEGE_BLOCK);

    public static final DeferredBlock<Block> SPAWN_POINT_BLOCK = BLOCKS.register("spawn_point",
            () -> new SpawnPointBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0f).noOcclusion()));


// ... existing imports ...

    public static final DeferredItem<BlockItem> SPAWN_POINT_BLOCK_ITEM = ITEMS.register("spawn_point",
            () -> new SpawnPointBlockItem(SPAWN_POINT_BLOCK.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}

