package com.mstn.pinecones.mixin;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.data.NestData;
import com.mstn.pinecones.entity.PollenEntity;
import com.mstn.pinecones.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Mixin for bee behavior modifications:
 * - Enable love mode after pollinating when population is low
 * - Drop pollen when bee finishes pollinating
 */
@Mixin(Bee.class)
public abstract class BeeMixin {

    @Shadow @Nullable public abstract BlockPos getSavedFlowerPos();

    private static final int LOVE_MODE_CHECK_RADIUS = 32;

    /**
     * When a bee finishes pollinating (setHasNectar called with true),
     * drop pollen and check if it should enter love mode based on population.
     */
    @Inject(method = "setHasNectar", at = @At("TAIL"))
    private void pinecones$onPollinationComplete(boolean hasNectar, CallbackInfo ci) {
        if (!hasNectar) return;

        Bee self = (Bee)(Object)this;
        if (!(self.level() instanceof ServerLevel level)) return;

        // Drop pollen near the flower that was just pollinated
        dropPollenNearFlower(self, level);

        if (self.isBaby()) return;

        // Check defender status from persistent data
        DefenderBeeData defenderData = DefenderBeeData.loadFromEntity(self);
        if (defenderData.isDefender()) return;

        BlockPos beePos = self.blockPosition();

        NestData nestData = countNearbyNestsAndOccupants(level, beePos);
        if (nestData.nestCount() == 0) return;

        int freeBeeCount = countNearbyFreeBees(level, beePos, self);

        int totalBeeCount = nestData.beesInNests() + freeBeeCount;

        int beesPerNest = Config.BEES_PER_NEST.get();
        int maxPopulation = Config.BEE_MAX_POPULATION.get();

        int maxBees = Math.min(nestData.nestCount() * beesPerNest, maxPopulation);

        if (totalBeeCount < maxBees) {
            self.setInLove(null);
        }
    }

    /**
     * Drops pollen near the flower that was just pollinated.
     */
    private void dropPollenNearFlower(Bee bee, ServerLevel level) {
        BlockPos flowerPos = getSavedFlowerPos();
        if (flowerPos == null) return;

        BlockState flowerState = level.getBlockState(flowerPos);
        if (flowerState.isAir()) return;

        // Create pollen with flower data
        ResourceLocation flowerId = BuiltInRegistries.BLOCK.getKey(flowerState.getBlock());
        ItemStack pollen = new ItemStack(ModItems.POLLEN.get());
        FlowerData.saveToStack(pollen, new FlowerData(flowerId));

        // Calculate random position further from the flower to prevent item merging
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = 4.0 + level.random.nextDouble() * 4.0; // 4-8 blocks away
        double dropX = flowerPos.getX() + 0.5 + Math.cos(angle) * distance;
        double dropZ = flowerPos.getZ() + 0.5 + Math.sin(angle) * distance;
        double dropY = flowerPos.getY() + 0.5;

        // Create and spawn pollen entity
        PollenEntity pollenEntity = new PollenEntity(level, dropX, dropY, dropZ, pollen);
        pollenEntity.setDeltaMovement(
                Math.cos(angle) * 0.3,
                0.15,
                Math.sin(angle) * 0.3
        );

        level.addFreshEntity(pollenEntity);
    }

    /**
     * Counts nearby bee nests and the total number of bees inside them.
     */
    private static NestData countNearbyNestsAndOccupants(ServerLevel level, BlockPos center) {
        int nestCount = 0;
        int beesInNests = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -LOVE_MODE_CHECK_RADIUS; dx <= LOVE_MODE_CHECK_RADIUS; dx++) {
            for (int dy = -LOVE_MODE_CHECK_RADIUS; dy <= LOVE_MODE_CHECK_RADIUS; dy++) {
                for (int dz = -LOVE_MODE_CHECK_RADIUS; dz <= LOVE_MODE_CHECK_RADIUS; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState blockState = level.getBlockState(mutable);
                    if (blockState.getBlock() instanceof BeehiveBlock) {
                        nestCount++;
                        BlockEntity blockEntity = level.getBlockEntity(mutable);
                        if (blockEntity instanceof BeehiveBlockEntity beehive) {
                            beesInNests += beehive.getOccupantCount();
                        }
                    }
                }
            }
        }
        return new NestData(nestCount, beesInNests);
    }

    /**
     * Counts nearby non-defender bees that are free in the world (including babies).
     * Baby bees count toward population but can't enter love mode themselves.
     */
    private static int countNearbyFreeBees(ServerLevel level, BlockPos center, Bee excludeBee) {
        AABB searchBox = new AABB(center).inflate(LOVE_MODE_CHECK_RADIUS);
        List<Bee> bees = level.getEntitiesOfClass(Bee.class, searchBox, bee -> {
            if (bee == excludeBee) return false;
            DefenderBeeData data = DefenderBeeData.loadFromEntity(bee);
            return !data.isDefender();
        });
        return bees.size();
    }
}
