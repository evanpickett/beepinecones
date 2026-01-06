package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.init.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pinecones.MODID)
public class EntityEventHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Replace regular ItemEntity with PineconeEntity for pinecone items
        if (event.getEntity() instanceof ItemEntity itemEntity && !(event.getEntity() instanceof PineconeEntity)) {
            ItemStack stack = itemEntity.getItem();

            if (stack.is(ModItems.PINECONE.get())) {
                event.setCanceled(true);

                // Create our custom entity
                PineconeEntity pineconeEntity = new PineconeEntity(
                        event.getLevel(),
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        stack.copy()
                );

                // Copy velocity
                pineconeEntity.setDeltaMovement(itemEntity.getDeltaMovement());
                pineconeEntity.setPickUpDelay(40); // 2 second pickup delay

                event.getLevel().addFreshEntity(pineconeEntity);
            }
        }
    }
}
