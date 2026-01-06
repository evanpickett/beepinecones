package com.mstn.pinecones.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Data stored on defender bees spawned by colony expansions.
 */
public record DefenderBeeData(
        boolean isDefender,
        Optional<BlockPos> homeExpansion
) {
    public static final DefenderBeeData EMPTY = new DefenderBeeData(false, Optional.empty());

    public static final Codec<DefenderBeeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("is_defender").forGetter(DefenderBeeData::isDefender),
            BlockPos.CODEC.optionalFieldOf("home_expansion").forGetter(DefenderBeeData::homeExpansion)
    ).apply(instance, DefenderBeeData::new));

    public static final MapCodec<DefenderBeeData> MAP_CODEC = CODEC.fieldOf("defender_bee_data");

    public static final StreamCodec<RegistryFriendlyByteBuf, DefenderBeeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DefenderBeeData::isDefender,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), DefenderBeeData::homeExpansion,
            DefenderBeeData::new
    );

    public static DefenderBeeData create(BlockPos expansionPos) {
        return new DefenderBeeData(true, Optional.of(expansionPos));
    }

    /**
     * Creates a defender bee with no home (e.g., when spawned from a destroyed expansion).
     * These bees will not try to return home and will just fight until they die.
     */
    public static DefenderBeeData createHomeless() {
        return new DefenderBeeData(true, Optional.empty());
    }
}
