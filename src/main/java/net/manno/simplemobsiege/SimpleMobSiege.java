package net.manno.simplemobsiege;

import com.mojang.logging.LogUtils;
import net.manno.simplemobsiege.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SimpleMobSiege.MODID)
public class SimpleMobSiege {
    public static final String MODID = "simplemobsiege";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Temporary cache for player selection (Siege Block Linking)
    public static final java.util.Map<java.util.UUID, net.minecraft.core.BlockPos> SELECTION_CACHE = new java.util.HashMap<>();

    public SimpleMobSiege(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        net.manno.simplemobsiege.recipe.ModRecipes.register(modEventBus);
        // NetworkHandler auto-registers via @EventBusSubscriber

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = SimpleMobSiege.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
    static class ClientModEvents {
        @net.neoforged.bus.api.SubscribeEvent
        static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        }
        
        @net.neoforged.bus.api.SubscribeEvent
        public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
             event.register(net.manno.simplemobsiege.registry.ModMenuTypes.SIEGE_MENU.get(), net.manno.simplemobsiege.client.gui.SiegeScreen::new);
        }
    }
}
