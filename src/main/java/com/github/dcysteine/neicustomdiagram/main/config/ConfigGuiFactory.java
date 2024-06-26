package com.github.dcysteine.neicustomdiagram.main.config;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.github.dcysteine.neicustomdiagram.main.NeiCustomDiagram;

import cpw.mods.fml.client.IModGuiFactory;

public final class ConfigGuiFactory implements IModGuiFactory {

    /**
     * This is used by {@link NeiCustomDiagram} to configure a GUI factory. Make sure to keep it updated if this class
     * is moved or renamed.
     */
    public static final String CLASS_NAME = "com.github.dcysteine.neicustomdiagram.main.config.ConfigGuiFactory";

    public static void checkClassName() {
        if (!CLASS_NAME.equals(ConfigGuiFactory.class.getName())) {
            throw new IllegalStateException("ConfigGuiFactory.CLASS_NAME needs to be updated!");
        }
    }

    @Override
    public void initialize(Minecraft minecraft) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement runtimeOptionCategoryElement) {
        return null;
    }
}
