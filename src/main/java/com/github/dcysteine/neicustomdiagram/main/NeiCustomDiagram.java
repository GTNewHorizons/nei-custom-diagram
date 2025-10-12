package com.github.dcysteine.neicustomdiagram.main;

import com.github.dcysteine.neicustomdiagram.lib.net.MessageToClient;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigGuiFactory;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * Main entry point for NEI Custom Diagram.
 */
@Mod(
        modid = NeiCustomDiagram.MOD_ID,
        name = NeiCustomDiagram.MOD_NAME,
        version = NeiCustomDiagram.MOD_VERSION,
        acceptableRemoteVersions = NeiCustomDiagram.MOD_VERSION,
        dependencies = NeiCustomDiagram.MOD_DEPENDENCIES,
        guiFactory = ConfigGuiFactory.CLASS_NAME)
public final class NeiCustomDiagram {

    public static final String MOD_ID = "neicustomdiagram";
    public static final String MOD_NAME = "NEI Custom Diagram";
    public static final String MOD_VERSION = Tags.VERSION;
    public static final String MOD_DEPENDENCIES = "required-after:NotEnoughItems;" + "after:dreamcraft;"
            + "after:gregtech;"
            + "after:bartworks;"
            + "after:miscutils;"
            + "after:detravscannermod;"
            + "after:MineTweaker3;"
            + "after:EnderStorage;";

    @Instance(MOD_ID)
    public static NeiCustomDiagram instance;

    @SidedProxy(
            clientSide = "com.github.dcysteine.neicustomdiagram.main.ClientProxy",
            serverSide = "com.github.dcysteine.neicustomdiagram.main.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    @SuppressWarnings("unused")
    public void onPreInit(FMLPreInitializationEvent event) {
        proxy.onPreInit(event);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onInitialization(FMLInitializationEvent event) {
        proxy.onInitialization(event);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        proxy.onLoadComplete(event);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        proxy.onClientConnected(event);
    }

    public void handleClientMessage(MessageToClient message) {
        message.onMessage();
    }
}
