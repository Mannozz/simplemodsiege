package net.manno.simplemobsiege.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.network.PacketStartSiege;
import net.manno.simplemobsiege.world.inventory.SiegeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class SiegeScreen extends AbstractContainerScreen<SiegeMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(SimpleMobSiege.MODID, "textures/gui/siege_gui.png");

    public SiegeScreen(SiegeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176; // Larger height for extra rows
    }

    @Override
    protected void init() {
        super.init();
        // Add Start Button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.simplemobsiege.start"), (button) -> {
            PacketDistributor.sendToServer(new PacketStartSiege(this.menu.blockEntity.getBlockPos()));
            this.onClose();
        }).bounds(this.leftPos + 120, this.topPos + 60, 40, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // Fallback since we don't have the texture: draw a grey rectangle
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
        // Draw slots locations (debug style)
        // Wave
        guiGraphics.drawString(this.font, "Wave", this.leftPos + 8, this.topPos + 8, 0x404040, false);
        // Challenge
        guiGraphics.drawString(this.font, "Challenge", this.leftPos + 8, this.topPos + 40, 0x404040, false);
        
        // Render Spawn Points
        int count = this.menu.blockEntity.getSpawnPoints().size();
        guiGraphics.drawString(this.font, Component.translatable("gui.simplemobsiege.spawn_points", count), this.leftPos + 8, this.topPos + 75, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}

