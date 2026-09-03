package org.aliceGrimoire.alicegrimoire.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.aliceGrimoire.alicegrimoire.entity.doll.data.DollJobType; 
import org.aliceGrimoire.alicegrimoire.item.DollItem;
import org.aliceGrimoire.alicegrimoire.item.DollStringItem;
import org.aliceGrimoire.alicegrimoire.item.baton.DollBatonItem;
import org.aliceGrimoire.alicegrimoire.registry.ModMenuTypes;
import org.aliceGrimoire.alicegrimoire.registry.ModBlocks;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

public class MagiweaverMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final SimpleContainer centerContainer = new SimpleContainer(1);
    private final SimpleContainer componentContainer = new SimpleContainer(8);
    private boolean isUpdating = false;

    // 保存中心物品的原始职业（用于在保存时保留，防止被覆盖为STANDARD）
    private DollJobType originalJobType = DollJobType.STANDARD;

    public MagiweaverMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MagiweaverMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MagiweaverMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.MAGIWEAVER.get(), containerId);
        this.access = access;

        // Center slot
        this.addSlot(new Slot(centerContainer, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DollItem || stack.getItem() instanceof DollStringItem || stack.getItem() instanceof DollBatonItem;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                loadComponents();
            }
        });

        // Component slots (Surrounding)
        int[][] coords = {
            {26, 17},   // 左上
            {44, 17},   // 上中
            {62, 17},   // 右上
            {26, 35},   // 左中
            {62, 35},   // 右中
            {26, 53},   // 左下
            {44, 53},   // 下中
            {62, 53}    // 右下
        };
        for (int i = 0; i < 8; i++) {
            this.addSlot(new Slot(componentContainer, i, coords[i][0], coords[i][1]) {
                @Override
                public boolean isActive() {
                    return !centerContainer.getItem(0).isEmpty();
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    // 核心修复：中心没有人偶时，不能放入任何组件
                    ItemStack center = centerContainer.getItem(0);
                    if (center.isEmpty()) {
                        return false; // ← 拒绝放入，物品不会消失
                    }
                    
                    // 只有中心是人偶时，才允许放入
                    if (center.getItem() instanceof DollItem) {
                        if (isExclusive(stack.getItem())) {
                            for (int j = 0; j < 8; j++) {
                                if (j != getContainerSlot() && isExclusive(componentContainer.getItem(j).getItem())) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                    return false;
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

    /**
     * 判断物品是否为独占物品（即职业信物）
     * 一个人偶只能装一个职业信物，多个信物会冲突
     * 
     * 独占物品列表：
     * - 近卫信物 (GUARD_CREST)
     * - 守御信物 (DEFENDER_CREST)
     * - 射手信物 (SHARPSHOOTER_CREST)
     * - 游击信物 (VANGUARD_CREST)
     */
    private boolean isExclusive(Item item) {
        return item == ModItems.GUARD_CREST.get() ||
               item == ModItems.DEFENDER_CREST.get() ||
               item == ModItems.SHARPSHOOTER_CREST.get() ||
               item == ModItems.VANGUARD_CREST.get();
    }

    /**
     * 加载中心物品的组件列表到周围槽位
     * 同时保存原始职业，防止后续保存时被覆盖
     */
    private void loadComponents() {
        if (isUpdating) return;
        isUpdating = true;
        ItemStack centerStack = centerContainer.getItem(0);
        componentContainer.clearContent();
        
        if (!centerStack.isEmpty()) {
            // 保存原始职业（如果物品没有职业，默认为STANDARD）
            this.originalJobType = centerStack.getOrDefault(ModDataComponents.DOLL_TYPE.get(), DollJobType.STANDARD);
            
            // 加载组件列表
            List<ItemStack> comps = centerStack.getOrDefault(ModDataComponents.COMPONENTS.get(), List.of());
            for (int i = 0; i < Math.min(comps.size(), 8); i++) {
                componentContainer.setItem(i, comps.get(i).copy());
            }
        } else {
            this.originalJobType = DollJobType.STANDARD;
        }
        isUpdating = false;
    }

    /**
     * 保存周围槽位的组件到中心物品
     * 职业由职业信物决定，如果没有信物则保留原始职业
     */
    private void saveComponents() {
        if (isUpdating) return;
        isUpdating = true;
        ItemStack centerStack = centerContainer.getItem(0);
        if (!centerStack.isEmpty()) {
            List<ItemStack> comps = new ArrayList<>();
            
            // 1. 先收集所有组件
            for (int i = 0; i < 8; i++) {
                ItemStack s = componentContainer.getItem(i);
                if (!s.isEmpty()) {
                    comps.add(s.copy());
                }
            }
            
            // 2. 检测职业信物（只检测一个，且仅对 DollItem 有效）
            if (centerStack.getItem() instanceof DollItem) {
                DollJobType newJob = detectJobFromComponents(comps);
                if (newJob != null) {
                    // 检测到信物，使用信物指定的职业
                    centerStack.set(ModDataComponents.DOLL_TYPE.get(), newJob);
                } else {
                    // 没有信物，恢复原始职业
                    centerStack.set(ModDataComponents.DOLL_TYPE.get(), DollJobType.STANDARD);
                }
            }
            
            // 3. 保存组件列表
            centerStack.set(ModDataComponents.COMPONENTS.get(), comps);
        }
        isUpdating = false;
    }

    /**
     * 从组件列表中检测职业信物
     * @param components 组件列表
     * @return 检测到的职业，如果没有或冲突则返回 null
     */
    private DollJobType detectJobFromComponents(List<ItemStack> components) {
        DollJobType detected = null;
        for (ItemStack stack : components) {
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            DollJobType job = getJobFromItem(item);
            if (job != null) {
                if (detected != null && detected != job) {
                    // 冲突：多个不同信物 → 忽略所有，返回 null 表示不改变职业
                    return null;
                }
                detected = job;
            }
        }
        return detected; // 可能是 null
    }

    /**
     * 将物品映射到职业
     */
    private DollJobType getJobFromItem(Item item) {
        if (item == ModItems.GUARD_CREST.get()) return DollJobType.GUARD;
        if (item == ModItems.DEFENDER_CREST.get()) return DollJobType.DEFENDER;
        if (item == ModItems.SHARPSHOOTER_CREST.get()) return DollJobType.SHARPSHOOTER;
        if (item == ModItems.VANGUARD_CREST.get()) return DollJobType.VANGUARD;
        return null;
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
                if (itemstack1.getItem() instanceof DollItem || itemstack1.getItem() instanceof DollStringItem || itemstack1.getItem() instanceof DollBatonItem) {
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
        this.clearContainer(player, componentContainer);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.MAGIWEAVER.get());
    }
}