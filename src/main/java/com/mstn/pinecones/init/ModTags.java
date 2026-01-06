package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> TREE_LOGS = tag("tree_logs");
        public static final TagKey<Block> TREE_LEAVES = tag("tree_leaves");
        public static final TagKey<Block> FLOWERS = tag("flowers");
        public static final TagKey<Block> FLOWER_PLANTABLE = tag("flower_plantable");
        public static final TagKey<Block> SAPLINGS = TagKey.create(Registries.BLOCK, new ResourceLocation("minecraft", "saplings"));

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(pinecones.MODID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> SAPLINGS = TagKey.create(Registries.ITEM, new ResourceLocation("minecraft", "saplings"));

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(pinecones.MODID, name));
        }
    }
}
