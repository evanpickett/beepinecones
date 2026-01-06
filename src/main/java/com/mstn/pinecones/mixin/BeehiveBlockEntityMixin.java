package com.mstn.pinecones.mixin;

import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.entity.PollenEntity;
import com.mstn.pinecones.event.ColonyExpansionHandler;
import com.mstn.pinecones.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(BeehiveBlockEntity.class)
public class BeehiveBlockEntityMixin {

    /**
     * When a bee enters the hive, drop any carried items first.
     * In 1.20.1, addOccupant takes (Entity, boolean hasNectar).
     */
    @Inject(method = "addOccupant", at = @At("HEAD"))
    private void onBeeEnter(Entity entity, boolean hasNectar, CallbackInfo ci) {
        if (!(entity instanceof Bee bee)) return;
        if (bee.level().isClientSide()) return;

        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        if (!data.isCarryingItem()) return;

        ItemStack carried = data.carriedItem();
        BlockPos dropPos = bee.blockPosition();
        Level level = bee.level();

        if (carried.is(ModItems.PINECONE.get())) {
            PineconeEntity pinecone = new PineconeEntity(
                    level,
                    dropPos.getX() + 0.5,
                    dropPos.getY() + 0.5,
                    dropPos.getZ() + 0.5,
                    carried.copy()
            );
            level.addFreshEntity(pinecone);
        } else if (carried.is(ModItems.POLLEN.get())) {
            PollenEntity pollen = new PollenEntity(
                    level,
                    dropPos.getX() + 0.5,
                    dropPos.getY() + 0.5,
                    dropPos.getZ() + 0.5,
                    carried.copy()
            );
            level.addFreshEntity(pollen);
        }

        BeeCarryData.saveToEntity(bee, data.clearCarriedItem());
    }

    /**
     * Inject at the end of releaseOccupant to trigger colony expansion check.
     * BeeData is made accessible via access transformer.
     */
    @Inject(method = "releaseOccupant", at = @At("RETURN"))
    private static void onBeeRelease(Level level, BlockPos pos, BlockState state,
                                     BeehiveBlockEntity.BeeData beeData,
                                     @Nullable List<Entity> storedInHives,
                                     BeehiveBlockEntity.BeeReleaseStatus releaseStatus,
                                     @Nullable BlockPos flowerPos,
                                     CallbackInfoReturnable<Boolean> cir) {
        Boolean released = cir.getReturnValue();
        com.mstn.pinecones.pinecones.LOGGER.debug("Bee release mixin triggered: released={}, level={}, pos={}", released, level != null, pos);
        if (Boolean.TRUE.equals(released) && level != null && pos != null && !level.isClientSide()) {
            com.mstn.pinecones.pinecones.LOGGER.debug("Calling checkExpansionFormation at {}", pos);
            ColonyExpansionHandler.checkExpansionFormation(level, pos);
        }
    }
}
