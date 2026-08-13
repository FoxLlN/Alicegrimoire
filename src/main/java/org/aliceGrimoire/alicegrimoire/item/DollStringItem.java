package org.aliceGrimoire.alicegrimoire.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;
import org.aliceGrimoire.alicegrimoire.item.string.IStringProperties;

public class DollStringItem extends ArmorItem implements IStringProperties {
    
    // 默认最大拴住数量（策划案：8个）
    private static final int DEFAULT_MAX_TETHERED = 8;
    
    public DollStringItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.LEGGINGS, properties);
    }
    
    @Override
    public int getMaxTethered() {
        return DEFAULT_MAX_TETHERED;
    }
    
    @Override
    public String getStringName() {
        return "doll_string";
    }
    
    @Override
    public boolean allowUpgrade() {
        return true; // 允许通过改装增加上限
    }
}