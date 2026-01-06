package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.block.ColonyExpansionBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, pinecones.MODID);

    public static final Supplier<BlockEntityType<ColonyExpansionBlockEntity>> COLONY_EXPANSION =
            BLOCK_ENTITIES.register("colony_expansion",
                    () -> new BlockEntityType<>(
                            ColonyExpansionBlockEntity::new,
                            ModBlocks.COLONY_EXPANSION.get()
                    ));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
