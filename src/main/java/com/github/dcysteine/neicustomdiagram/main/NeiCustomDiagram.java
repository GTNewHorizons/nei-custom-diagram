package com.github.dcysteine.neicustomdiagram.main;

import net.minecraftforge.common.MinecraftForge;

import com.github.dcysteine.neicustomdiagram.main.config.Config;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigGuiFactory;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

/** Main entry point for NEI Custom Diagram. */
@Mod(
        modid = NeiCustomDiagram.MOD_ID,
        name = NeiCustomDiagram.MOD_NAME,
        version = NeiCustomDiagram.MOD_VERSION,
        acceptableRemoteVersions = "*", // Client-side-only mod.
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

    private boolean hasGenerated;

    public NeiCustomDiagram() {
        this.hasGenerated = false;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onInitialization(FMLInitializationEvent event) {
        if (event.getSide() != Side.CLIENT) {
            return;
        }
        Logger.MOD.info("Mod initialization starting...");

        ConfigGuiFactory.checkClassName();

        Config.initialize();
        Registry.INSTANCE.initialize();
        Config.initializeDiagramGroupVisibility(Registry.INSTANCE.infoList());
        Config.saveConfig();
        NeiIntegration.INSTANCE.initialize(Registry.INSTANCE.infoList());

        MinecraftForge.EVENT_BUS.register(NeiIntegration.INSTANCE);
        if (ConfigOptions.GENERATE_DIAGRAMS_ON_CLIENT_CONNECT.get()) {
            FMLCommonHandler.instance().bus().register(this);
        }

        Logger.MOD.info("Mod initialization complete!");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        if (event.getSide() != Side.CLIENT || ConfigOptions.GENERATE_DIAGRAMS_ON_CLIENT_CONNECT.get()) {
            return;
        }
        Logger.MOD.info("Mod post-load starting...");

        Registry.INSTANCE.generateDiagramGroups();
        Registry.INSTANCE.cleanUp();
        hasGenerated = true;

        Logger.MOD.info("Mod post-load complete!");
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
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
