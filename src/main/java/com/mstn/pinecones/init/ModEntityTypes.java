package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.entity.PollenEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, pinecones.MODID);

    private static final ResourceKey<EntityType<?>> PINECONE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(pinecones.MODID + ":pinecone"));

    private static final ResourceKey<EntityType<?>> POLLEN_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(pinecones.MODID + ":pollen"));

    public static final Supplier<EntityType<PineconeEntity>> PINECONE_ENTITY =
            ENTITY_TYPES.register("pinecone", () -> EntityType.Builder.<PineconeEntity>of(PineconeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build(PINECONE_KEY));

    public static final Supplier<EntityType<PollenEntity>> POLLEN_ENTITY =
            ENTITY_TYPES.register("pollen", () -> EntityType.Builder.<PollenEntity>of(PollenEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build(POLLEN_KEY));

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
