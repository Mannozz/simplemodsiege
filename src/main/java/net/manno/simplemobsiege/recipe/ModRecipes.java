package net.manno.simplemobsiege.recipe;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SimpleMobSiege.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MobCardCloningRecipe>> MOB_CARD_CLONING =
            RECIPE_SERIALIZERS.register("mob_card_cloning",
                    () -> new SimpleCraftingRecipeSerializer<>(MobCardCloningRecipe::new));

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}

