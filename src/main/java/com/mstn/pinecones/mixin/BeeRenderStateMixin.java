package com.mstn.pinecones.mixin;

import com.mstn.pinecones.client.BeeCarriedItemAccess;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add carried item data to BeeRenderState.
 */
@Mixin(BeeRenderState.class)
public class BeeRenderStateMixin implements BeeCarriedItemAccess {

    @Unique
    private final ItemStackRenderState pinecones$carriedItemState = new ItemStackRenderState();

    @Unique
    private boolean pinecones$hasCarriedItem = false;

    @Unique
    private int pinecones$entityId = -1;

    @Unique
    private boolean pinecones$isDefenderBee = false;

    @Override
    public ItemStackRenderState pinecones$getCarriedItemState() {
        return pinecones$carriedItemState;
    }

    @Override
    public void pinecones$setCarriedItemState(ItemStackRenderState state) {
        // The state is stored in the final field, so we copy data if needed
        // For now, updateForTopItem is called directly on our field
    }

    @Override
    public boolean pinecones$hasCarriedItem() {
        return pinecones$hasCarriedItem;
    }

    @Override
    public void pinecones$setHasCarriedItem(boolean hasItem) {
        this.pinecones$hasCarriedItem = hasItem;
    }

    @Override
    public int pinecones$getEntityId() {
        return pinecones$entityId;
    }

    @Override
    public void pinecones$setEntityId(int id) {
        this.pinecones$entityId = id;
    }

    @Override
    public boolean pinecones$isDefenderBee() {
        return pinecones$isDefenderBee;
    }

    @Override
    public void pinecones$setDefenderBee(boolean isDefender) {
        this.pinecones$isDefenderBee = isDefender;
    }
}
