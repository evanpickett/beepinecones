package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.entity.PollenEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, pinecones.MODID);

    public static final RegistryObject<EntityType<PineconeEntity>> PINECONE_ENTITY =
            ENTITY_TYPES.register("pinecone", () -> EntityType.Builder.<PineconeEntity>of(PineconeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build(new ResourceLocation(pinecones.MODID, "pinecone").toString()));

    public static final RegistryObject<EntityType<PollenEntity>> POLLEN_ENTITY =
            ENTITY_TYPES.register("pollen", () -> EntityType.Builder.<PollenEntity>of(PollenEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build(new ResourceLocation(pinecones.MODID, "pollen").toString()));

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
