package com.mstn.pinecones.ai;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.entity.PollenEntity;
import com.mstn.pinecones.init.ModAttachments;
import com.mstn.pinecones.init.ModDataComponents;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal for bees to drop pollen when they pollinate flowers.
 * When a bee has nectar (just pollinated), it drops a pollen item near the flower.
 */
public class BeeCollectPollenGoal extends Goal {

    private final Bee bee;
    private BlockPos targetFlower;
    private int collectingTicks;
    private boolean hasDroppedPollen;

    public BeeCollectPollenGoal(Bee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only trigger when bee has nectar (just pollinated)
        if (!bee.hasNectar()) {
            // Reset the flag when bee no longer has nectar (deposited in hive)
            BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());
            if (data.hasDroppedPollenThisCycle()) {
                bee.setData(ModAttachments.BEE_CARRY_DATA.get(), data.withDroppedPollen(false));
            }
            return false;
        }

        // Don't drop pollen again if we already did this nectar cycle
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());
        if (data.hasDroppedPollenThisCycle()) {
            return false;
        }

        // Check if within nest radius
        if (!isWithinNestRadius()) {
            return false;
        }

        // Find nearby flower to drop pollen at
        targetFlower = findNearbyFlower();
        return targetFlower != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetFlower == null) return false;

        // Stop if we've dropped pollen
        if (hasDroppedPollen) {
            return false;
        }

        // Stop if flower is gone
        return bee.level().getBlockState(targetFlower).is(ModTags.Blocks.FLOWERS);
    }

    @Override
    public void start() {
        collectingTicks = 0;
        hasDroppedPollen = false;
    }

    @Override
    public void stop() {
        targetFlower = null;
        hasDroppedPollen = false;
    }

    @Override
    public void tick() {
        if (targetFlower == null || hasDroppedPollen) return;

        // Move towards flower
        bee.getNavigation().moveTo(
                targetFlower.getX() + 0.5,
                targetFlower.getY() + 0.5,
                targetFlower.getZ() + 0.5,
                1.0
        );

        // Check if close enough to drop pollen
        double dist = bee.position().distanceToSqr(
                targetFlower.getX() + 0.5,
                targetFlower.getY() + 0.5,
                targetFlower.getZ() + 0.5
        );

        if (dist < 2.0) {
            collectingTicks++;

            // Drop pollen after a short delay
            if (collectingTicks > 30) {
                dropPollen();
            }
        }
    }

    private void dropPollen() {
        if (targetFlower == null) return;

        Level level = bee.level();
        BlockState flowerState = level.getBlockState(targetFlower);

        if (!flowerState.is(ModTags.Blocks.FLOWERS)) return;

        // Create pollen with flower data
        Identifier flowerId = BuiltInRegistries.BLOCK.getKey(flowerState.getBlock());
        ItemStack pollen = new ItemStack(ModItems.POLLEN.get());
        pollen.set(ModDataComponents.FLOWER_DATA.get(), new FlowerData(flowerId));

        // Calculate random direction to drop pollen
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = 1.0 + level.random.nextDouble() * 2.0; // 1-3 blocks away
        double dropX = targetFlower.getX() + 0.5 + Math.cos(angle) * distance;
        double dropZ = targetFlower.getZ() + 0.5 + Math.sin(angle) * distance;
        double dropY = targetFlower.getY() + 0.5;

        // Create and spawn pollen entity
        PollenEntity pollenEntity = new PollenEntity(level, dropX, dropY, dropZ, pollen);

        // Give it some velocity in the random direction
        pollenEntity.setDeltaMovement(
                Math.cos(angle) * 0.2,
                0.1,
                Math.sin(angle) * 0.2
        );

        level.addFreshEntity(pollenEntity);
        hasDroppedPollen = true;

        // Mark on bee data so we don't drop again this nectar cycle
        BeeCarryData data = bee.getData(ModAttachments.BEE_CARRY_DATA.get());
        bee.setData(ModAttachments.BEE_CARRY_DATA.get(), data.withDroppedPollen(true));
    }

    @Nullable
    private BlockPos findNearbyFlower() {
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

                    if (state.is(ModTags.Blocks.FLOWERS)) {
                        double dist = beePos.distSqr(checkPos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = checkPos;
                        }
                    }
                }
            }
        }

        return closest;
    }

    private boolean isWithinNestRadius() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos == null) return true;

        double dist = bee.blockPosition().distSqr(hivePos);
        int radius = Config.BEE_NEST_RADIUS.get();
        return dist <= radius * radius;
    }
}
