package org.aliceGrimoire.alicegrimoire.entity.doll.util;

import net.minecraft.world.entity.ai.attributes.Attributes;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;

/**
 * 人偶属性刷新辅助
 * 现在从 DollData 读取属性，而非旧的 DollType 枚举
 */
public class DollAttributeHelper {

    /**
     * 根据 DollData 刷新生命、伤害、护甲属性
     */
    public static void refreshAttributes(DollEntity doll) {
        if (doll == null) return;
        
        // 从 DollDataManager 获取数据
        var data = doll.getDataManager();
        
        doll.getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getData().getMaxHealth());
        doll.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(data.getData().getDamage());
        doll.getAttribute(Attributes.ARMOR).setBaseValue(data.getData().getArmor());
        doll.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(data.getData().getArmorToughness());
        doll.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(data.getData().getKnockbackResistance());
        
        // 如果当前生命值超过新上限，调整
        if (doll.getHealth() > doll.getMaxHealth()) {
            doll.setHealth(doll.getMaxHealth());
        }
    }

    /**
     * 获取人偶的拴绳范围（从 DollData 读取）
     */
    public static double getTetherRange(DollEntity doll) {
        return doll.getDollData().getTetherRange();
    }
}