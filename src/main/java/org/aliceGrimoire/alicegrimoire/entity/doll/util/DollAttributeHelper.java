package org.aliceGrimoire.alicegrimoire.entity.doll.util;

import net.minecraft.world.entity.ai.attributes.Attributes;
import org.aliceGrimoire.alicegrimoire.entity.DollEntity;
import org.aliceGrimoire.alicegrimoire.entity.doll.DollType;

/**
 * 人偶属性刷新辅助，在类型变更或初始化时同步基础属性。
 */
public class DollAttributeHelper {

    /**
     * 根据人偶类型刷新生命、伤害、护甲属性
     */
    public static void refreshAttributes(DollEntity doll) {
        if (doll == null) return;
        DollType type = doll.getDollType();
        doll.getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.getMaxHealth());
        doll.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(type.getDamage());
        doll.getAttribute(Attributes.ARMOR).setBaseValue(type.getArmor());
        // 如果当前生命值超过新上限，调整
        if (doll.getHealth() > doll.getMaxHealth()) {
            doll.setHealth(doll.getMaxHealth());
        }
    }

    /**
     * 获取人偶的拴绳范围（不同职业可能不同）
     */
    public static double getTetherRange(DollEntity doll) {
        return doll.getDollType().getTetherRange();
    }
}