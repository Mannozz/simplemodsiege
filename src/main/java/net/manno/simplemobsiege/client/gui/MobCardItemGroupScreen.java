package net.manno.simplemobsiege.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.manno.simplemobsiege.world.inventory.MobCardItemGroupMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MobCardItemGroupScreen extends AbstractContainerScreen<MobCardItemGroupMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    public MobCardItemGroupScreen(MobCardItemGroupMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + 3 * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        
        // Draw top part (container slots) - 3 rows
        // 3 rows height is 3 * 18 + 17 = 71
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, 3 * 18 + 17);
        // Draw bottom part (player inventory)
        guiGraphics.blit(TEXTURE, i, j + 3 * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}

