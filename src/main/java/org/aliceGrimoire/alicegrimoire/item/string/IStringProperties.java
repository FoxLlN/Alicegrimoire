package org.aliceGrimoire.alicegrimoire.item.string;

/**
 * 丝线属性接口
 * 为后续不同等级丝线预留扩展
 */
public interface IStringProperties {
    
    /**
     * 最大拴住数量
     */
    int getMaxTethered();
    
    /**
     * 丝线名称（用于显示）
     */
    default String getStringName() {
        return "doll_string";
    }
    
    /**
     * 是否允许通过改装增加上限
     */
    default boolean allowUpgrade() {
        return true;
    }
}