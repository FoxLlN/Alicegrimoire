package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.core.Holder;

public class DollStringItem extends ArmorItem {
    public DollStringItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.LEGGINGS, properties);
    }
}
