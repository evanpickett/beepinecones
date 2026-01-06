package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles custom tooltips for pinecones and pollen items.
 */
@Mod.EventBusSubscriber(modid = pinecones.MODID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.is(ModItems.PINECONE.get())) {
            addPineconeTooltip(stack, event);
        } else if (stack.is(ModItems.POLLEN.get())) {
            addPollenTooltip(stack, event);
        }
    }

    private static void addPineconeTooltip(ItemStack stack, ItemTooltipEvent event) {
        TreeOriginData data = TreeOriginData.loadFromStack(stack);

        if (data != null) {
            // Format wood type
            String woodName = formatResourceLocation(data.woodType());
            event.getToolTip().add(Component.translatable("tooltip.pinecones.tree_type", woodName)
                    .withStyle(ChatFormatting.GRAY));

            // Format biome
            String biomeName = formatResourceLocation(data.biome());
            event.getToolTip().add(Component.translatable("tooltip.pinecones.biome", biomeName)
                    .withStyle(ChatFormatting.DARK_GREEN));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.pinecones.unknown_origin")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static void addPollenTooltip(ItemStack stack, ItemTooltipEvent event) {
        FlowerData data = FlowerData.loadFromStack(stack);

        if (data != null) {
            // Format flower type
            String flowerName = formatResourceLocation(data.flowerType());
            event.getToolTip().add(Component.translatable("tooltip.pinecones.flower_type", flowerName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.pinecones.unknown_flower")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /**
     * Formats a ResourceLocation into a human-readable string.
     * e.g., "minecraft:oak_log" -> "Oak Log"
     */
    private static String formatResourceLocation(ResourceLocation id) {
        String path = id.getPath();
        // Remove common suffixes and convert to title case
        String formatted = path
                .replace("_log", "")
                .replace("_leaves", "")
                .replace("_", " ");

        // Title case
        StringBuilder result = new StringBuilder();
        boolean nextUpper = true;
        for (char c : formatted.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
