package com.mstn.pinecones.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.init.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientSetup {

    @Mod.EventBusSubscriber(modid = pinecones.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.PINECONE_ENTITY.get(), ItemEntityRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.POLLEN_ENTITY.get(), ItemEntityRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = pinecones.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onRenderBee(RenderLivingEvent.Post<Bee, ?> event) {
            if (!(event.getEntity() instanceof Bee bee)) return;

            BeeCarryData data = BeeCarryData.loadFromEntity(bee);
            if (!data.isCarryingItem()) return;

            ItemStack carriedItem = data.carriedItem();
            if (carriedItem.isEmpty()) return;

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();

            poseStack.pushPose();

            // The pose stack is already transformed to the entity's position
            // We just need to position the item relative to the bee's body

            // Get interpolated body rotation
            float partialTick = event.getPartialTick();
            float bodyYaw = bee.yBodyRotO + (bee.yBodyRot - bee.yBodyRotO) * partialTick;

            // Undo the renderer's rotation and apply our own
            poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));

            // Position below the bee's abdomen
            poseStack.translate(0.0, 0.0, 0.0);

            // Rotate 90 degrees on Z axis so item faces forward
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));

            // Scale the item
            poseStack.scale(0.7f, 0.7f, 0.7f);

            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            itemRenderer.renderStatic(
                    carriedItem,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    bee.level(),
                    bee.getId()
            );

            poseStack.popPose();
        }
    }
}
