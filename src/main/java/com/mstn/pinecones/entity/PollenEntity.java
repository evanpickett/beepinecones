package com.mstn.pinecones.entity;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.command.PineconesCommand;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.init.ModDataComponents;
import com.mstn.pinecones.init.ModEntityTypes;
import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class PollenEntity extends ItemEntity {

    private int ticksOnGround = 0;
    private static final int PLANT_CHECK_INTERVAL = 20; // Check every second

    public PollenEntity(EntityType<? extends ItemEntity> entityType, Level level) {
        super(entityType, level);
    }

    public PollenEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntityTypes.POLLEN_ENTITY.get(), level);
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

            // Check for planting periodically
            if (ticksOnGround % PLANT_CHECK_INTERVAL == 0) {
                // Use same chance system as pinecones
                double plantChance = PineconesCommand.replantChanceOverride != null
                        ? PineconesCommand.replantChanceOverride
                        : Config.PINECONE_REPLANT_CHANCE.get();
                if (level().random.nextFloat() < plantChance) {
                    if (tryPlantFlower()) {
                        discard();
                        return;
                    }
                }
            }
        } else {
            ticksOnGround = 0;
        }
    }

    private boolean tryPlantFlower() {
        BlockPos pos = blockPosition();
        ItemStack stack = getItem();

        // Get flower data
        FlowerData flowerData = stack.get(ModDataComponents.FLOWER_DATA.get());
        if (flowerData == null) {
            return false;
        }

        // Get flower block
        Block flowerBlock = BuiltInRegistries.BLOCK.getValue(flowerData.flowerType());
        if (flowerBlock == Blocks.AIR) {
            return false;
        }

        // Find a plantable position
        BlockPos plantPos = findPlantablePosition(pos, flowerBlock);
        if (plantPos == null) {
            return false;
        }

        // Plant the flower
        BlockState flowerState = flowerBlock.defaultBlockState();
        level().setBlock(plantPos, flowerState, Block.UPDATE_ALL);
        return true;
    }

    @Nullable
    private BlockPos findPlantablePosition(BlockPos startPos, Block flowerBlock) {
        BlockState flowerState = flowerBlock.defaultBlockState();

        // Only check directly below the pollen (no offset)
        // Try at the position and one block down (in case pollen is floating slightly)
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos plantPos = startPos.above(dy);
            BlockState stateAt = level().getBlockState(plantPos);
            BlockState stateBelow = level().getBlockState(plantPos.below());

            // Check if air and valid soil below
            if (stateAt.isAir() && isValidSoil(stateBelow)) {
                // Check if flower can survive here
                if (flowerState.canSurvive(level(), plantPos)) {
                    return plantPos;
                }
            }
        }
        return null;
    }

    private boolean isValidSoil(BlockState state) {
        return state.is(ModTags.Blocks.FLOWER_PLANTABLE);
    }
}
