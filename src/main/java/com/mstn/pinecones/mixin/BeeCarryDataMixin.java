package com.mstn.pinecones.mixin;

import com.mstn.pinecones.api.IBeeCarryDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add synced carried item data to bees for client-side rendering.
 */
@Mixin(Bee.class)
public abstract class BeeCarryDataMixin extends Animal implements IBeeCarryDataAccessor {

    @Unique
    private static final EntityDataAccessor<ItemStack> PINECONES_CARRIED_ITEM =
            SynchedEntityData.defineId(Bee.class, EntityDataSerializers.ITEM_STACK);

    protected BeeCarryDataMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void pinecones$defineSynchedData(CallbackInfo ci) {
        this.entityData.define(PINECONES_CARRIED_ITEM, ItemStack.EMPTY);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void pinecones$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        ItemStack carried = this.entityData.get(PINECONES_CARRIED_ITEM);
        if (!carried.isEmpty()) {
            tag.put("PineconesCarriedItem", carried.save(new CompoundTag()));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void pinecones$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("PineconesCarriedItem")) {
            this.entityData.set(PINECONES_CARRIED_ITEM, ItemStack.of(tag.getCompound("PineconesCarriedItem")));
        }
    }

    /**
     * Gets the synced carried item for client-side rendering.
     */
    @Override
    public ItemStack pinecones$getCarriedItem() {
        return this.entityData.get(PINECONES_CARRIED_ITEM);
    }

    /**
     * Sets the synced carried item (call from server).
     */
    @Override
    public void pinecones$setCarriedItem(ItemStack item) {
        this.entityData.set(PINECONES_CARRIED_ITEM, item);
    }
}
