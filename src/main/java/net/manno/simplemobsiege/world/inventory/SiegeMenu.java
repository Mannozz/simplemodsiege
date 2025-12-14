package net.manno.simplemobsiege.world.inventory;

import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.manno.simplemobsiege.registry.ModBlocks;
import net.manno.simplemobsiege.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SiegeMenu extends AbstractContainerMenu {
    public final SiegeBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // Client Constructor
    public SiegeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), ContainerLevelAccess.NULL);
    }

    // Server Constructor
    public SiegeMenu(int containerId, Inventory inv, BlockEntity entity, ContainerLevelAccess levelAccess) {
        super(ModMenuTypes.SIEGE_MENU.get(), containerId);
        this.blockEntity = (SiegeBlockEntity) entity;
        this.levelAccess = levelAccess;

        checkContainerSize(inv, 18);

        // BE Inventory Slots
        // Wave: 0-8
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), i, 8 + i * 18, 18));
        }
        
        // Challenge: 9-17
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), i + 9, 8 + i * 18, 50));
        }

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 10)); // Adjusted Y
            }
        }

        // Player Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142 + 10)); // Adjusted Y
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 18) { // From Container to Player
                if (!this.moveItemStackTo(itemstack1, 18, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 18, false)) { // From Player to Container
                return ItemStack.EMPTY;
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
        return stillValid(levelAccess, player, ModBlocks.SIEGE_BLOCK.get());
    }
}

