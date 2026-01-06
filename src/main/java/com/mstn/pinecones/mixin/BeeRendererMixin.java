package com.mstn.pinecones.mixin;

import com.mstn.pinecones.client.BeeCarriedItemLayer;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add carried item render layer to bees.
 * Note: Defender bee scaling is handled in LivingEntityRendererMixin.
 */
@Mixin(BeeRenderer.class)
public class BeeRendererMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void pinecones$addCarriedItemLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        BeeRenderer self = (BeeRenderer)(Object)this;
        self.addLayer((net.minecraft.client.renderer.entity.layers.RenderLayer) new BeeCarriedItemLayer(self));
    }
}
