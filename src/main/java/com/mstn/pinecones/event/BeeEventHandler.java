package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.ai.BeeCarryPineconeGoal;
import com.mstn.pinecones.ai.BeeCarryPollenGoal;
import com.mstn.pinecones.ai.BeePlacePollenGoal;
import com.mstn.pinecones.ai.DefenderBeeGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles injecting custom AI goals into bees.
 */
@Mod.EventBusSubscriber(modid = pinecones.MODID)
public class BeeEventHandler {

    // Track which bees have had goals injected to prevent duplicates
    private static final Set<UUID> injectedBees = new HashSet<>();

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
     *
     * Our goals use priority 5 for carrying (competes with going home)
     * and priority 4 for placing (higher priority when carrying).
     *
     * Note: Pollen dropping is handled via BeeMixin when bee finishes pollinating.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Bee bee) {
            // Prevent duplicate goal injection
            if (injectedBees.contains(bee.getUUID())) {
                return;
            }
            injectedBees.add(bee.getUUID());

            // Clean up old entries periodically (when set gets large)
            if (injectedBees.size() > 1000) {
                injectedBees.clear();
                injectedBees.add(bee.getUUID());
            }

            bee.goalSelector.addGoal(0, new DefenderBeeGoal(bee));
            // Priority 5: Carry goals compete with BeeGoToHiveGoal
            bee.goalSelector.addGoal(5, new BeeCarryPineconeGoal(bee));
            bee.goalSelector.addGoal(5, new BeeCarryPollenGoal(bee));
            // Priority 4: Place pollen has higher priority when bee is carrying
            bee.goalSelector.addGoal(4, new BeePlacePollenGoal(bee));
        }
    }
}
