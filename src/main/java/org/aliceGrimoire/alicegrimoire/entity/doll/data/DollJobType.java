package org.aliceGrimoire.alicegrimoire.entity.doll.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * 人偶职业类型
 * 用于决定战斗AI和基础属性模板
 */
public enum DollJobType implements StringRepresentable {
    STANDARD,      // 标准人偶 - 基础近战
    GUARD,         // 近卫人偶 - 四步循环攻击
    DEFENDER,      // 守御人偶 - 举盾保护玩家
    SHARPSHOOTER,  // 射手人偶 - 远程走位射击
    VANGUARD;      // 游击人偶 - 一击脱离

    public static final Codec<DollJobType> CODEC = StringRepresentable.fromEnum(DollJobType::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, DollJobType> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    @Override
    public String getSerializedName() {
        return name();
    }
}