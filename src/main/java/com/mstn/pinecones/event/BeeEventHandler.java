package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.ai.BeeCarryPineconeGoal;
import com.mstn.pinecones.ai.BeeCarryPollenGoal;
import com.mstn.pinecones.ai.BeeCollectPollenGoal;
import com.mstn.pinecones.ai.BeePlacePollenGoal;
import com.mstn.pinecones.ai.DefenderBeeGoal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Handles injecting custom AI goals into bees.
 */
@EventBusSubscriber(modid = pinecones.MODID)
public class BeeEventHandler {

    /**
     * Inject custom goals into bees when they spawn.
     *
     * Vanilla bee goal priorities:
     * - 0: Float
     * - 1-3: Combat, breeding, tempt
     * - 4: BeePollinateGoal (pollinate flowers)
     * - 5: BeeGoToHiveGoal (return to hive when has nectar)
     * - 6: BeeGoToKnownFlowerGoal
     * - 7: BeeGrowCropGoal
     * - 8: BeeWanderGoal
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Bee bee) {
            bee.goalSelector.addGoal(0, new DefenderBeeGoal(bee));
            bee.goalSelector.addGoal(4, new BeeCollectPollenGoal(bee));
            bee.goalSelector.addGoal(6, new BeeCarryPineconeGoal(bee));
            bee.goalSelector.addGoal(6, new BeeCarryPollenGoal(bee));
            bee.goalSelector.addGoal(6, new BeePlacePollenGoal(bee));
        }
    }
}
