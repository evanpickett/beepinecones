package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.component.TreeOriginData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, pinecones.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TreeOriginData>> TREE_ORIGIN =
            DATA_COMPONENTS.register("tree_origin", () -> DataComponentType.<TreeOriginData>builder()
                    .persistent(TreeOriginData.CODEC)
                    .networkSynchronized(TreeOriginData.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlowerData>> FLOWER_DATA =
            DATA_COMPONENTS.register("flower_data", () -> DataComponentType.<FlowerData>builder()
                    .persistent(FlowerData.CODEC)
                    .networkSynchronized(FlowerData.STREAM_CODEC)
                    .build());
}
