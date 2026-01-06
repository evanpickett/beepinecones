package com.mstn.pinecones.item;

import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Pinecone item that places the appropriate sapling when used on valid ground.
 */
public class PineconeItem extends Item {

    // Map log types to their corresponding saplings
    private static final Map<String, Block> LOG_TO_SAPLING = Map.ofEntries(
            Map.entry("minecraft:oak_log", Blocks.OAK_SAPLING),
            Map.entry("minecraft:spruce_log", Blocks.SPRUCE_SAPLING),
            Map.entry("minecraft:birch_log", Blocks.BIRCH_SAPLING),
            Map.entry("minecraft:jungle_log", Blocks.JUNGLE_SAPLING),
            Map.entry("minecraft:acacia_log", Blocks.ACACIA_SAPLING),
            Map.entry("minecraft:dark_oak_log", Blocks.DARK_OAK_SAPLING),
            Map.entry("minecraft:mangrove_log", Blocks.MANGROVE_PROPAGULE),
            Map.entry("minecraft:cherry_log", Blocks.CHERRY_SAPLING),
            Map.entry("minecraft:stripped_oak_log", Blocks.OAK_SAPLING),
            Map.entry("minecraft:stripped_spruce_log", Blocks.SPRUCE_SAPLING),
            Map.entry("minecraft:stripped_birch_log", Blocks.BIRCH_SAPLING),
            Map.entry("minecraft:stripped_jungle_log", Blocks.JUNGLE_SAPLING),
            Map.entry("minecraft:stripped_acacia_log", Blocks.ACACIA_SAPLING),
            Map.entry("minecraft:stripped_dark_oak_log", Blocks.DARK_OAK_SAPLING),
            Map.entry("minecraft:stripped_mangrove_log", Blocks.MANGROVE_PROPAGULE),
            Map.entry("minecraft:stripped_cherry_log", Blocks.CHERRY_SAPLING),
            Map.entry("minecraft:oak_leaves", Blocks.OAK_SAPLING),
            Map.entry("minecraft:spruce_leaves", Blocks.SPRUCE_SAPLING),
            Map.entry("minecraft:birch_leaves", Blocks.BIRCH_SAPLING),
            Map.entry("minecraft:jungle_leaves", Blocks.JUNGLE_SAPLING),
            Map.entry("minecraft:acacia_leaves", Blocks.ACACIA_SAPLING),
            Map.entry("minecraft:dark_oak_leaves", Blocks.DARK_OAK_SAPLING),
            Map.entry("minecraft:mangrove_leaves", Blocks.MANGROVE_PROPAGULE),
            Map.entry("minecraft:cherry_leaves", Blocks.CHERRY_SAPLING),
            Map.entry("minecraft:azalea_leaves", Blocks.AZALEA),
            Map.entry("minecraft:flowering_azalea_leaves", Blocks.FLOWERING_AZALEA)
    );

    public PineconeItem(Identifier id) {
        super(new Properties().setId(ResourceKey.create(Registries.ITEM, id)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = clickedPos.relative(context.getClickedFace());
        ItemStack stack = context.getItemInHand();

        // Check if we can place on top of clicked block
        BlockState groundState = level.getBlockState(clickedPos);
        if (!canPlaceOn(groundState)) {
            return InteractionResult.PASS;
        }

        // Check if the place position is empty
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        // Get the sapling to place based on tree origin data
        Block sapling = getSaplingFromItem(stack);

        // Check if the sapling can survive at this position
        BlockState saplingState = sapling.defaultBlockState();
        if (!saplingState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            level.setBlock(placePos, saplingState, Block.UPDATE_ALL);
            level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

            // Consume the item
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private boolean canPlaceOn(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND);
    }

    private Block getSaplingFromItem(ItemStack stack) {
        TreeOriginData data = stack.get(ModDataComponents.TREE_ORIGIN.get());
        if (data != null) {
            Block sapling = LOG_TO_SAPLING.get(data.woodType().toString());
            if (sapling != null) {
                return sapling;
            }

            // Try to find sapling by converting wood type ID
            String woodTypeStr = data.woodType().toString();
            String saplingId = woodTypeStr
                    .replace("_log", "_sapling")
                    .replace("_wood", "_sapling")
                    .replace("_leaves", "_sapling")
                    .replace("stripped_", "");

            Block dynamicSapling = BuiltInRegistries.BLOCK.getValue(Identifier.parse(saplingId));
            if (dynamicSapling != Blocks.AIR) {
                return dynamicSapling;
            }
        }

        // Default to oak sapling
        return Blocks.OAK_SAPLING;
    }
}
