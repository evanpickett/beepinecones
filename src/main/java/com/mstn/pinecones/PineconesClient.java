package com.mstn.pinecones;

import com.mstn.pinecones.init.ModEntityTypes;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = pinecones.MODID, dist = Dist.CLIENT)
public class PineconesClient {

    public PineconesClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::onClientSetup);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.PINECONE_ENTITY.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.POLLEN_ENTITY.get(), ItemEntityRenderer::new);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        pinecones.LOGGER.info("Pinecones client initialized!");
    }
}
