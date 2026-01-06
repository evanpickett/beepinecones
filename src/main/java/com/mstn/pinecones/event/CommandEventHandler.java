package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.command.PineconesCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = pinecones.MODID)
public class CommandEventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PineconesCommand.register(event.getDispatcher());
    }
}
