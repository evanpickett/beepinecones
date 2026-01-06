package com.mstn.pinecones.ai;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.init.ModAttachments;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import com.mstn.pinecones.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * AI goal for bees to pick up pinecones and drop them away from trees.
 */
public class BeeCarryPineconeGoal extends Goal {

    private final Bee bee;
    private ItemEntity targetPinecone;
    private BlockPos dropTarget;
    private int ticksCarrying;

    public BeeCarryPineconeGoal(Bee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());

        if (data.isCarryingItem() && data.carriedItem().is(ModItems.PINECONE.get())) {
            return true;
        }

        if (data.isCarryingItem()) {
            return false;
        }

        if (!isWithinNestRadius()) {
            return false;
        }

        targetPinecone = findNearbyPinecone();
        if (targetPinecone == null) {
            return false;
        }

        dropTarget = findDropLocation(targetPinecone.blockPosition());
        return dropTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());

        if (data.isCarryingItem()) {
            return true;
        }

        return targetPinecone != null && targetPinecone.isAlive() && targetPinecone.onGround() && dropTarget != null;
    }

    @Override
    public void start() {
        ticksCarrying = 0;
    }

    @Override
    public void stop() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());

        if (data.isCarryingItem()) {
            dropCarriedItem();
        }

        bee.setData(ModAttachments.BEE_CARRY_DATA.get(), data.withJustLeftNest(false));

        targetPinecone = null;
        dropTarget = null;
    }

    @Override
    public void tick() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());

        if (data.isCarryingItem()) {
            ticksCarrying++;

            if (dropTarget == null) {
                dropTarget = findDropLocation(data.pickupPos().orElse(bee.blockPosition()));
            }

            if (dropTarget != null) {
                bee.getNavigation().moveTo(dropTarget.getX() + 0.5, dropTarget.getY() + 1, dropTarget.getZ() + 0.5, 1.0);

                double dist = bee.position().distanceToSqr(Vec3.atCenterOf(dropTarget));
                if (dist < 2.0) {
                    dropCarriedItem();
                    return;
                }
            }

            if (ticksCarrying > Config.BEE_PINECONE_DROP_TIMEOUT_TICKS.get()) {
                dropCarriedItem();
            }
        } else if (targetPinecone != null && targetPinecone.isAlive() && targetPinecone.onGround()) {
            bee.getNavigation().moveTo(targetPinecone, 1.0);

            double dist = bee.position().distanceToSqr(targetPinecone.position());
            if (dist < 1.5) {
                if (!targetPinecone.isAlive() || !targetPinecone.onGround()) {
                    targetPinecone = null;
                    return;
                }
                if (isValidPlantingSpot(targetPinecone.blockPosition())) {
                    targetPinecone = null;
                    return;
                }
                pickUpPinecone();
            }
        } else {
            targetPinecone = null;
        }
    }

    private void pickUpPinecone() {
        if (targetPinecone == null || !targetPinecone.isAlive()) return;

        ItemStack stack = targetPinecone.getItem().copy();
        BlockPos pickupPos = targetPinecone.blockPosition();

        BeeCarryData newData = bee.getData(ModAttachments.BEE_CARRY_DATA.get())
                .withCarriedItem(stack, pickupPos);
        bee.setData(ModAttachments.BEE_CARRY_DATA.get(), newData);

        targetPinecone.discard();
        targetPinecone = null;
    }

    private void dropCarriedItem() {
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());
        if (!data.isCarryingItem()) return;

        Level level = bee.level();
        BlockPos dropPos = bee.blockPosition();

        PineconeEntity pinecone = new PineconeEntity(
                level,
                dropPos.getX() + 0.5,
                dropPos.getY() + 0.5,
                dropPos.getZ() + 0.5,
                data.carriedItem().copy()
        );
        level.addFreshEntity(pinecone);

        bee.setData(ModAttachments.BEE_CARRY_DATA.get(), data.clearCarriedItem());
    }

    @Nullable
    private ItemEntity findNearbyPinecone() {
        int radius = Config.BEE_PINECONE_SEARCH_RADIUS.get();
        AABB searchBox = bee.getBoundingBox().inflate(radius);

        List<ItemEntity> items = bee.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item.getItem().is(ModItems.PINECONE.get()) && item.isAlive() && item.onGround());

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
     * Checks if a position is already a valid spot for planting (no need to move it).
     */
    private boolean isValidPlantingSpot(BlockPos pos) {
        Level level = bee.level();
        BlockState below = level.getBlockState(pos.below());

        boolean validSoil = below.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
                below.is(net.minecraft.world.level.block.Blocks.DIRT) ||
                below.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT) ||
                below.is(net.minecraft.world.level.block.Blocks.PODZOL) ||
                below.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT) ||
                below.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK);

        if (!validSoil) return false;

        int spacing = Config.PINECONE_TREE_SPACING.get();
        if (spacing > 0) {
            BlockPos nearestLog = TreeUtils.findNearestLog(level, pos, spacing);
            if (nearestLog != null) return false;

            for (int dx = -spacing; dx <= spacing; dx++) {
                for (int dy = -spacing; dy <= spacing; dy++) {
                    for (int dz = -spacing; dz <= spacing; dz++) {
                        BlockPos checkPos = pos.offset(dx, dy, dz);
                        if (level.getBlockState(checkPos).getBlock() instanceof net.minecraft.world.level.block.SaplingBlock) {
                            return false;
                        }
                    }
                }
            }

            if (hasNearbyPinecones(pos, spacing)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if there are other pinecones on the ground within the specified distance.
     */
    private boolean hasNearbyPinecones(BlockPos pos, int distance) {
        AABB searchBox = new AABB(pos).inflate(distance);
        List<ItemEntity> nearbyPinecones = bee.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item.getItem().is(ModItems.PINECONE.get()) && item.isAlive() && item.onGround());

        for (ItemEntity pinecone : nearbyPinecones) {
            double dist = pinecone.blockPosition().distSqr(pos);
            if (dist < 1) continue;
            if (dist <= distance * distance) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findDropLocation(BlockPos pickupPos) {
        Level level = bee.level();
        int minDist = Config.BEE_MIN_PINECONE_DROP_DISTANCE.get();
        int searchRadius = Config.BEE_PINECONE_SEARCH_RADIUS.get();
        int spacing = Config.PINECONE_TREE_SPACING.get();

        for (int attempts = 0; attempts < 10; attempts++) {
            int dx = level.random.nextInt(searchRadius * 2) - searchRadius;
            int dz = level.random.nextInt(searchRadius * 2) - searchRadius;

            BlockPos candidate = pickupPos.offset(dx, 0, dz);

            candidate = findGround(level, candidate);
            if (candidate == null) continue;

            if (candidate.distManhattan(pickupPos) < minDist) continue;

            BlockPos nearestLog = TreeUtils.findNearestLog(level, candidate, minDist);
            if (nearestLog != null && candidate.distManhattan(nearestLog) < minDist) continue;

            if (spacing > 0 && hasNearbyPinecones(candidate, spacing)) continue;

            return candidate;
        }

        return null;
    }

    @Nullable
    private BlockPos findGround(Level level, BlockPos pos) {
        for (int y = pos.getY() + 10; y > level.getMinY(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.above()).isAir()) {
                return checkPos.above();
            }
        }
        return null;
    }

    private boolean isWithinNestRadius() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos == null) return true;

        double dist = bee.blockPosition().distSqr(hivePos);
        int radius = Config.BEE_NEST_RADIUS.get();
        return dist <= radius * radius;
    }
}
