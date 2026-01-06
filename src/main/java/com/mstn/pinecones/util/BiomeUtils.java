package com.mstn.pinecones.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class BiomeUtils {

    /**
     * Gets the biome ResourceLocation at the given position.
     */
    public static ResourceLocation getBiomeAt(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        // Get the registry key's location directly
        return biomeHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(new ResourceLocation("minecraft:plains"));
    }
}
