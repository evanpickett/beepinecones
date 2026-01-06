package com.mstn.pinecones.ai;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.init.ModAttachments;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * AI goal for bees to pick up pollen items from the ground.
 */
public class BeeCarryPollenGoal extends Goal {

    private final Bee bee;
    private ItemEntity targetPollen;

    public BeeCarryPollenGoal(Bee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());

        if (data.isCarryingItem()) {
            return false;
        }

        if (data.hasDroppedPollenThisCycle()) {
            return false;
        }

        if (bee.hasNectar()) {
            return false;
        }

        if (!isWithinNestRadius()) {
            return false;
        }

        targetPollen = findNearbyPollen();
        return targetPollen != null;
    }

    @Override
    public boolean canContinueToUse() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());

        if (data.isCarryingItem()) {
            return false;
        }

        return targetPollen != null && targetPollen.isAlive() && targetPollen.onGround();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        targetPollen = null;
    }

    @Override
    public void tick() {
        if (targetPollen == null || !targetPollen.isAlive() || !targetPollen.onGround()) {
            targetPollen = null;
            return;
        }

        bee.getNavigation().moveTo(targetPollen, 1.0);

        double dist = bee.position().distanceToSqr(targetPollen.position());
        if (dist < 1.5) {
            if (!targetPollen.isAlive() || !targetPollen.onGround()) {
                targetPollen = null;
                return;
            }
            if (isValidPlantingSpot(targetPollen.blockPosition())) {
                targetPollen = null;
                return;
            }
            pickUpPollen();
        }
    }

    private void pickUpPollen() {
        if (targetPollen == null || !targetPollen.isAlive()) return;

        ItemStack stack = targetPollen.getItem().copy();
        BlockPos pickupPos = targetPollen.blockPosition();

        BeeCarryData newData = bee.getData(ModAttachments.BEE_CARRY_DATA.get())
                .withCarriedItem(stack, pickupPos);
        bee.setData(ModAttachments.BEE_CARRY_DATA.get(), newData);

        targetPollen.discard();
        targetPollen = null;
    }

    @Nullable
    private ItemEntity findNearbyPollen() {
        int radius = Config.BEE_POLLEN_SEARCH_RADIUS.get();
        AABB searchBox = bee.getBoundingBox().inflate(radius);

        List<ItemEntity> items = bee.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item.getItem().is(ModItems.POLLEN.get()) && item.isAlive() && item.onGround());

        if (items.isEmpty()) return null;

        ItemEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            if (isValidPlantingSpot(item.blockPosition())) {
                continue;
            }

            double dist = bee.distanceToSqr(item);
            if (dist < closestDist) {
                closestDist = dist;
                closest = item;
            }
        }

        return closest;
    }

    /**
     * Checks if a position is already a valid spot for planting a flower (no need to move it).
     */
    private boolean isValidPlantingSpot(BlockPos pos) {
        Level level = bee.level();
        BlockState below = level.getBlockState(pos.below());
        BlockState at = level.getBlockState(pos);

        if (!at.isAir()) return false;

        if (!below.is(ModTags.Blocks.FLOWER_PLANTABLE)) return false;

        if (hasNearbyFlower(pos, 2)) return false;

        int spacing = Config.PINECONE_TREE_SPACING.get();
        if (spacing > 0 && hasNearbyPollen(pos, spacing)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if there is a flower within the specified distance.
     */
    private boolean hasNearbyFlower(BlockPos pos, int distance) {
        Level level = bee.level();
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).is(ModTags.Blocks.FLOWERS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if there is other pollen on the ground within the specified distance.
     */
    private boolean hasNearbyPollen(BlockPos pos, int distance) {
        AABB searchBox = new AABB(pos).inflate(distance);
        List<ItemEntity> nearbyPollen = bee.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item.getItem().is(ModItems.POLLEN.get()) && item.isAlive() && item.onGround());

        for (ItemEntity pollen : nearbyPollen) {
            double dist = pollen.blockPosition().distSqr(pos);
            if (dist < 1) continue;
            if (dist <= distance * distance) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinNestRadius() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos == null) return true;

        double dist = bee.blockPosition().distSqr(hivePos);
        int radius = Config.BEE_NEST_RADIUS.get();
        return dist <= radius * radius;
    }
}
