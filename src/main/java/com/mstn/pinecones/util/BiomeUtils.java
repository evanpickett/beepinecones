package com.mstn.pinecones.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BiomeUtils {

    /**
     * Gets the biome Identifier at the given position.
     */
    public static Identifier getBiomeAt(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        // Get the registry key's string and parse it
        return biomeHolder.unwrapKey()
                .map(key -> {
                    // ResourceKey's location is accessed differently - convert to string
                    String keyString = key.toString();
                    // Format: "ResourceKey[minecraft:worldgen/biome / minecraft:plains]"
                    // Extract the location part after the " / "
                    int slashIndex = keyString.lastIndexOf(" / ");
                    if (slashIndex != -1) {
                        String location = keyString.substring(slashIndex + 3, keyString.length() - 1);
                        return Identifier.parse(location);
                    }
                    return Identifier.parse("minecraft:plains");
                })
                .orElse(Identifier.parse("minecraft:plains"));
    }
}
