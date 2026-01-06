package com.mstn.pinecones.mixin;

import com.mstn.pinecones.client.BeeCarriedItemAccess;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.init.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to extract carried item data for bees during render state extraction.
 */
@Mixin(LivingEntityRenderer.class)
public class MobRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void pinecones$resetBeeCarriedItem(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        // Reset flags at HEAD to ensure they're cleared each frame
        if (entity instanceof Bee && state instanceof BeeCarriedItemAccess access) {
            access.pinecones$setHasCarriedItem(false);
            access.pinecones$setDefenderBee(false);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void pinecones$extractBeeCarriedItem(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!(entity instanceof Bee bee)) {
            return;
        }

        if (!(state instanceof BeeCarriedItemAccess access)) {
            return;
        }

        // Always set the entity ID for lookup
        access.pinecones$setEntityId(bee.getId());

        // Check if this is a defender bee
        DefenderBeeData defenderData = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        access.pinecones$setDefenderBee(defenderData.isDefender());

        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());
        ItemStack carriedItem = data.carriedItem();

        if (carriedItem.isEmpty()) {
            return;
        }

        access.pinecones$setHasCarriedItem(true);

        // Update the item render state
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.getItemModelResolver().updateForTopItem(
                    access.pinecones$getCarriedItemState(),
                    carriedItem,
                    ItemDisplayContext.GROUND,
                    mc.level,
                    null,
                    0
            );
        }
    }
}
