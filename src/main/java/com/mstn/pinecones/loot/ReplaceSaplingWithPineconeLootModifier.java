package com.mstn.pinecones.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.util.BiomeUtils;
import com.mstn.pinecones.util.TreeUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Loot modifier that replaces sapling drops from leaves with pinecones.
 */
public class ReplaceSaplingWithPineconeLootModifier extends LootModifier {

    public static final Supplier<Codec<ReplaceSaplingWithPineconeLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, ReplaceSaplingWithPineconeLootModifier::new)));

    public ReplaceSaplingWithPineconeLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ObjectArrayList<ItemStack> newLoot = new ObjectArrayList<>();

        for (ItemStack stack : generatedLoot) {
            // Check if this is a sapling
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId.getPath().endsWith("_sapling")) {
                // Replace with pinecone
                ItemStack pinecone = new ItemStack(ModItems.PINECONE.get(), stack.getCount());

                // Try to get tree origin data from the block state
                if (context.hasParam(LootContextParams.BLOCK_STATE) && context.hasParam(LootContextParams.ORIGIN)) {
                    BlockState state = context.getParam(LootContextParams.BLOCK_STATE);
                    Vec3 origin = context.getParam(LootContextParams.ORIGIN);
                    BlockPos pos = BlockPos.containing(origin);
                    ServerLevel serverLevel = context.getLevel();

                    ResourceLocation woodType = TreeUtils.getWoodType(state);
                    ResourceLocation biome = BiomeUtils.getBiomeAt(serverLevel, pos);
                    TreeOriginData.saveToStack(pinecone, new TreeOriginData(woodType, biome));
                }

                newLoot.add(pinecone);
            } else {
                // Keep other loot unchanged
                newLoot.add(stack);
            }
        }

        return newLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
