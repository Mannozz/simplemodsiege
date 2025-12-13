package net.manno.simplemobsiege.registry;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.item.ChallengeCardItem;
import net.manno.simplemobsiege.item.MobCardItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleMobSiege.MODID);

    public static final DeferredItem<Item> MOB_CARD = ITEMS.register("mob_card",
            () -> new MobCardItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> CHALLENGE_CARD = ITEMS.register("challenge_card",
            () -> new ChallengeCardItem(new Item.Properties().stacksTo(64)));
            
    public static final DeferredItem<Item> BLANK_CARD = ITEMS.register("blank_card",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

