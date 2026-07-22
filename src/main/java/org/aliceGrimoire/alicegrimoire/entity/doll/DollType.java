package org.aliceGrimoire.alicegrimoire.entity.doll;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.ICombatStrategy;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.strategy.*;

/**
 * 人偶类型枚举，包含基础属性（生命、伤害、护甲、拴绳范围）
 * 以及关联的战斗策略工厂
 */
public enum DollType implements StringRepresentable {
    STANDARD("standard", 20.0, 2.0, 0, 8.0, MeleeStrategy::new),
    SWORD("sword", 20.0, 6.0, 0, 8.0, MeleeStrategy::new),
    GUARD("guard", 32.0, 4.0, 4, 4.0, GuardStrategy::new),
    PILLAGER("pillager", 20.0, 2.0, 0, 8.0, RangedKiteStrategy::new),
    LANCER("lancer", 32.0, 12.0, 2, 8.0, LancerChargeStrategy::new),
    SHARPSHOOTER("sharpshooter", 16.0, 2.0, 0, 8.0, SyncTurretStrategy::new);

    public static final Codec<DollType> CODEC = StringRepresentable.fromEnum(DollType::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, DollType> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private final String name;
    private final double maxHealth;
    private final double damage;
    private final int armor;
    private final double tetherRange;
    private final ICombatStrategySupplier strategySupplier;

    @FunctionalInterface
    public interface ICombatStrategySupplier {
        ICombatStrategy get();
    }

    DollType(String name, double maxHealth, double damage, int armor, double tetherRange,
             ICombatStrategySupplier strategySupplier) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.armor = armor;
        this.tetherRange = tetherRange;
        this.strategySupplier = strategySupplier;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getName() { return name; }
    public double getMaxHealth() { return maxHealth; }
    public double getDamage() { return damage; }
    public int getArmor() { return armor; }
    public double getTetherRange() { return tetherRange; }

    /**
     * 创建对应的战斗策略实例
     */
    public ICombatStrategy createStrategy() {
        return strategySupplier.get();
    }
}