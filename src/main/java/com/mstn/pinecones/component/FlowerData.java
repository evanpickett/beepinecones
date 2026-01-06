package com.mstn.pinecones.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Stores the flower type that pollen came from.
 * In 1.20.1, this is stored in NBT on the ItemStack.
 */
public record FlowerData(ResourceLocation flowerType) {

    private static final String TAG_FLOWER_TYPE = "FlowerType";
    private static final String TAG_KEY = "FlowerData";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_FLOWER_TYPE, flowerType.toString());
        return tag;
    }

    public static FlowerData fromNbt(CompoundTag tag) {
        ResourceLocation flower = new ResourceLocation(tag.getString(TAG_FLOWER_TYPE));
        return new FlowerData(flower);
    }

    public static void saveToStack(ItemStack stack, FlowerData data) {
        stack.getOrCreateTag().put(TAG_KEY, data.toNbt());
    }

    public static FlowerData loadFromStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_KEY)) {
            return fromNbt(stack.getTag().getCompound(TAG_KEY));
        }
        return null;
    }
}
