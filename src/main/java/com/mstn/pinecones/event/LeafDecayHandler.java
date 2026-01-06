package com.mstn.pinecones.event;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.init.ModDataComponents;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import com.mstn.pinecones.util.BiomeUtils;
import com.mstn.pinecones.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles pinecone drops when leaves are destroyed (by player or natural decay).
 * Intercepts sapling drops and either replaces them with pinecones or adds pinecones alongside.
 */
@EventBusSubscriber(modid = pinecones.MODID)
public class LeafDecayHandler {

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

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
        Identifier woodType;
        if (logPos != null) {
            woodType = TreeUtils.getWoodType(serverLevel.getBlockState(logPos));
        } else {
            woodType = TreeUtils.getWoodType(state);
        }
        Identifier biome = BiomeUtils.getBiomeAt(serverLevel, pos);

        // Find and process sapling drops
        List<ItemEntity> drops = event.getDrops();
        List<ItemEntity> pineconesToAdd = new ArrayList<>();
        Iterator<ItemEntity> iterator = drops.iterator();

        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            ItemStack stack = itemEntity.getItem();

            if (stack.is(ModTags.Items.SAPLINGS)) {
                // Create pinecone with same count as sapling
                ItemStack pinecone = new ItemStack(ModItems.PINECONE.get(), stack.getCount());
                pinecone.set(ModDataComponents.TREE_ORIGIN.get(), new TreeOriginData(woodType, biome));

                if (Config.REPLACE_SAPLING_DROPS.get()) {
                    // Replace sapling with pinecone
                    itemEntity.setItem(pinecone);
                } else {
                    // Add pinecone alongside sapling
                    ItemEntity pineconeEntity = new ItemEntity(
                            serverLevel,
                            itemEntity.getX(),
                            itemEntity.getY(),
                            itemEntity.getZ(),
                            pinecone
                    );
                    pineconeEntity.setDefaultPickUpDelay();
                    pineconesToAdd.add(pineconeEntity);
                }
            }
        }

        // Add any additional pinecones (when not replacing)
        drops.addAll(pineconesToAdd);
    }
}
