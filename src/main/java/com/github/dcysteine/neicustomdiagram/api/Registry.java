package com.github.dcysteine.neicustomdiagram.api;

import codechicken.nei.api.API;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGenerator;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup;
import com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroupInfo;
import com.github.dcysteine.neicustomdiagram.generators.forge.fluidcontainers.ForgeFluidContainers;
import com.github.dcysteine.neicustomdiagram.generators.forge.oredictionary.ForgeOreDictionary;
import com.github.dcysteine.neicustomdiagram.generators.gregtech.oredictionary.GregTechOreDictionary;
import com.github.dcysteine.neicustomdiagram.generators.gregtech.oreprocessing.GregTechOreProcessing;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import cpw.mods.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registry of diagram generators. Add your diagram generator here! */
public final class Registry {
    /** This will be prepended to all group IDs, to ensure that they are globally unique. */
    public static final String GROUP_ID_PREFIX = "neicustomdiagram.diagramgroup.";

    private static final List<RegistryEntry> entries = new ArrayList<>();
    private static final List<DiagramGenerator> generators = new ArrayList<>();

    static {
        // Add your diagram generator here!
        addGenerator("forge.fluidcontainers", ForgeFluidContainers::new);
        addGenerator("forge.oredictionary", ForgeOreDictionary::new);
        addGenerator("gregtech.oredictionary", GregTechOreDictionary::new, ModIds.GREGTECH);
        addGenerator("gregtech.oreprocessing", GregTechOreProcessing::new, ModIds.GREGTECH);
    }

    /** For convenience, some mod IDs stored as constants. */
    public static final class ModIds {
        // If you're adding a new mod dependency here, don't forget to also add it to the list of
        // dependencies in NeiCustomDiagram.java (if necessary).
        public static final String GREGTECH = "gregtech";
        public static final String BARTWORKS = "bartworks";

        public static boolean isModLoaded(String modId) {
            return Loader.isModLoaded(modId);
        }
    }

    @AutoValue
    protected abstract static class RegistryEntry {
        protected static RegistryEntry create(
                String groupIdSuffix, Function<String, DiagramGenerator> generatorConstructor,
                String... hardDependencies) {
            return new AutoValue_Registry_RegistryEntry(
                    GROUP_ID_PREFIX + groupIdSuffix, generatorConstructor,
                    ImmutableSet.copyOf(hardDependencies));
        }

        protected abstract String groupId();
        protected abstract Function<String, DiagramGenerator> generatorConstructor();
        protected abstract ImmutableSet<String> hardDependencies();

        protected DiagramGenerator get() {
            return generatorConstructor().apply(groupId());
        }

        protected List<String> missingDependencies() {
            return hardDependencies().stream()
                    .filter(modId -> !ModIds.isModLoaded(modId))
                    .collect(Collectors.toList());
        }
    }

    // Static class.
    private Registry() {};

    /** This method must be called before mod initialization. */
    public static void addGenerator(
            String groupIdSuffix, Function<String, DiagramGenerator> generatorConstructor,
            String... hardDependencies) {
        entries.add(RegistryEntry.create(groupIdSuffix, generatorConstructor, hardDependencies));
    }

    /** This method is only intended to be called during mod initialization. */
    public static void initialize() {
        Logger.MOD.info("Initializing diagram groups...");

        for (RegistryEntry entry : entries) {
            List<String> missingDependencies = entry.missingDependencies();
            if (!missingDependencies.isEmpty()) {
                Logger.MOD.warn(
                        "Diagram group [{}] is missing dependencies: {}",
                        entry.groupId(), missingDependencies);
                continue;
            }

            generators.add(entry.get());
            Logger.MOD.info("Initialized diagram group [{}]!", entry.groupId());
        }

        Logger.MOD.info("Initialization complete!");
    }

    /**
     * We use this method as an accessor so that we can guarantee that callers cannot mutate
     * {@link #entries}.
     */
    public static ImmutableList<DiagramGenerator> generators() {
        return ImmutableList.copyOf(generators);
    }

    public static void generateDiagramGroups() {
        Logger.MOD.info("Generating diagram groups...");

        for (DiagramGenerator generator : generators) {
            DiagramGroupInfo info = generator.info();
            if (!Config.getDiagramEnabled(info)) {
                Logger.MOD.info("Diagram group [{}] disabled by config.", info.groupId());
                continue;
            }

            Logger.MOD.info("Generating diagram group [{}]...", info.groupId());
            DiagramGroup diagramGroup = generator.generate();
            API.registerRecipeHandler(diagramGroup);
            API.registerUsageHandler(diagramGroup);
            Logger.MOD.info("Generated diagram group [{}]!", info.groupId());
        }

        Logger.MOD.info("Generation complete!");
    }
}