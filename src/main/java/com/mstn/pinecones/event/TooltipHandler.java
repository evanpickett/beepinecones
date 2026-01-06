package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.init.ModDataComponents;
import com.mstn.pinecones.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Handles custom tooltips for pinecones and pollen items.
 */
@EventBusSubscriber(modid = pinecones.MODID, value = Dist.CLIENT)
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
        TreeOriginData data = stack.get(ModDataComponents.TREE_ORIGIN.get());

        if (data != null) {
            // Format wood type
            String woodName = formatIdentifier(data.woodType());
            event.getToolTip().add(Component.translatable("tooltip.pinecones.tree_type", woodName)
                    .withStyle(ChatFormatting.GRAY));

            // Format biome
            String biomeName = formatIdentifier(data.biome());
            event.getToolTip().add(Component.translatable("tooltip.pinecones.biome", biomeName)
                    .withStyle(ChatFormatting.DARK_GREEN));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.pinecones.unknown_origin")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static void addPollenTooltip(ItemStack stack, ItemTooltipEvent event) {
        FlowerData data = stack.get(ModDataComponents.FLOWER_DATA.get());

        if (data != null) {
            // Format flower type
            String flowerName = formatIdentifier(data.flowerType());
            event.getToolTip().add(Component.translatable("tooltip.pinecones.flower_type", flowerName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.pinecones.unknown_flower")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /**
     * Formats an Identifier into a human-readable string.
     * e.g., "minecraft:oak_log" -> "Oak Log"
     */
    private static String formatIdentifier(Identifier id) {
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
