package com.mstn.pinecones.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Stores the flower type that pollen came from.
 */
public record FlowerData(Identifier flowerType) {

    public static final Codec<FlowerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("flower_type").forGetter(FlowerData::flowerType)
            ).apply(instance, FlowerData::new)
    );

    public static final MapCodec<FlowerData> MAP_CODEC = CODEC.fieldOf("flower_data");

    public static final StreamCodec<ByteBuf, FlowerData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, FlowerData::flowerType,
            FlowerData::new
    );
}
