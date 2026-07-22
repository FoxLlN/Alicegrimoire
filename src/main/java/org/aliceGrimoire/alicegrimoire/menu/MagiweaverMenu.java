package org.aliceGrimoire.alicegrimoire.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.aliceGrimoire.alicegrimoire.entity.doll.DollType;
import org.aliceGrimoire.alicegrimoire.item.DollItem;
import org.aliceGrimoire.alicegrimoire.item.DollStringItem;
import org.aliceGrimoire.alicegrimoire.item.DollWandItem;
import org.aliceGrimoire.alicegrimoire.registry.ModMenuTypes;
import org.aliceGrimoire.alicegrimoire.registry.ModBlocks;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;

import java.util.ArrayList;
import java.util.List;

public class MagiweaverMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final SimpleContainer centerContainer = new SimpleContainer(1);
    private final SimpleContainer componentContainer = new SimpleContainer(8);
    private boolean isUpdating = false;

    public MagiweaverMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MagiweaverMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MagiweaverMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.MAGIWEAVER.get(), containerId);
        this.access = access;

        // Center slot (38, 38)
        this.addSlot(new Slot(centerContainer, 0, 38, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DollItem || stack.getItem() instanceof DollStringItem || stack.getItem() instanceof DollWandItem;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                loadComponents();
            }
        });

        // Component slots (Surrounding)
        int[][] coords = {{20, 20}, {38, 20}, {56, 20}, {20, 38}, {56, 38}, {20, 56}, {38, 56}, {56, 56}};
        for (int i = 0; i < 8; i++) {
            this.addSlot(new Slot(componentContainer, i, coords[i][0], coords[i][1]) {
                @Override
                public boolean isActive() {
                    return !centerContainer.getItem(0).isEmpty();
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (centerContainer.getItem(0).getItem() instanceof DollItem) {
                        if (isExclusive(stack.getItem())) {
                            // Check if any other exclusive item is already present
                            for (int j = 0; j < 8; j++) {
                                if (j != getContainerSlot() && isExclusive(componentContainer.getItem(j).getItem())) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    saveComponents();
                }
            });
        }

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    private boolean isExclusive(net.minecraft.world.item.Item item) {
        return item == Items.IRON_SWORD || item == Items.SHIELD || item == Items.CROSSBOW || item == Items.TRIDENT || item == Items.BOW;
    }

    private void loadComponents() {
        if (isUpdating) return;
        isUpdating = true;
        ItemStack centerStack = centerContainer.getItem(0);
        componentContainer.clearContent();
        if (!centerStack.isEmpty()) {
            List<ItemStack> comps = centerStack.getOrDefault(ModDataComponents.COMPONENTS.get(), List.of());
            for (int i = 0; i < Math.min(comps.size(), 8); i++) {
                componentContainer.setItem(i, comps.get(i).copy());
            }
        }
        isUpdating = false;
    }

    private void saveComponents() {
        if (isUpdating) return;
        isUpdating = true;
        ItemStack centerStack = centerContainer.getItem(0);
        if (!centerStack.isEmpty()) {
            List<ItemStack> comps = new ArrayList<>();
            DollType newType = DollType.STANDARD;
            for (int i = 0; i < 8; i++) {
                ItemStack s = componentContainer.getItem(i);
                if (!s.isEmpty()) {
                    comps.add(s.copy());
                    
                    // Update doll type based on exclusive item
                    if (centerStack.getItem() instanceof DollItem) {
                        if (s.getItem() == Items.IRON_SWORD) newType = DollType.SWORD;
                        else if (s.getItem() == Items.SHIELD) newType = DollType.GUARD;
                        else if (s.getItem() == Items.CROSSBOW) newType = DollType.PILLAGER;
                        else if (s.getItem() == Items.TRIDENT) newType = DollType.LANCER;
                        else if (s.getItem() == Items.BOW) newType = DollType.SHARPSHOOTER;
                    }
                }
            }
            centerStack.set(ModDataComponents.COMPONENTS.get(), comps);
            if (centerStack.getItem() instanceof DollItem) {
                centerStack.set(ModDataComponents.DOLL_TYPE.get(), newType);
            }
        }
        isUpdating = false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 9) { // From custom slots
                if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory
                if (itemstack1.getItem() instanceof DollItem || itemstack1.getItem() instanceof DollStringItem || itemstack1.getItem() instanceof DollWandItem) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(itemstack1, 1, 9, false)) {
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
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, centerContainer);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.MAGIWEAVER.get());
    }
}
