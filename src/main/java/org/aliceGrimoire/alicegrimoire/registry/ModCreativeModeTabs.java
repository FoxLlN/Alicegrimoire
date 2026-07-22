package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.entity.doll.DollType;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alicegrimoire.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALICE_GRIMOIRE_TAB = CREATIVE_MODE_TABS.register("alicegrimoire_tab", 
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alicegrimoire"))
            .icon(() -> new ItemStack(ModItems.DOLL.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.MAGIWEAVER.get());
                output.accept(ModItems.DOLL_WAND.get());
                output.accept(ModItems.DOLL_NET.get());
                output.accept(ModItems.DOLL_STRING.get());
                output.accept(ModItems.DOLL_BASKET.get());
                output.accept(ModItems.DOLL_WHISTLE.get());
                
                for (DollType type : DollType.values()) {
                    ItemStack stack = new ItemStack(ModItems.DOLL.get());
                    stack.set(ModDataComponents.DOLL_TYPE.get(), type);
                    output.accept(stack);
                }
            })
            .build());
}
