package com.mstn.pinecones.entity;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.command.PineconesCommand;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.init.ModEntityTypes;
import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PineconeEntity extends ItemEntity {

    private int ticksOnGround = 0;
    private static final int REPLANT_CHECK_INTERVAL = 20; // Check every second

    // Map wood types to their saplings
    private static final Map<String, Block> WOOD_TO_SAPLING = new HashMap<>();

    static {
        // Vanilla mappings
        WOOD_TO_SAPLING.put("minecraft:oak_log", Blocks.OAK_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:spruce_log", Blocks.SPRUCE_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:birch_log", Blocks.BIRCH_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:jungle_log", Blocks.JUNGLE_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:acacia_log", Blocks.ACACIA_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:dark_oak_log", Blocks.DARK_OAK_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:mangrove_log", Blocks.MANGROVE_PROPAGULE);
        WOOD_TO_SAPLING.put("minecraft:cherry_log", Blocks.CHERRY_SAPLING);
        // Leaves mappings (fallback)
        WOOD_TO_SAPLING.put("minecraft:oak_leaves", Blocks.OAK_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:spruce_leaves", Blocks.SPRUCE_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:birch_leaves", Blocks.BIRCH_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:jungle_leaves", Blocks.JUNGLE_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:acacia_leaves", Blocks.ACACIA_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:dark_oak_leaves", Blocks.DARK_OAK_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:mangrove_leaves", Blocks.MANGROVE_PROPAGULE);
        WOOD_TO_SAPLING.put("minecraft:cherry_leaves", Blocks.CHERRY_SAPLING);
        WOOD_TO_SAPLING.put("minecraft:azalea_leaves", Blocks.AZALEA);
        WOOD_TO_SAPLING.put("minecraft:flowering_azalea_leaves", Blocks.FLOWERING_AZALEA);
    }

    public PineconeEntity(EntityType<? extends ItemEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PineconeEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntityTypes.PINECONE_ENTITY.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setDeltaMovement(
                level.random.nextDouble() * 0.2 - 0.1,
                0.2,
                level.random.nextDouble() * 0.2 - 0.1
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        if (onGround()) {
            ticksOnGround++;

            // Must wait minimum delay before planting
            int minDelay = Config.PLANT_DELAY_TICKS.get();
            if (ticksOnGround < minDelay) return;

            // Check for replanting periodically (use override if set, otherwise config)
            if (ticksOnGround % REPLANT_CHECK_INTERVAL == 0) {
                double replantChance = PineconesCommand.replantChanceOverride != null
                        ? PineconesCommand.replantChanceOverride
                        : Config.PINECONE_REPLANT_CHANCE.get();
                if (level().random.nextFloat() < replantChance) {
                    if (tryReplant()) {
                        discard();
                        return;
                    }
                }
            }
        } else {
            ticksOnGround = 0;
        }
    }

    private boolean tryReplant() {
        BlockPos pos = blockPosition();
        ItemStack stack = getItem();

        // Get tree origin data from NBT
        TreeOriginData originData = TreeOriginData.loadFromStack(stack);
        if (originData == null) {
            return false;
        }

        // Find the sapling for this wood type
        Block sapling = getSaplingForWoodType(originData.woodType());
        if (sapling == null) {
            return false;
        }

        // Check if position is valid for planting
        BlockPos plantPos = findPlantablePosition(pos);
        if (plantPos == null) {
            return false;
        }

        // Check if too close to existing trees or saplings
        int spacingDistance = Config.PINECONE_TREE_SPACING.get();
        if (spacingDistance > 0 && isNearTreeOrSapling(plantPos, spacingDistance)) {
            return false;
        }

        // Check if sapling can survive here
        BlockState saplingState = sapling.defaultBlockState();
        if (!saplingState.canSurvive(level(), plantPos)) {
            return false;
        }

        // Plant the sapling
        level().setBlock(plantPos, saplingState, Block.UPDATE_ALL);
        return true;
    }

    /**
     * Checks if there's a tree (log) or sapling within the specified distance.
     */
    private boolean isNearTreeOrSapling(BlockPos pos, int distance) {
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);

                    // Check for logs
                    if (state.is(ModTags.Blocks.TREE_LOGS)) {
                        return true;
                    }

                    // Check for saplings
                    if (state.getBlock() instanceof SaplingBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findPlantablePosition(BlockPos startPos) {
        // Only check directly below the pinecone (no offset)
        // Try at the position and one block down (in case pinecone is floating slightly)
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos plantPos = startPos.above(dy);
            BlockState stateAt = level().getBlockState(plantPos);
            BlockState stateBelow = level().getBlockState(plantPos.below());

            // Check if air and valid soil below
            if (stateAt.isAir() && isValidSoil(stateBelow)) {
                return plantPos;
            }
        }
        return null;
    }

    private boolean isValidSoil(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) ||
                state.is(Blocks.DIRT) ||
                state.is(Blocks.COARSE_DIRT) ||
                state.is(Blocks.PODZOL) ||
                state.is(Blocks.ROOTED_DIRT) ||
                state.is(Blocks.MOSS_BLOCK) ||
                state.is(Blocks.MUD) ||
                state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    @Nullable
    private Block getSaplingForWoodType(ResourceLocation woodType) {
        String woodTypeStr = woodType.toString();

        // Direct lookup
        Block sapling = WOOD_TO_SAPLING.get(woodTypeStr);
        if (sapling != null) {
            return sapling;
        }

        // Try to find sapling by naming convention
        // e.g., "modid:some_log" -> try "modid:some_sapling"
        String saplingName = woodTypeStr
                .replace("_log", "_sapling")
                .replace("_wood", "_sapling")
                .replace("_leaves", "_sapling");

        Block foundSapling = BuiltInRegistries.BLOCK.get(new ResourceLocation(saplingName));
        if (foundSapling != Blocks.AIR && foundSapling instanceof SaplingBlock) {
            return foundSapling;
        }

        return null;
    }
}
