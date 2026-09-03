package org.aliceGrimoire.alicegrimoire.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollDataTemplate;
import org.aliceGrimoire.alicegrimoire.item.DollItem;
import org.aliceGrimoire.alicegrimoire.menu.MagiweaverMenu;
import org.aliceGrimoire.alicegrimoire.modifier.ModifierManager;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModItems;

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
        
        ItemStack centerStack = this.menu.getSlot(0).getItem();
        if (!centerStack.isEmpty() && centerStack.getItem() instanceof DollItem) {
            // 获取当前职业（来自物品的数据组件）
            DollJobType currentType = centerStack.getOrDefault(ModDataComponents.DOLL_TYPE.get(), DollJobType.STANDARD);
            
            DollDataTemplate template = DollDataTemplate.getTemplateForJob(currentType);
            
            // 收集周围8格组件
            List<ItemStack> currentComponents = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                ItemStack s = this.menu.getSlot(i).getItem();
                if (!s.isEmpty()) currentComponents.add(s);
            }

            // 计算修正
            double baseHealth = template.maxHealth();
            double finalHealth = baseHealth;
            double baseDamage = template.damage();
            double finalDamage = baseDamage;
            int baseArmor = template.armor();
            int finalArmor = baseArmor;

            for (ItemStack s : currentComponents) {
                finalHealth += ModifierManager.getModifiedValue(s, "max_health");
                finalDamage += ModifierManager.getModifiedValue(s, "attack_damage");
                finalArmor += (int) ModifierManager.getModifiedValue(s, "armor");
            }

            // 钳制（与 DollData.recalculate 保持一致）
            finalHealth = Math.max(1, Math.min(200.0, finalHealth));
            finalDamage = Math.max(0, Math.min(50.0, finalDamage));
            finalArmor = (int) Math.max(0, Math.min(30, finalArmor));

            // 检测周围是否有职业信物（用于预览职业变化）
            DollJobType detectedJob = detectJobFromComponents(currentComponents);
            
            int color = 0x303030;
            int xOffset = 95, yOffset = 20;
            
            // 显示当前职业
            graphics.drawString(this.font, 
                Component.translatable("label.alicegrimoire.type", 
                    Component.translatable("doll_type.alicegrimoire." + currentType.name().toLowerCase())), 
                xOffset, yOffset, color, false);

            // 显示属性变化
            String healthText = String.format("❤ %.1f → %.1f", baseHealth, finalHealth);
            graphics.drawString(this.font, Component.literal(healthText), xOffset, yOffset + 12, color, false);

            String dmgText = String.format("⚔ %.1f → %.1f", baseDamage, finalDamage);
            graphics.drawString(this.font, Component.literal(dmgText), xOffset, yOffset + 24, color, false);

            String armorText = String.format("🛡 %d → %d", baseArmor, finalArmor);
            graphics.drawString(this.font, Component.literal(armorText), xOffset, yOffset + 36, color, false);

            // 如果有信物且与当前职业不同，显示"将变为 X"预览
            if (detectedJob != null && detectedJob != currentType) {
                graphics.drawString(this.font, 
                    Component.translatable("label.alicegrimoire.will_become", 
                        Component.translatable("doll_type.alicegrimoire." + detectedJob.name().toLowerCase())),
                    xOffset, yOffset + 52, 0xFF8800, false);
            }
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

    /**
     * 从组件列表中检测职业信物（与 Menu 中的逻辑保持一致）
     */
    private DollJobType detectJobFromComponents(List<ItemStack> components) {
        DollJobType detected = null;
        for (ItemStack stack : components) {
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            DollJobType job = getJobFromItem(item);
            if (job != null) {
                if (detected != null && detected != job) {
                    // 多个不同信物冲突 → 不显示预览
                    return null;
                }
                detected = job;
            }
        }
        return detected;
    }

    /**
     * 将物品映射到职业（与 Menu 中的逻辑保持一致）
     */
    private DollJobType getJobFromItem(Item item) {
        if (item == ModItems.GUARD_CREST.get()) return DollJobType.GUARD;
        if (item == ModItems.DEFENDER_CREST.get()) return DollJobType.DEFENDER;
        if (item == ModItems.SHARPSHOOTER_CREST.get()) return DollJobType.SHARPSHOOTER;
        if (item == ModItems.VANGUARD_CREST.get()) return DollJobType.VANGUARD;
        return null;
    }
}