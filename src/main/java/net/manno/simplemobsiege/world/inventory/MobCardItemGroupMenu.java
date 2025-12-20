package net.manno.simplemobsiege.world.inventory;

import net.manno.simplemobsiege.registry.ModItems;
import net.manno.simplemobsiege.registry.ModMenuTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MobCardItemGroupMenu extends AbstractContainerMenu {
    private final ItemStack stack;
    private final ItemStackHandler itemHandler;
    private final Player player;
    private boolean active = true;

    // Client Constructor
    public MobCardItemGroupMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBoolean() ? inv.player.getMainHandItem() : inv.player.getOffhandItem());
    }

    // Server Constructor
    public MobCardItemGroupMenu(int containerId, Inventory inv, ItemStack stack) {
        super(ModMenuTypes.MOB_CARD_GROUP_MENU.get(), containerId);
        this.stack = stack;
        this.player = inv.player;
        this.itemHandler = new ItemStackHandler(27) {
            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.getItem() == ModItems.MOB_CARD.get();
            }
        };

        loadFromStack();

        // Container Slots (3x9)
        int containerYOffset = 18;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new SlotItemHandler(itemHandler, j + i * 9, 8 + j * 18, containerYOffset + i * 18));
            }
        }

        // Player Inventory
        int playerInvY = containerYOffset + 3 * 18 + 14;
        for (int l = 0; l < 3; ++l) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inv, k + l * 9 + 9, 8 + k * 18, playerInvY + l * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        // Prevent picking up the opened bag
                        return getItem() != MobCardItemGroupMenu.this.stack;
                    }
                });
            }
        }

        // Hotbar
        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(inv, i1, 8 + i1 * 18, playerInvY + 58) {
                @Override
                public boolean mayPickup(Player player) {
                    return getItem() != MobCardItemGroupMenu.this.stack;
                }
            });
        }
    }

    private void saveToStack() {
        if (!active) return;
        // HolderLookup.Provider from player's level
        HolderLookup.Provider provider = player.level().registryAccess();
        CompoundTag nbt = itemHandler.serializeNBT(provider);
        
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.put("Inventory", nbt);
        });
    }

    private void loadFromStack() {
        active = false;
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(player.level().registryAccess(), tag.getCompound("Inventory"));
        }
        active = true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            
            // Prevent moving the bag itself
            if (itemstack1 == stack) return ItemStack.EMPTY;
            
            itemstack = itemstack1.copy();
            if (index < 27) { // From Container
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Inventory
                if (itemstack1.getItem() == ModItems.MOB_CARD.get()) { 
                     if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                         return ItemStack.EMPTY;
                     }
                } else if (index < 27 + 27) { // Player Inv -> Hotbar
                    if (!this.moveItemStackTo(itemstack1, 27 + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else { // Hotbar -> Player Inv
                    if (!this.moveItemStackTo(itemstack1, 27, 27 + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !stack.isEmpty() && (player.getMainHandItem() == stack || player.getOffhandItem() == stack);
    }
}

