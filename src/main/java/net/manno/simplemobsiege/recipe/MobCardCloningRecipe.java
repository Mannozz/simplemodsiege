package net.manno.simplemobsiege.recipe;

import net.manno.simplemobsiege.registry.ModDataComponents;
import net.manno.simplemobsiege.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public class MobCardCloningRecipe extends CustomRecipe {
    public MobCardCloningRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack mobCard = ItemStack.EMPTY;
        ItemStack blankCard = ItemStack.EMPTY;
        int count = 0;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                count++;
                if (stack.getItem() == ModItems.MOB_CARD.get() && stack.has(ModDataComponents.MOB_TYPE)) {
                    mobCard = stack;
                } else if (stack.getItem() == ModItems.BLANK_CARD.get()) {
                    blankCard = stack;
                }
            }
        }

        return !mobCard.isEmpty() && !blankCard.isEmpty() && count == 2;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, HolderLookup.Provider registries) {
        ItemStack mobCard = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() == ModItems.MOB_CARD.get() && stack.has(ModDataComponents.MOB_TYPE)) {
                mobCard = stack;
                break;
            }
        }

        if (mobCard.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = mobCard.copy();
        result.setCount(2);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MOB_CARD_CLONING.get();
    }
}

