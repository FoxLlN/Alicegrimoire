package org.aliceGrimoire.alicegrimoire.entity;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum DollType implements StringRepresentable {
    STANDARD("standard", 20.0, 2.0, 0, 8.0),
    SWORD("sword", 20.0, 6.0, 0, 8.0),
    GUARD("guard", 32.0, 4.0, 4, 4.0),
    PILLAGER("pillager", 20.0, 2.0, 0, 8.0),
    LANCER("lancer", 32.0, 12.0, 2, 8.0),
    SHARPSHOOTER("sharpshooter", 16.0, 2.0, 0, 8.0);

    public static final Codec<DollType> CODEC = StringRepresentable.fromEnum(DollType::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, DollType> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private final String name;
    private final double maxHealth;
    private final double damage;
    private final int armor;
    private final double tetherRange;

    DollType(String name, double maxHealth, double damage, int armor, double tetherRange) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.armor = armor;
        this.tetherRange = tetherRange;
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
}
