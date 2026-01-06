package com.mstn.pinecones.event;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import com.mstn.pinecones.util.BiomeUtils;
import com.mstn.pinecones.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles pinecone drops when leaves are destroyed (by player or natural decay).
 * In Forge 1.20.1, we use BlockEvent.BreakEvent to spawn pinecones when leaves are broken.
 * Note: This won't catch natural leaf decay, but that's handled separately or via loot tables.
 */
@Mod.EventBusSubscriber(modid = pinecones.MODID)
public class LeafDecayHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockState state = event.getState();
        BlockPos pos = event.getPos();

        // Only process leaf blocks
        if (!state.is(ModTags.Blocks.TREE_LEAVES)) return;

        // Only process natural leaves (not player-placed)
        if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
            return;
        }

        // Get tree origin data for pinecones
        BlockPos logPos = TreeUtils.findConnectedLog(serverLevel, pos);
        ResourceLocation woodType;
        if (logPos != null) {
            woodType = TreeUtils.getWoodType(serverLevel.getBlockState(logPos));
        } else {
            woodType = TreeUtils.getWoodType(state);
        }
        ResourceLocation biome = BiomeUtils.getBiomeAt(serverLevel, pos);

        // Roll chance to drop a pinecone (similar to sapling drop chance)
        if (serverLevel.random.nextFloat() < 0.05f) { // 5% base chance, similar to sapling
            ItemStack pinecone = new ItemStack(ModItems.PINECONE.get());
            TreeOriginData.saveToStack(pinecone, new TreeOriginData(woodType, biome));

            // Spawn as PineconeEntity so it can try to replant
            PineconeEntity pineconeEntity = new PineconeEntity(
                    serverLevel,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    pinecone
            );
            serverLevel.addFreshEntity(pineconeEntity);
        }
    }
}
