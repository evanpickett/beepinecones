package com.mstn.pinecones;

import com.mojang.logging.LogUtils;
import com.mstn.pinecones.init.ModBlockEntities;
import com.mstn.pinecones.init.ModBlocks;
import com.mstn.pinecones.init.ModEntityTypes;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModLootModifiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(pinecones.MODID)
public class pinecones {
    public static final String MODID = "pinecones";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Creative tab
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> PINECONES_TAB =
            CREATIVE_MODE_TABS.register("pinecones_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.pinecones"))
                    .withTabsBefore(CreativeModeTabs.NATURAL_BLOCKS)
                    .icon(() -> ModItems.PINECONE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PINECONE.get());
                        output.accept(ModItems.POLLEN.get());
                        output.accept(ModBlocks.COLONY_EXPANSION_ITEM.get());
                    })
                    .build());

    public pinecones() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register lifecycle events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // Register all deferred registers
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Pinecones mod initialized!");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items to vanilla tabs as well
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.PINECONE.get());
            event.accept(ModItems.POLLEN.get());
        }
    }
}
