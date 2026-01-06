package com.mstn.pinecones.ai;

import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.init.ModAttachments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.bee.Bee;

import java.util.EnumSet;

/**
 * AI goal for defender bees that handles:
 * - Bleeding out after stinging (lost stinger)
 * - Preventing pollen collection
 * - Returning home and despawning when no target
 */
public class DefenderBeeGoal extends Goal {

    private static final int BLEED_DAMAGE_INTERVAL = 20;
    private static final float BLEED_DAMAGE_AMOUNT = 2.0f;

    private final Bee bee;
    private int ticksWithoutTarget = 0;
    private int ticksSinceStung = 0;
    private boolean hasStungTracked = false;

    public DefenderBeeGoal(Bee bee) {
        this.bee = bee;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        return data.isDefender();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        DefenderBeeData data = bee.getData(ModAttachments.DEFENDER_BEE_DATA.get());
        if (!data.isDefender()) return;

        if (bee.hasStung()) {
            if (!hasStungTracked) {
                hasStungTracked = true;
                ticksSinceStung = 0;
            }
            ticksSinceStung++;

            if (ticksSinceStung % BLEED_DAMAGE_INTERVAL == 0) {
                DamageSource bleedDamage = bee.damageSources().genericKill();
                bee.hurtServer(bee.level().getServer().overworld(), bleedDamage, BLEED_DAMAGE_AMOUNT);
            }
            return;
        }

        if (bee.hasNectar()) {
            bee.dropOffNectar();
        }

        bee.setSavedFlowerPos(null);

        if (bee.getTarget() == null && !bee.isAngry()) {
            ticksWithoutTarget++;

            data.homeExpansion().ifPresent(homePos -> {
                double distSq = bee.blockPosition().distSqr(homePos);

                if (distSq < 4) {
                    bee.level().playSound(null, bee.getX(), bee.getY(), bee.getZ(),
                            SoundEvents.BEEHIVE_ENTER, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    bee.discard();
                } else {
                    if (ticksWithoutTarget == 1 || ticksWithoutTarget % 40 == 0) {
                        bee.getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY() + 0.5, homePos.getZ() + 0.5, 1.0);
                    }
                }
            });
        } else {
            ticksWithoutTarget = 0;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
