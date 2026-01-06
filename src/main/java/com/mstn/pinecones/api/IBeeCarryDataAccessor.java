package com.mstn.pinecones.api;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for accessing synced carried item data on bees.
 * Implemented by BeeCarryDataMixin.
 */
public interface IBeeCarryDataAccessor {
    ItemStack pinecones$getCarriedItem();
    void pinecones$setCarriedItem(ItemStack item);
}
