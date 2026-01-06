package com.mstn.pinecones.event;

import com.mstn.pinecones.pinecones;
import com.mstn.pinecones.command.PineconesCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = pinecones.MODID)
public class CommandEventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        PineconesCommand.register(event.getDispatcher());
    }
}
