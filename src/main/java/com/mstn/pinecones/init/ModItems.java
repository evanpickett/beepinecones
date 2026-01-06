package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.item.PineconeItem;
import com.mstn.pinecones.item.PollenItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, pinecones.MODID);

    public static final RegistryObject<PineconeItem> PINECONE = ITEMS.register("pinecone",
            () -> new PineconeItem(new Item.Properties()));

    public static final RegistryObject<PollenItem> POLLEN = ITEMS.register("pollen",
            () -> new PollenItem(new Item.Properties()));
}
