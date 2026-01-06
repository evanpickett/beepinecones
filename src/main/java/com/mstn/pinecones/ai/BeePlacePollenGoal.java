package com.mstn.pinecones.ai;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.entity.PollenEntity;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal for bees to use pollen: apply to crops (instant grow) or drop on ground to plant flowers.
 */
public class BeePlacePollenGoal extends Goal {

    private final Bee bee;
    private BlockPos target;
    private TargetType targetType;
    private int ticksCarrying;

    private enum TargetType {
        CROP,       // Apply pollen to grow crop instantly
        DROP        // Drop pollen for flower planting
    }

    public BeePlacePollenGoal(Bee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BeeCarryData data = BeeCarryData.loadFromEntity(bee);

        if (!data.isCarryingItem() || !data.carriedItem().is(ModItems.POLLEN.get())) {
            return false;
        }

        if (isWithinNestRadius()) {
            target = findCrop();
            if (target != null) {
                targetType = TargetType.CROP;
                return true;
            }

            target = findDropLocation();
            if (target != null) {
                targetType = TargetType.DROP;
                return true;
            }
        }

        target = bee.blockPosition();
        targetType = TargetType.DROP;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        if (!data.isCarryingItem()) {
            return false;
        }

        return target != null;
    }

    @Override
    public void start() {
        ticksCarrying = 0;
    }

    @Override
    public void stop() {
        target = null;
        targetType = null;
    }

    @Override
    public void tick() {
        if (target == null) return;

        ticksCarrying++;

        bee.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY() + 1,
                target.getZ() + 0.5,
                1.0
        );

        double dist = bee.position().distanceToSqr(Vec3.atCenterOf(target));

        if (dist < 2.0) {
            if (targetType == TargetType.CROP) {
                applyCropGrowth();
            } else {
                dropPollen();
            }
        }

        if (ticksCarrying > Config.BEE_PINECONE_DROP_TIMEOUT_TICKS.get()) {
            dropPollen();
        }
    }

    private void applyCropGrowth() {
        if (target == null) return;

        Level level = bee.level();
        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        BlockState state = level.getBlockState(target);

        if (state.getBlock() instanceof BonemealableBlock growable) {
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 5; i++) {
                    if (growable.isValidBonemealTarget(level, target, state, false)) {
                        growable.performBonemeal(serverLevel, level.random, target, state);
                        state = level.getBlockState(target);
                    }
                }
            }
        }

        BeeCarryData.saveToEntity(bee, data.clearCarriedItem());
        target = null;
    }

    private void dropPollen() {
        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        if (!data.isCarryingItem()) return;

        Level level = bee.level();
        BlockPos dropPos = bee.blockPosition();

        PollenEntity pollen = new PollenEntity(
                level,
                dropPos.getX() + 0.5,
                dropPos.getY() + 0.5,
                dropPos.getZ() + 0.5,
                data.carriedItem().copy()
        );
        level.addFreshEntity(pollen);

        BeeCarryData.saveToEntity(bee, data.clearCarriedItem());
        target = null;
    }

    @Nullable
    private BlockPos findCrop() {
        Level level = bee.level();
        BlockPos beePos = bee.blockPosition();
        int radius = Config.BEE_POLLEN_SEARCH_RADIUS.get();

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius / 2; dy <= radius / 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = beePos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.getBlock() instanceof CropBlock crop) {
                        if (!crop.isMaxAge(state)) {
                            double dist = beePos.distSqr(checkPos);
                            if (dist < closestDist) {
                                closestDist = dist;
                                closest = checkPos;
                            }
                        }
                    }
                }
            }
        }

        return closest;
    }

    @Nullable
    private BlockPos findDropLocation() {
        Level level = bee.level();
        BlockPos beePos = bee.blockPosition();
        int radius = Config.BEE_POLLEN_SEARCH_RADIUS.get();
        int spacing = Config.PINECONE_TREE_SPACING.get();

        for (int attempts = 0; attempts < 10; attempts++) {
            int dx = level.random.nextInt(radius * 2) - radius;
            int dz = level.random.nextInt(radius * 2) - radius;

            BlockPos candidate = beePos.offset(dx, 0, dz);

            candidate = findGround(level, candidate);
            if (candidate == null) continue;

            BlockState below = level.getBlockState(candidate.below());
            if (!below.is(ModTags.Blocks.FLOWER_PLANTABLE) || !level.getBlockState(candidate).isAir()) {
                continue;
            }

            if (hasNearbyFlower(candidate, 2)) {
                continue;
            }

            if (spacing > 0 && hasNearbyPollen(candidate, spacing)) {
                continue;
            }

            return candidate;
        }

        return beePos;
    }

    /**
     * Checks if there is a flower within the specified distance.
     */
    private boolean hasNearbyFlower(BlockPos pos, int distance) {
        if (pos == null) return false;
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
        if (pos == null) return false;
        AABB searchBox = new AABB(pos).inflate(distance);
        List<ItemEntity> nearbyPollen = bee.level().getEntitiesOfClass(ItemEntity.class, searchBox,
                item -> item.getItem().is(ModItems.POLLEN.get()) && item.isAlive() && item.onGround());

        for (ItemEntity pollen : nearbyPollen) {
            double dist = pollen.blockPosition().distSqr(pos);
            if (dist <= distance * distance) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findGround(Level level, BlockPos pos) {
        for (int y = pos.getY() + 10; y > level.getMinBuildHeight(); y--) {
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

        BlockPos beePos = bee.blockPosition();
        if (beePos == null) return true;
        double dist = beePos.distSqr(hivePos);
        int radius = Config.BEE_NEST_RADIUS.get();
        return dist <= radius * radius;
    }
}
