package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.menu.MagiweaverMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Alicegrimoire.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MagiweaverMenu>> MAGIWEAVER = MENUS.register("magiweaver", 
        () -> IMenuTypeExtension.create(MagiweaverMenu::new));
}
