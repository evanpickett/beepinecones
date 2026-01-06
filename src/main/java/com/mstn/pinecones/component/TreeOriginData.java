package com.mstn.pinecones.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Stores the origin data for a pinecone: the type of tree it came from and the biome.
 * In 1.20.1, this is stored in NBT on the ItemStack.
 */
public record TreeOriginData(ResourceLocation woodType, ResourceLocation biome) {

    private static final String TAG_WOOD_TYPE = "WoodType";
    private static final String TAG_BIOME = "Biome";
    private static final String TAG_KEY = "TreeOrigin";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_WOOD_TYPE, woodType.toString());
        tag.putString(TAG_BIOME, biome.toString());
        return tag;
    }

    public static TreeOriginData fromNbt(CompoundTag tag) {
        ResourceLocation wood = new ResourceLocation(tag.getString(TAG_WOOD_TYPE));
        ResourceLocation biome = new ResourceLocation(tag.getString(TAG_BIOME));
        return new TreeOriginData(wood, biome);
    }

    public static void saveToStack(ItemStack stack, TreeOriginData data) {
        stack.getOrCreateTag().put(TAG_KEY, data.toNbt());
    }

    public static TreeOriginData loadFromStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_KEY)) {
            return fromNbt(stack.getTag().getCompound(TAG_KEY));
        }
        return null;
    }
}
