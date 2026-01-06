package com.mstn.pinecones.mixin;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.data.NestData;
import com.mstn.pinecones.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin for bee behavior modifications:
 * - Enable love mode after pollinating when population is low
 * Note: Defender bee aggro prevention is handled via NeoForge events
 */
@Mixin(Bee.class)
public abstract class BeeMixin {

    private static final int LOVE_MODE_CHECK_RADIUS = 32;

    /**
     * When a bee finishes pollinating (setHasNectar called with true),
     * check if it should enter love mode based on population.
     */
    @Inject(method = "setHasNectar", at = @At("TAIL"))
    private void pinecones$checkLoveModeAfterPollinating(boolean hasNectar, CallbackInfo ci) {
        if (!hasNectar) return;

        Bee self = (Bee)(Object)this;
        if (!(self.level() instanceof ServerLevel level)) return;

        if (self.isBaby()) return;

        DefenderBeeData defenderData = self.getData(ModAttachments.DEFENDER_BEE_DATA.get());
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
            DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
            return !data.isDefender();
        });
        return bees.size();
    }
}
