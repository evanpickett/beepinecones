package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.block.ColonyExpansionBlock;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(pinecones.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(pinecones.MODID);

    public static final DeferredBlock<ColonyExpansionBlock> COLONY_EXPANSION = BLOCKS.registerBlock(
            "colony_expansion",
            ColonyExpansionBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.6F)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)
                    .noOcclusion());

    public static final DeferredItem<?> COLONY_EXPANSION_ITEM = ITEMS.registerSimpleBlockItem(COLONY_EXPANSION);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModBlocks::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(COLONY_EXPANSION_ITEM);
        }
    }
}
