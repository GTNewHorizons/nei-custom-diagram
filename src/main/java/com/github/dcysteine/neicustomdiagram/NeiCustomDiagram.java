package com.github.dcysteine.neicustomdiagram;

import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.mod.Logger;
import com.github.dcysteine.neicustomdiagram.mod.NeiIntegration;
import com.github.dcysteine.neicustomdiagram.mod.Reflection;
import com.github.dcysteine.neicustomdiagram.mod.Registry;
import com.github.dcysteine.neicustomdiagram.mod.config.Config;
import com.github.dcysteine.neicustomdiagram.mod.config.ConfigGuiFactory;
import com.github.dcysteine.neicustomdiagram.mod.config.ConfigOptions;
import com.github.dcysteine.neicustomdiagram.mod.config.DiagramGroupVisibility;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

/** Main entry point for NEI Custom Diagram. */
@Mod(
        modid = NeiCustomDiagram.MOD_ID,
        name = NeiCustomDiagram.MOD_NAME,
        version = NeiCustomDiagram.MOD_VERSION,
        dependencies = NeiCustomDiagram.MOD_DEPENDENCIES,
        guiFactory = ConfigGuiFactory.CLASS_NAME)
public final class NeiCustomDiagram {
    public static final String MOD_ID = "neicustomdiagram";
    public static final String MOD_NAME = "NEI Custom Diagram";
    public static final String MOD_VERSION = "@version@";
    public static final String MOD_DEPENDENCIES =
            "required-after:NotEnoughItems;"
                + "after:gregtech;"
                + "after:bartworks;"
                + "after:detravscannermod;";

    @Instance(MOD_ID)
    public static NeiCustomDiagram instance;

    @EventHandler
    public void onInitialization(FMLInitializationEvent event) {
        if (event.getSide() != Side.CLIENT) {
            return;
        }
        Logger.MOD.info("Mod initialization starting...");

        Reflection.initialize();
        Registry.initialize();
        Config.initialize();
        NeiIntegration.initialize();
        MinecraftForge.EVENT_BUS.register(NeiCustomDiagram.this);

        Logger.MOD.info("Mod initialization complete!");
    }

    @EventHandler
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        if (event.getSide() != Side.CLIENT) {
            return;
        }
        Logger.MOD.info("Mod post-load starting...");

        Registry.generateDiagramGroups();
        Registry.cleanUp();

        Logger.MOD.info("Mod post-load complete!");
    }

    @SubscribeEvent
    public void registerHandlers(NEIRegisterHandlerInfosEvent event) {
        Logger.MOD.info("Registering handlers for diagram groups...");

        for (DiagramGroupInfo info : Registry.info()) {
            if (ConfigOptions.getDiagramGroupVisibility(info) == DiagramGroupVisibility.DISABLED) {
                continue;
            }

            event.registerHandlerInfo(info.groupId(), MOD_NAME, MOD_ID, info::buildHandlerInfo);
            Logger.MOD.info("Registered handler for diagram group [{}]!", info.groupId());
        }

        Logger.MOD.info("Registration complete!");
    }
}