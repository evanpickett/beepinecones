package com.mstn.pinecones.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Data stored on bees to track what they're carrying (pinecone or pollen).
 */
public record BeeCarryData(
        ItemStack carriedItem,
        Optional<BlockPos> pickupPos,
        int carryingTicks,
        boolean justLeftNest,
        boolean hasDroppedPollenThisCycle
) {
    public static final BeeCarryData EMPTY = new BeeCarryData(ItemStack.EMPTY, Optional.empty(), 0, false, false);

    public static final Codec<BeeCarryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("carried_item").forGetter(BeeCarryData::carriedItem),
            BlockPos.CODEC.optionalFieldOf("pickup_pos").forGetter(BeeCarryData::pickupPos),
            Codec.INT.fieldOf("carrying_ticks").forGetter(BeeCarryData::carryingTicks),
            Codec.BOOL.fieldOf("just_left_nest").forGetter(BeeCarryData::justLeftNest),
            Codec.BOOL.fieldOf("has_dropped_pollen").forGetter(BeeCarryData::hasDroppedPollenThisCycle)
    ).apply(instance, BeeCarryData::new));

    public static final MapCodec<BeeCarryData> MAP_CODEC = CODEC.fieldOf("bee_carry_data");

    /**
     * Stream codec for network synchronization.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, BeeCarryData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, BeeCarryData::carriedItem,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), BeeCarryData::pickupPos,
            ByteBufCodecs.VAR_INT, BeeCarryData::carryingTicks,
            ByteBufCodecs.BOOL, BeeCarryData::justLeftNest,
            ByteBufCodecs.BOOL, BeeCarryData::hasDroppedPollenThisCycle,
            BeeCarryData::new
    );

    public boolean isCarryingItem() {
        return !carriedItem.isEmpty();
    }

    public BeeCarryData withCarriedItem(ItemStack item, BlockPos pickup) {
        return new BeeCarryData(item, Optional.of(pickup), 0, this.justLeftNest, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData clearCarriedItem() {
        return new BeeCarryData(ItemStack.EMPTY, Optional.empty(), 0, this.justLeftNest, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData incrementTicks() {
        return new BeeCarryData(this.carriedItem, this.pickupPos, this.carryingTicks + 1, this.justLeftNest, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData withJustLeftNest(boolean left) {
        return new BeeCarryData(this.carriedItem, this.pickupPos, this.carryingTicks, left, this.hasDroppedPollenThisCycle);
    }

    public BeeCarryData withDroppedPollen(boolean dropped) {
        return new BeeCarryData(this.carriedItem, this.pickupPos, this.carryingTicks, this.justLeftNest, dropped);
    }
}
