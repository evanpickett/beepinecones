package com.mstn.pinecones.data;

import com.mstn.pinecones.api.IBeeCarryDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Data stored on bees to track what they're carrying (pinecone or pollen).
 * In 1.20.1, this is stored in persistent entity NBT data.
 */
public record BeeCarryData(
        ItemStack carriedItem,
        Optional<BlockPos> pickupPos,
        int carryingTicks,
        boolean justLeftNest,
        boolean hasDroppedPollenThisCycle
) {
    public static final BeeCarryData EMPTY = new BeeCarryData(ItemStack.EMPTY, Optional.empty(), 0, false, false);

    private static final String TAG_KEY = "PineconesBeeCarryData";
    private static final String TAG_CARRIED_ITEM = "CarriedItem";
    private static final String TAG_PICKUP_POS = "PickupPos";
    private static final String TAG_CARRYING_TICKS = "CarryingTicks";
    private static final String TAG_JUST_LEFT_NEST = "JustLeftNest";
    private static final String TAG_HAS_DROPPED_POLLEN = "HasDroppedPollen";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        if (!carriedItem.isEmpty()) {
            tag.put(TAG_CARRIED_ITEM, carriedItem.save(new CompoundTag()));
        }
        pickupPos.ifPresent(pos -> tag.put(TAG_PICKUP_POS, NbtUtils.writeBlockPos(pos)));
        tag.putInt(TAG_CARRYING_TICKS, carryingTicks);
        tag.putBoolean(TAG_JUST_LEFT_NEST, justLeftNest);
        tag.putBoolean(TAG_HAS_DROPPED_POLLEN, hasDroppedPollenThisCycle);
        return tag;
    }

    public static BeeCarryData fromNbt(CompoundTag tag) {
        ItemStack item = tag.contains(TAG_CARRIED_ITEM)
                ? ItemStack.of(tag.getCompound(TAG_CARRIED_ITEM))
                : ItemStack.EMPTY;
        Optional<BlockPos> pickupPos = tag.contains(TAG_PICKUP_POS)
                ? Optional.of(NbtUtils.readBlockPos(tag.getCompound(TAG_PICKUP_POS)))
                : Optional.empty();
        int carryingTicks = tag.getInt(TAG_CARRYING_TICKS);
        boolean justLeftNest = tag.getBoolean(TAG_JUST_LEFT_NEST);
        boolean hasDroppedPollen = tag.getBoolean(TAG_HAS_DROPPED_POLLEN);
        return new BeeCarryData(item, pickupPos, carryingTicks, justLeftNest, hasDroppedPollen);
    }

    public static void saveToEntity(Entity entity, BeeCarryData data) {
        entity.getPersistentData().put(TAG_KEY, data.toNbt());
        // Also sync the carried item to clients via mixin
        if (entity instanceof Bee bee && bee instanceof IBeeCarryDataAccessor accessor) {
            accessor.pinecones$setCarriedItem(data.carriedItem.copy());
        }
    }

    public static BeeCarryData loadFromEntity(Entity entity) {
        // On client side, prefer synced data for the carried item
        if (entity.level().isClientSide() && entity instanceof Bee bee && bee instanceof IBeeCarryDataAccessor accessor) {
            ItemStack syncedItem = accessor.pinecones$getCarriedItem();
            if (!syncedItem.isEmpty()) {
                // Return minimal data with just the synced item for rendering
                return new BeeCarryData(syncedItem, Optional.empty(), 0, false, false);
            }
        }
        // Server side or no synced data - use persistent data
        if (entity.getPersistentData().contains(TAG_KEY)) {
            return fromNbt(entity.getPersistentData().getCompound(TAG_KEY));
        }
        return EMPTY;
    }

    public boolean isCarryingItem() {
        return !carriedItem.isEmpty();
    }

    public BeeCarryData withCarriedItem(ItemStack item, BlockPos pickup) {
        return new BeeCarryData(item, Optional.of(pickup), 0, this.justLeftNest, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData clearCarriedItem() {
        return new BeeCarryData(ItemStack.EMPTY, Optional.empty(), 0, this.justLeftNest, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData incrementTicks() {
        return new BeeCarryData(this.carriedItem, this.pickupPos, this.carryingTicks + 1, this.justLeftNest, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData withJustLeftNest(boolean left) {
        return new BeeCarryData(this.carriedItem, this.pickupPos, this.carryingTicks, left, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData withDroppedPollen(boolean dropped) {
        return new BeeCarryData(this.carriedItem, this.pickupPos, this.carryingTicks, this.justLeftNest, dropped);
    }
}
