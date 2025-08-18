package com.github.dcysteine.neicustomdiagram.main;

import net.minecraftforge.common.MinecraftForge;

import com.github.dcysteine.neicustomdiagram.main.config.Config;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigGuiFactory;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void onInitialization(FMLInitializationEvent event) {
        Logger.MOD.info("Mod initialization starting...");

        ConfigGuiFactory.checkClassName();

        Config.initialize();
        Registry.INSTANCE.initialize();
        Config.initializeDiagramGroupVisibility(Registry.INSTANCE.infoList());
        Config.saveConfig();
        NeiIntegration.INSTANCE.initialize(Registry.INSTANCE.infoList());

        MinecraftForge.EVENT_BUS.register(NeiIntegration.INSTANCE);
        if (ConfigOptions.GENERATE_DIAGRAMS_ON_CLIENT_CONNECT.get()) {
            FMLCommonHandler.instance().bus().register(NeiCustomDiagram.instance);
        }

        Logger.MOD.info("Mod initialization complete!");
    }

    @Override
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        if (ConfigOptions.GENERATE_DIAGRAMS_ON_CLIENT_CONNECT.get()) {
            return;
        }
        Logger.MOD.info("Mod post-load starting...");

        Registry.INSTANCE.generateDiagramGroups();
        Registry.INSTANCE.cleanUp();
        hasGenerated = true;

        Logger.MOD.info("Mod post-load complete!");
    }
}
