package com.mstn.pinecones.integration.jei;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.init.ModBlocks;
import com.mstn.pinecones.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI plugin for Pinecones mod.
 * Provides information pages for mod items and blocks.
 */
@JeiPlugin
public class PineconesJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = new ResourceLocation(pinecones.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Pinecone info
        registration.addIngredientInfo(
                new ItemStack(ModItems.PINECONE.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.pinecones.pinecone.info")
        );

        // Pollen info
        registration.addIngredientInfo(
                new ItemStack(ModItems.POLLEN.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.pinecones.pollen.info")
        );

        // Colony Expansion info
        registration.addIngredientInfo(
                new ItemStack(ModBlocks.COLONY_EXPANSION.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.pinecones.colony_expansion.info")
        );
    }
}
