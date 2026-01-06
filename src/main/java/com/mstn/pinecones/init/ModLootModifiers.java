package com.mstn.pinecones.init;

import com.mojang.serialization.Codec;
import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.loot.ReplaceSaplingWithPineconeLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModLootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, pinecones.MODID);

    public static final RegistryObject<Codec<ReplaceSaplingWithPineconeLootModifier>> REPLACE_SAPLING_WITH_PINECONE =
            LOOT_MODIFIER_SERIALIZERS.register("replace_sapling_with_pinecone",
                    ReplaceSaplingWithPineconeLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
