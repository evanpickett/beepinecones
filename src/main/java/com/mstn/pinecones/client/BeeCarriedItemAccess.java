package com.mstn.pinecones.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Interface injected into BeeRenderState to provide access to carried item data
 * and defender bee state.
 */
public interface BeeCarriedItemAccess {

    ItemStackRenderState pinecones$getCarriedItemState();

    void pinecones$setCarriedItemState(ItemStackRenderState state);

    boolean pinecones$hasCarriedItem();

    void pinecones$setHasCarriedItem(boolean hasItem);

    int pinecones$getEntityId();

    void pinecones$setEntityId(int id);

    boolean pinecones$isDefenderBee();

    void pinecones$setDefenderBee(boolean isDefender);
}
