package com.github.dcysteine.neicustomdiagram.main;

import com.github.dcysteine.neicustomdiagram.main.config.Config;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;
import com.github.dcysteine.neicustomdiagram.net.NcdNetHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class CommonProxy {

    protected boolean hasGenerated = false;

    public void onPreInit(FMLPreInitializationEvent event) {
        NcdNetHandler.init();
    }

    public void onInitialization(FMLInitializationEvent event) {
        Logger.MOD.info("Mod initialization starting...");
        Config.initializeServer();
        Config.saveConfig();
        Logger.MOD.info("Mod initialization complete!");
    }

    public void onLoadComplete(FMLLoadCompleteEvent event) {}

    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!ConfigOptions.GENERATE_DIAGRAMS_ON_CLIENT_CONNECT.get() || hasGenerated) {
            return;
        }
        Logger.MOD.info("Mod pre-connect starting...");

        Registry.INSTANCE.generateDiagramGroups();
        Registry.INSTANCE.cleanUp();
        hasGenerated = true;

        Logger.MOD.info("Mod pre-connect complete!");
    }
}
