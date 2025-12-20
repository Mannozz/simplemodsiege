package net.manno.simplemobsiege.registry;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleMobSiege.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIEGE_TAB = CREATIVE_MODE_TABS.register("siege_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simplemobsiege"))
                    .icon(() -> ModBlocks.SIEGE_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.SIEGE_BLOCK_ITEM.get());
                        output.accept(ModBlocks.SPAWN_POINT_BLOCK_ITEM.get());
                        output.accept(ModItems.BLANK_CARD.get());
                        output.accept(ModItems.MOB_CARD.get());
                        output.accept(ModItems.MOB_CARD_GROUP.get());
                        output.accept(ModItems.CHALLENGE_CARD.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

