package com.mstn.pinecones.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Stores the origin data for a pinecone: the type of tree it came from and the biome.
 */
public record TreeOriginData(Identifier woodType, Identifier biome) {

    public static final Codec<TreeOriginData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("wood_type").forGetter(TreeOriginData::woodType),
                    Identifier.CODEC.fieldOf("biome").forGetter(TreeOriginData::biome)
            ).apply(instance, TreeOriginData::new)
    );

    public static final MapCodec<TreeOriginData> MAP_CODEC = CODEC.fieldOf("tree_origin");

    public static final StreamCodec<ByteBuf, TreeOriginData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, TreeOriginData::woodType,
            Identifier.STREAM_CODEC, TreeOriginData::biome,
            TreeOriginData::new
    );
}
