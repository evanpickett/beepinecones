package com.mstn.pinecones.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * Data stored on defender bees spawned by colony expansions.
 * In 1.20.1, this is stored in persistent entity NBT data.
 */
public record DefenderBeeData(
        boolean isDefender,
        Optional<BlockPos> homeExpansion
) {
    public static final DefenderBeeData EMPTY = new DefenderBeeData(false, Optional.empty());

    private static final String TAG_KEY = "PineconesDefenderBeeData";
    private static final String TAG_IS_DEFENDER = "IsDefender";
    private static final String TAG_HOME_EXPANSION = "HomeExpansion";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_IS_DEFENDER, isDefender);
        homeExpansion.ifPresent(pos -> tag.put(TAG_HOME_EXPANSION, NbtUtils.writeBlockPos(pos)));
        return tag;
    }

    public static DefenderBeeData fromNbt(CompoundTag tag) {
        boolean isDefender = tag.getBoolean(TAG_IS_DEFENDER);
        Optional<BlockPos> homeExpansion = tag.contains(TAG_HOME_EXPANSION)
                ? Optional.of(NbtUtils.readBlockPos(tag.getCompound(TAG_HOME_EXPANSION)))
                : Optional.empty();
        return new DefenderBeeData(isDefender, homeExpansion);
    }

    public static void saveToEntity(Entity entity, DefenderBeeData data) {
        entity.getPersistentData().put(TAG_KEY, data.toNbt());
    }

    public static DefenderBeeData loadFromEntity(Entity entity) {
        if (entity.getPersistentData().contains(TAG_KEY)) {
            return fromNbt(entity.getPersistentData().getCompound(TAG_KEY));
        }
        return EMPTY;
    }

    public static DefenderBeeData create(BlockPos expansionPos) {
        return new DefenderBeeData(true, Optional.of(expansionPos));
    }

    /**
     * Creates a defender bee with no home (e.g., when spawned from a destroyed expansion).
     * These bees will not try to return home and will just fight until they die.
     */
    public static DefenderBeeData createHomeless() {
        return new DefenderBeeData(true, Optional.empty());
    }
}
