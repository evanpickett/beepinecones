package com.mstn.pinecones.init;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.item.PineconeItem;
import com.mstn.pinecones.item.PollenItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(pinecones.MODID);

    public static final DeferredItem<PineconeItem> PINECONE = ITEMS.register("pinecone",
            PineconeItem::new);

    public static final DeferredItem<PollenItem> POLLEN = ITEMS.register("pollen",
            PollenItem::new);
}
