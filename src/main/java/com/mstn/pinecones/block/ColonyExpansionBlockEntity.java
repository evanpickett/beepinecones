package com.mstn.pinecones.block;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.init.ModAttachments;
import com.mstn.pinecones.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity for colony expansion that handles hostile mob detection
 * and defensive bee spawning.
 */
public class ColonyExpansionBlockEntity extends BlockEntity {
    private static final int SPAWN_COOLDOWN_TICKS = 60;
    private static final float DEFENDER_BEE_SCALE = 0.5f;
    private static final Identifier DEFENDER_SCALE_ID = Identifier.parse("pinecones:defender_bee_scale");

    private final Set<UUID> spawnedBeeIds = new HashSet<>();
    private int tickCounter = 0;
    private int lastSpawnTick = -SPAWN_COOLDOWN_TICKS;

    private UUID aggroTarget = null;
    private long aggroStartTime = 0;

    public ColonyExpansionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLONY_EXPANSION.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ColonyExpansionBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.tickCounter++;

        blockEntity.cleanupDeadBees((ServerLevel) level);

        int checkInterval = Config.COLONY_EXPANSION_HOSTILE_CHECK_INTERVAL.get();
        if (blockEntity.tickCounter % checkInterval == 0) {
            blockEntity.checkForHostiles((ServerLevel) level, pos);
        }

        if (blockEntity.aggroTarget != null) {
            long aggroDuration = Config.COLONY_EXPANSION_AGGRO_DURATION.get();
            if (level.getGameTime() - blockEntity.aggroStartTime > aggroDuration) {
                blockEntity.aggroTarget = null;
            } else {
                blockEntity.checkForAggressor((ServerLevel) level, pos);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ColonyExpansionBlockEntity blockEntity) {
        if (level.random.nextInt(20) == 0) {
            double x = pos.getX() + 0.3 + level.random.nextDouble() * 0.4;
            double y = pos.getY() + 0.67;
            double z = pos.getZ() + 0.3 + level.random.nextDouble() * 0.4;
            level.addParticle(ParticleTypes.DRIPPING_HONEY, x, y, z, 0, 0, 0);
        }
    }

    private void cleanupDeadBees(ServerLevel level) {
        Iterator<UUID> iterator = spawnedBeeIds.iterator();
        while (iterator.hasNext()) {
            UUID beeId = iterator.next();
            Entity entity = level.getEntity(beeId);
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
            }
        }
    }

    private void checkForHostiles(ServerLevel level, BlockPos pos) {
        int radius = Config.COLONY_EXPANSION_HOSTILE_CHECK_RADIUS.get();
        AABB searchBox = new AABB(pos).inflate(radius);

        List<Mob> hostiles = level.getEntitiesOfClass(Mob.class, searchBox,
                mob -> mob.getType().getCategory() == MobCategory.MONSTER
                        && mob.isAlive()
                        && !(mob instanceof Creeper));

        if (!hostiles.isEmpty()) {
            Mob target = findUntargetedHostile(level, hostiles);
            if (target != null) {
                spawnDefenderBees(level, pos, target);
            }
        }
    }

    private Mob findUntargetedHostile(ServerLevel level, List<Mob> hostiles) {
        for (Mob hostile : hostiles) {
            boolean isTargeted = false;
            for (UUID beeId : spawnedBeeIds) {
                Entity entity = level.getEntity(beeId);
                if (entity instanceof Bee bee && bee.getTarget() == hostile) {
                    isTargeted = true;
                    break;
                }
            }
            if (!isTargeted) {
                return hostile;
            }
        }
        return null;
    }

    /**
     * Spawns defender bees to attack a target.
     * Called when hostiles are detected or a nearby bee is hurt.
     */
    public void spawnDefenderBees(ServerLevel level, BlockPos pos, LivingEntity target) {
        if (target instanceof Creeper) return;

        if (tickCounter - lastSpawnTick < SPAWN_COOLDOWN_TICKS) return;

        int maxBees = Config.COLONY_EXPANSION_MAX_SPAWNED_BEES.get();
        int spawnCount = Config.COLONY_EXPANSION_SPAWN_COUNT.get();

        int currentCount = spawnedBeeIds.size();
        int canSpawn = Math.min(spawnCount, maxBees - currentCount);

        if (canSpawn <= 0) return;

        lastSpawnTick = tickCounter;

        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                canSpawn * 2, 0.3, 0.3, 0.3, 0.0);

        for (int i = 0; i < canSpawn; i++) {
            Bee bee = new Bee(net.minecraft.world.entity.EntityType.BEE, level);

            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
            double y = pos.getY() - 0.3;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
            bee.setPos(x, y, z);

            bee.setTarget(target);
            bee.startPersistentAngerTimer();

            bee.setData(ModAttachments.DEFENDER_BEE_DATA.get(), DefenderBeeData.create(pos));

            AttributeInstance scaleAttr = bee.getAttribute(Attributes.SCALE);
            if (scaleAttr != null) {
                scaleAttr.addPermanentModifier(new AttributeModifier(
                        DEFENDER_SCALE_ID,
                        DEFENDER_BEE_SCALE - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }

            level.addFreshEntity(bee);
            spawnedBeeIds.add(bee.getUUID());

            level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.NEUTRAL, 1.0F, 1.2F);
        }

        setChanged();
    }

    /**
     * Called when a bee is hurt near this expansion.
     */
    public void onNearbyBeeHurt(ServerLevel level, LivingEntity attacker) {
        if (attacker != null && attacker.isAlive() && !(attacker instanceof Creeper)) {
            spawnDefenderBees(level, worldPosition, attacker);
        }
    }

    /**
     * Sets the expansion into aggro state against a player.
     * While in aggro state, if the player comes near, defenders will spawn.
     */
    public void setAggro(LivingEntity target, long gameTime) {
        if (target != null) {
            this.aggroTarget = target.getUUID();
            this.aggroStartTime = gameTime;
            setChanged();
        }
    }

    /**
     * Checks if the aggro target is nearby and spawns defenders if so.
     */
    private void checkForAggressor(ServerLevel level, BlockPos pos) {
        if (aggroTarget == null) return;

        int radius = Config.COLONY_EXPANSION_AGGRO_RADIUS.get();
        Entity target = level.getEntity(aggroTarget);

        if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
            double distSq = target.blockPosition().distSqr(pos);
            if (distSq <= radius * radius) {
                spawnDefenderBees(level, pos, livingTarget);
            }
        }
    }

    /**
     * Returns true if the expansion is currently in aggro state.
     */
    public boolean isAggro() {
        return aggroTarget != null;
    }

    /**
     * Gets the current count of alive spawned bees.
     */
    public int getSpawnedBeeCount() {
        return spawnedBeeIds.size();
    }
}
