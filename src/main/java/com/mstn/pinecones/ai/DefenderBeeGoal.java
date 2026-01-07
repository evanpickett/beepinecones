package com.mstn.pinecones.ai;

import com.mstn.pinecones.data.DefenderBeeData;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;

import java.util.EnumSet;

/**
 * AI goal for defender bees that handles:
 * - Bleeding out after stinging (lost stinger)
 * - Preventing pollen collection and hive entry
 * - Returning home and despawning when no target
 * - Giving up on unreachable targets
 */
public class DefenderBeeGoal extends Goal {

    private static final int BLEED_DAMAGE_INTERVAL = 20;
    private static final float BLEED_DAMAGE_AMOUNT = 2.0f;
    private static final int DESPAWN_TIMEOUT = 2400; // 2 minutes without target = despawn
    private static final int PATH_FAIL_TIMEOUT = 100; // 5 seconds of failed pathing = give up
    private static final double PATH_PROGRESS_THRESHOLD = 1.0; // Must get at least 1 block closer

    private final Bee bee;
    private int ticksWithoutTarget = 0;
    private int ticksSinceStung = 0;
    private boolean hasStungTracked = false;
    private int ticksFailedPathing = 0;
    private double lastDistanceToTarget = Double.MAX_VALUE;

    public DefenderBeeGoal(Bee bee) {
        this.bee = bee;
        // Use MOVE and LOOK flags to prevent vanilla goals from running (including FollowParentGoal for babies)
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        DefenderBeeData data = DefenderBeeData.loadFromEntity(bee);
        return data.isDefender();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        DefenderBeeData data = DefenderBeeData.loadFromEntity(bee);
        if (!data.isDefender()) return;

        if (bee.hasStung()) {
            if (!hasStungTracked) {
                hasStungTracked = true;
                ticksSinceStung = 0;
            }
            ticksSinceStung++;

            if (ticksSinceStung % BLEED_DAMAGE_INTERVAL == 0) {
                DamageSource bleedDamage = bee.damageSources().genericKill();
                bee.hurt(bleedDamage, BLEED_DAMAGE_AMOUNT);
            }
            return;
        }

        // Prevent defender bees from collecting nectar or pollinating
        if (bee.hasNectar()) {
            bee.dropOffNectar();
        }
        bee.setSavedFlowerPos(null);

        // Reset stay-out-of-hive timer to prevent hive entry
        // The MOVE and LOOK flags should prevent BeeGoToHiveGoal from running
        bee.setStayOutOfHiveCountdown(400);

        // Check if we have a target but can't reach it
        LivingEntity target = bee.getTarget();
        if (target != null && target.isAlive()) {
            double currentDistance = bee.distanceTo(target);

            // Actively navigate toward target (prevents baby follow behavior from taking over)
            if (bee.tickCount % 10 == 0) {
                bee.getNavigation().moveTo(target, 1.0);
                bee.getLookControl().setLookAt(target);
            }

            // Check if we're making progress toward the target
            if (currentDistance < lastDistanceToTarget - PATH_PROGRESS_THRESHOLD) {
                // Making progress, reset fail counter
                ticksFailedPathing = 0;
                lastDistanceToTarget = currentDistance;
            } else {
                // Not making progress
                ticksFailedPathing++;

                // Give up if we've been stuck too long
                if (ticksFailedPathing > PATH_FAIL_TIMEOUT) {
                    bee.setTarget(null);
                    bee.stopBeingAngry();
                    ticksFailedPathing = 0;
                    lastDistanceToTarget = Double.MAX_VALUE;
                }
            }
            ticksWithoutTarget = 0;
        } else if (bee.isAngry()) {
            // Angry but no valid target - clear anger
            bee.stopBeingAngry();
            ticksWithoutTarget = 0;
            ticksFailedPathing = 0;
            lastDistanceToTarget = Double.MAX_VALUE;
        } else {
            // No target and not angry - return home
            ticksWithoutTarget++;
            ticksFailedPathing = 0;
            lastDistanceToTarget = Double.MAX_VALUE;

            // Despawn if no target for too long
            if (ticksWithoutTarget > DESPAWN_TIMEOUT) {
                bee.discard();
                return;
            }

            // Try to return home if we have a home expansion
            if (data.homeExpansion().isPresent()) {
                BlockPos homePos = data.homeExpansion().get();
                if (homePos != null) {
                    BlockPos beePos = bee.blockPosition();
                    if (beePos != null) {
                        double distSq = beePos.distSqr(homePos);

                        if (distSq < 4) {
                            bee.level().playSound(null, bee.getX(), bee.getY(), bee.getZ(),
                                    SoundEvents.BEEHIVE_ENTER, SoundSource.NEUTRAL, 1.0F, 1.0F);
                            bee.discard();
                            return;
                        } else {
                            if (ticksWithoutTarget == 1 || ticksWithoutTarget % 40 == 0) {
                                bee.getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY() + 0.5, homePos.getZ() + 0.5, 1.0);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
