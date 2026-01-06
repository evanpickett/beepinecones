package com.mstn.pinecones.event;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.block.ColonyExpansionBlockEntity;
import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.init.ModAttachments;
import com.mstn.pinecones.init.ModBlocks;
import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles events related to colony expansion blocks.
 */
@EventBusSubscriber(modid = pinecones.MODID)
public class ColonyExpansionHandler {

    private static final Map<UUID, Long> recentlyHurtDefenders = new ConcurrentHashMap<>();
    private static final long AGGRO_SPREAD_WINDOW_TICKS = 5;
    private static final double AGGRO_SPREAD_RADIUS = 16.0;

    /**
     * When a bee is hurt, check if there's a colony expansion nearby and spawn defender bees.
     */
    @SubscribeEvent
    public static void onBeeHurt(LivingDamageEvent.Post event) {
        if (!Config.COLONY_EXPANSION_ENABLED.get()) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Bee bee)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        DefenderBeeData defenderData = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        if (defenderData.isDefender()) {
            recentlyHurtDefenders.put(bee.getUUID(), level.getGameTime());
            long currentTime = level.getGameTime();
            recentlyHurtDefenders.entrySet().removeIf(e -> currentTime - e.getValue() > AGGRO_SPREAD_WINDOW_TICKS * 2);
        }

        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker == null) return;

        if (attacker instanceof Bee) return;

        BlockPos beePos = bee.blockPosition();
        int searchRadius = Config.COLONY_EXPANSION_HOSTILE_CHECK_RADIUS.get();

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos checkPos = beePos.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);
                    if (blockEntity instanceof ColonyExpansionBlockEntity expansion) {
                        expansion.onNearbyBeeHurt(level, attacker);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Prevent normal bees from becoming aggressive when a defender bee is hurt.
     * When a defender bee is attacked, the vanilla alertOthers() would normally
     * make nearby bees angry at the attacker. We intercept this by canceling
     * target changes for regular bees that happen right after a defender is hurt.
     */
    @SubscribeEvent
    public static void onBeeTargetChange(LivingChangeTargetEvent event) {
        if (!Config.COLONY_EXPANSION_ENABLED.get()) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Bee bee)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        if (data.isDefender()) return;

        long currentTime = level.getGameTime();
        BlockPos beePos = bee.blockPosition();

        for (Map.Entry<UUID, Long> entry : recentlyHurtDefenders.entrySet()) {
            if (currentTime - entry.getValue() > AGGRO_SPREAD_WINDOW_TICKS) continue;

            net.minecraft.world.entity.Entity defenderEntity = level.getEntity(entry.getKey());
            if (defenderEntity instanceof Bee defenderBee && defenderBee.isAlive()) {
                double distSq = beePos.distSqr(defenderBee.blockPosition());
                if (distSq <= AGGRO_SPREAD_RADIUS * AGGRO_SPREAD_RADIUS) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    /**
     * Make defender bees drop experience when killed by a player.
     * Normal bees don't drop XP, but defender bees do as a reward for defeating them.
     */
    @SubscribeEvent
    public static void onDefenderBeeExperienceDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Bee bee)) return;

        DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        if (data.isDefender()) {
            event.setDroppedExperience(3 + bee.level().random.nextInt(3));
        }
    }

    /**
     * Apply slowness effect when a defender bee stings a target.
     */
    @SubscribeEvent
    public static void onDefenderBeeAttack(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Bee bee)) return;

        DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        if (!data.isDefender()) return;

        LivingEntity target = event.getEntity();
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
    }

    /**
     * When a player uses shears or bottles on a beehive, check if there's a
     * colony expansion below and spawn defender bees to protect the hive.
     */
    @SubscribeEvent
    public static void onHiveInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.COLONY_EXPANSION_ENABLED.get()) return;

        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos hivePos = event.getPos();
        BlockState hiveState = level.getBlockState(hivePos);

        if (!(hiveState.getBlock() instanceof BeehiveBlock)) return;

        ItemStack heldItem = event.getItemStack();
        boolean isShears = heldItem.is(Items.SHEARS);
        boolean isBottle = heldItem.is(Items.GLASS_BOTTLE);

        if (!isShears && !isBottle) return;

        BlockPos belowPos = hivePos.below();
        BlockEntity blockEntity = level.getBlockEntity(belowPos);

        if (blockEntity instanceof ColonyExpansionBlockEntity expansion) {
            Player player = event.getEntity();
            ServerLevel serverLevel = (ServerLevel) level;
            expansion.spawnDefenderBees(serverLevel, belowPos, player);
        }
    }

    /**
     * Checks if a colony expansion should form below a bee nest.
     * Called when a bee leaves the nest.
     */
    public static void checkExpansionFormation(Level level, BlockPos nestPos) {
        if (level == null || nestPos == null) return;
        if (level.isClientSide()) return;

        try {
            if (!Config.COLONY_EXPANSION_ENABLED.get()) return;
        } catch (Exception e) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockState nestState = level.getBlockState(nestPos);

        if (!(nestState.getBlock() instanceof BeehiveBlock)) return;

        BlockPos belowPos = nestPos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (!belowState.isAir()) return;

        int flowerCount = countNearbyFlowers(level, nestPos);
        int requiredFlowers = Config.COLONY_EXPANSION_FLOWER_COUNT.get();

        if (flowerCount >= requiredFlowers) {
            try {
                BlockState expansionState = ModBlocks.COLONY_EXPANSION.get().defaultBlockState();

                if (nestState.hasProperty(BeehiveBlock.FACING)) {
                    expansionState = expansionState.setValue(
                            com.mstn.pinecones.block.ColonyExpansionBlock.FACING,
                            nestState.getValue(BeehiveBlock.FACING)
                    );
                }

                level.setBlock(belowPos, expansionState, Block.UPDATE_ALL);
                pinecones.LOGGER.info("Colony expansion formed at {} (found {} flowers)", belowPos, flowerCount);
            } catch (Exception e) {
                pinecones.LOGGER.error("Failed to create colony expansion at {}: {}", belowPos, e.getMessage());
            }
        }
    }

    /**
     * Counts flowers within the configured radius of a position.
     */
    private static int countNearbyFlowers(Level level, BlockPos center) {
        try {
            int radius = Config.COLONY_EXPANSION_FLOWER_RADIUS.get();
            int count = 0;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos checkPos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(checkPos);
                        if (state.is(ModTags.Blocks.FLOWERS)) {
                            count++;
                        }
                    }
                }
            }

            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Handles defender bee behavior after their target is killed.
     * Called when a bee's target dies.
     */
    public static void onDefenderBeeTargetKilled(Bee bee, ServerLevel level) {
        DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        if (!data.isDefender()) return;

        BlockPos expansionPos = data.homeExpansion().orElse(bee.blockPosition());

        bee.setTarget(null);
        bee.stopBeingAngry();

        if (bee.blockPosition().distSqr(expansionPos) < 4) {
            bee.discard();
        } else {
            bee.getNavigation().moveTo(expansionPos.getX() + 0.5, expansionPos.getY() + 0.5, expansionPos.getZ() + 0.5, 1.0);
        }
    }
}
