package com.mstn.pinecones.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.init.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Render layer that displays the item a bee is carrying underneath its body.
 * Prioritizes pre-extracted render state data from MobRendererMixin for consistency,
 * with fallback to direct entity lookup.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BeeCarriedItemLayer extends RenderLayer {

    private final ItemStackRenderState fallbackItemState = new ItemStackRenderState();

    public BeeCarriedItemLayer(RenderLayerParent renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       EntityRenderState renderStateObj, float yRot, float xRot) {

        if (!(renderStateObj instanceof BeeRenderState renderState)) {
            return;
        }

        if (!(renderState instanceof BeeCarriedItemAccess access)) {
            return;
        }

        ItemStackRenderState itemStateToRender = null;

        // Priority 1: Use pre-extracted render state from MobRendererMixin
        if (access.pinecones$hasCarriedItem()) {
            ItemStackRenderState extracted = access.pinecones$getCarriedItemState();
            if (extracted != null && !extracted.isEmpty()) {
                itemStateToRender = extracted;
            }
        }

        // Fallback: Direct entity lookup (for cases where extraction didn't work)
        if (itemStateToRender == null) {
            Bee bee = findBeeById(access);
            if (bee != null) {
                BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());
                ItemStack carriedItem = data.carriedItem();
                if (!carriedItem.isEmpty()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        mc.getItemModelResolver().updateForTopItem(
                                fallbackItemState,
                                carriedItem,
                                ItemDisplayContext.GROUND,
                                mc.level,
                                null,
                                0
                        );
                        if (!fallbackItemState.isEmpty()) {
                            itemStateToRender = fallbackItemState;
                        }
                    }
                }
            }
        }

        if (itemStateToRender == null) {
            return;
        }

        poseStack.pushPose();

        // Position the item underneath the bee's body (positive Y is down in model space)
        // Match the bee's natural hover bob with subtle movement
        float beeBob = (float) Math.cos(renderState.ageInTicks * 0.3F) * 0.035F;
        poseStack.translate(0.0, 1.35 + beeBob, 0.0);

        // Submit the item render state
        itemStateToRender.submit(poseStack, collector, packedLight, 0, 0);

        poseStack.popPose();
    }

    private Bee findBeeById(BeeCarriedItemAccess access) {
        int entityId = access.pinecones$getEntityId();
        if (entityId >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(entityId);
                if (entity instanceof Bee bee) {
                    return bee;
                }
            }
        }
        return null;
    }
}
