package org.aliceGrimoire.alicegrimoire.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.DollType;
import org.aliceGrimoire.alicegrimoire.item.DollItem;
import org.aliceGrimoire.alicegrimoire.menu.MagiweaverMenu;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;

public class MagiweaverScreen extends AbstractContainerScreen<MagiweaverMenu> {
    private static final ResourceLocation BG_LOCATION = ResourceLocation.fromNamespaceAndPath(Alicegrimoire.MODID, "textures/gui/container/magiweaver.png");

    public MagiweaverScreen(MagiweaverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        
        ItemStack stack = this.menu.getSlot(0).getItem();
        if (!stack.isEmpty() && stack.getItem() instanceof DollItem) {
            DollType type = stack.getOrDefault(ModDataComponents.DOLL_TYPE.get(), DollType.STANDARD);
            
            int color = 0x303030;
            int xOffset = 95;
            int yOffset = 20;
            
            graphics.drawString(this.font, Component.translatable("label.alicegrimoire.type", Component.translatable("doll_type.alicegrimoire." + type.getName())), xOffset, yOffset, color, false);
            graphics.drawString(this.font, Component.translatable("label.alicegrimoire.health", (int)type.getMaxHealth()), xOffset, yOffset + 12, color, false);
            graphics.drawString(this.font, Component.translatable("label.alicegrimoire.armor", type.getArmor()), xOffset, yOffset + 24, color, false);
            graphics.drawString(this.font, Component.translatable("label.alicegrimoire.attack", (int)type.getDamage()), xOffset, yOffset + 36, color, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(BG_LOCATION, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
