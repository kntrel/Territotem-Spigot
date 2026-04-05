package com.kntrel.mc.territotem.config.blueprint;

import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.kntrel.mc.territotem.totem.TotemBlueprint;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TotemBlueprintDirectoryLoader {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemBlueprintDirectoryLoader.class);
    private static final String DIRECTORY_NAME = "totems";
    private static final List<String> DEFAULT_RESOURCES = List.of("totems/domestic.yml");


    //FIELDS
    private final Plugin plugin_;
    private final TotemBlueprintYamlLoader yamlLoader_;


    //CONSTRUCTOR
    public TotemBlueprintDirectoryLoader(
            Plugin plugin,
            TotemCoreTracker coreTracker,
            HierarchyRepository hierarchyRepository,
            RuleRegistry ruleRegistry
    ) {
        this.plugin_ = plugin;
        this.yamlLoader_ = new TotemBlueprintYamlLoader(coreTracker, hierarchyRepository, ruleRegistry);
    }


    //ALL
    public List<TotemBlueprint> loadAll() {
        File directory = ensureDirectoryExists();
        this.installBundledDefaults();

        File[] files = directory.listFiles(file ->
                file.isFile() && (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))
        );

        if (files == null || files.length == 0) {
            LOGGER.warn("No totem blueprint files were found in '{}'.", directory.getAbsolutePath());
            return List.of();
        }

        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        List<TotemBlueprint> out = new ArrayList<>(files.length);
        Map<Long, File> ids = new HashMap<>();
        for (File file : files) {
            TotemBlueprint blueprint = this.yamlLoader_.load(file);
            File existing = ids.putIfAbsent(blueprint.id(), file);
            if (existing != null) {
                throw new BlueprintValidationException(
                        "Blueprint ID '" + blueprint.id() + "' is duplicated in files '"
                                + existing.getAbsolutePath() + "' and '" + file.getAbsolutePath() + "'."
                );
            }
            out.add(blueprint);
        }

        LOGGER.info("Loaded {} totem blueprint(s) from '{}'.", out.size(), directory.getAbsolutePath());
        return List.copyOf(out);
    }


    //HELPERS
    private File ensureDirectoryExists() {
        File directory = new File(this.plugin_.getDataFolder(), DIRECTORY_NAME);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new BlueprintValidationException(
                    "Could not create blueprint directory '" + directory.getAbsolutePath() + "'."
            );
        }
        if (!directory.isDirectory()) {
            throw new BlueprintValidationException(
                    "Blueprint path '" + directory.getAbsolutePath() + "' is not a directory."
            );
        }
        return directory;
    }
    private void installBundledDefaults() {
        for (String resource : DEFAULT_RESOURCES) {
            try {
                this.plugin_.saveResource(resource, false);
            } catch (IllegalArgumentException ignored) {
                LOGGER.debug("Bundled blueprint resource '{}' is not present in the jar.", resource);
            }
        }
    }
}
