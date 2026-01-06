package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.data.DefenderBeeData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registers attachment types for storing data on entities.
 */
public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, pinecones.MODID);

    /**
     * Attachment for storing what a bee is carrying (pinecone or pollen).
     * Synced to clients for rendering.
     */
    public static final Supplier<AttachmentType<BeeCarryData>> BEE_CARRY_DATA =
            ATTACHMENT_TYPES.register("bee_carry_data",
                    () -> AttachmentType.builder(() -> BeeCarryData.EMPTY)
                            .serialize(BeeCarryData.MAP_CODEC)
                            .sync(BeeCarryData.STREAM_CODEC)
                            .build());

    /**
     * Attachment for defender bees spawned by colony expansions.
     * Synced to clients for rendering (smaller scale).
     */
    public static final Supplier<AttachmentType<DefenderBeeData>> DEFENDER_BEE_DATA =
            ATTACHMENT_TYPES.register("defender_bee_data",
                    () -> AttachmentType.builder(() -> DefenderBeeData.EMPTY)
                            .serialize(DefenderBeeData.MAP_CODEC)
                            .sync(DefenderBeeData.STREAM_CODEC)
                            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
