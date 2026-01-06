package com.mstn.pinecones.item;

import com.mstn.pinecones.component.FlowerData;
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

/**
 * Pollen item that places the appropriate flower when used on valid ground.
 */
public class PollenItem extends Item {

    public PollenItem(Identifier id) {
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

        // Get the flower to place based on flower data
        Block flower = getFlowerFromItem(stack);

        // Check if the flower can survive at this position
        BlockState flowerState = flower.defaultBlockState();
        if (!flowerState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            level.setBlock(placePos, flowerState, Block.UPDATE_ALL);
            level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

            // Consume the item
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private boolean canPlaceOn(BlockState state) {
        return state.is(BlockTags.DIRT);
    }

    private Block getFlowerFromItem(ItemStack stack) {
        FlowerData data = stack.get(ModDataComponents.FLOWER_DATA.get());
        if (data != null) {
            Block flower = BuiltInRegistries.BLOCK.getValue(data.flowerType());
            if (flower != Blocks.AIR) {
                return flower;
            }
        }

        // Default to poppy
        return Blocks.POPPY;
    }
}
