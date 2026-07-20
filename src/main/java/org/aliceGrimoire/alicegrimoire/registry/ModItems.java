package org.aliceGrimoire.alicegrimoire.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import org.aliceGrimoire.alicegrimoire.Alicegrimoire;
import org.aliceGrimoire.alicegrimoire.item.*;
import org.aliceGrimoire.alicegrimoire.registry.ModBlocks;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alicegrimoire.MODID);

    public static final DeferredItem<Item> MAGIWEAVER = ITEMS.register("magiweaver", 
        () -> new MagiweaverItem(ModBlocks.MAGIWEAVER.get(), new Item.Properties()));
    
    public static final DeferredItem<Item> DOLL_WAND = ITEMS.register("doll_wand", 
        () -> new DollWandItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4f))));
        
    public static final DeferredItem<Item> DOLL_NET = ITEMS.register("doll_net", 
        () -> new DollNetItem(new Item.Properties().stacksTo(1)));
        
    public static final DeferredItem<Item> DOLL_STRING = ITEMS.register("doll_string", 
        () -> new DollStringItem(ArmorMaterials.LEATHER, new Item.Properties().stacksTo(1)));
        
    public static final DeferredItem<Item> DOLL_BASKET = ITEMS.register("doll_basket", 
        () -> new DollBasketItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DOLL_WHISTLE = ITEMS.register("doll_whistle",
        () -> new DollWhistleItem(new Item.Properties().stacksTo(1)));
        
    public static final DeferredItem<Item> DOLL = ITEMS.register("doll", 
        () -> new DollItem(ModBlocks.DOLL.get(), new Item.Properties().stacksTo(1)));
}
