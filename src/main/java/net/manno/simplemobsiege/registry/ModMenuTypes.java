package net.manno.simplemobsiege.registry;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.world.inventory.SiegeMenu;
import net.manno.simplemobsiege.world.inventory.MobCardItemGroupMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SimpleMobSiege.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SiegeMenu>> SIEGE_MENU =
            MENUS.register("siege_menu", () -> IMenuTypeExtension.create(SiegeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MobCardItemGroupMenu>> MOB_CARD_GROUP_MENU =
            MENUS.register("mob_card_group_menu", () -> IMenuTypeExtension.create(MobCardItemGroupMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

