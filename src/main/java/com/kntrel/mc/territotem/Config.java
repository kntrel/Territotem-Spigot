package com.kntrel.mc.territotem;

import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.util.Grid;
import com.kntrel.mc.territotem.region.RegionEnterTitleConfig;
import com.kntrel.mc.territotem.region.RegionFeatureMaterialsConfig;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

record Config(
        Material directionalSelectorItem,
        Set<Material> allowedDeedsRequestItems,
        RegionContextConfig regionContextConfig,
        RegionEnterTitleConfig regionEnterTitle,
        RegionFeatureMaterialsConfig regionFeatureMaterials
) {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private static final Config DEFAULT = new Config(
            Material.AMETHYST_BLOCK,
            immutableLinkedSet(List.of(Material.WRITABLE_BOOK)),
            RegionContextConfig.defaultConfig(),
            new RegionEnterTitleConfig(true, 10, 20, 10),
            new RegionFeatureMaterialsConfig(
                    immutableLinkedSet(List.of(
                            Material.STONE_BUTTON,
                            Material.POLISHED_BLACKSTONE_BUTTON
                    )),
                    immutableLinkedSet(List.of(
                            Material.COPPER_BLOCK,
                            Material.CUT_COPPER,
                            Material.EXPOSED_COPPER,
                            Material.EXPOSED_CUT_COPPER,
                            Material.WEATHERED_COPPER,
                            Material.WEATHERED_CUT_COPPER,
                            Material.WAXED_COPPER_BLOCK,
                            Material.WAXED_CUT_COPPER,
                            Material.WAXED_EXPOSED_COPPER,
                            Material.WAXED_EXPOSED_CUT_COPPER,
                            Material.WAXED_WEATHERED_COPPER,
                            Material.WAXED_WEATHERED_CUT_COPPER,
                            Material.CUT_COPPER_SLAB,
                            Material.CUT_COPPER_STAIRS,
                            Material.EXPOSED_CUT_COPPER_SLAB,
                            Material.EXPOSED_CUT_COPPER_STAIRS,
                            Material.WEATHERED_CUT_COPPER_SLAB,
                            Material.WEATHERED_CUT_COPPER_STAIRS,
                            Material.WAXED_CUT_COPPER_SLAB,
                            Material.WAXED_CUT_COPPER_STAIRS,
                            Material.WAXED_EXPOSED_CUT_COPPER_SLAB,
                            Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,
                            Material.WAXED_WEATHERED_CUT_COPPER_SLAB,
                            Material.WAXED_WEATHERED_CUT_COPPER_STAIRS,
                            Material.IRON_BLOCK
                    ))
            )
    );

    static Config load(JavaPlugin plugin, String relativePath) {
        File file = ensureFileExists(plugin, relativePath);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        Material directionalSelectorItem = readBlockMaterial(
                yaml,
                "totem.directional_selector_item",
                DEFAULT.directionalSelectorItem()
        );
        Set<Material> allowedDeedsRequestItems = readMaterialSet(
                yaml,
                "totem.deeds_request_items",
                DEFAULT.allowedDeedsRequestItems()
        );
        RegionContextConfig defaultRegionContextConfig = DEFAULT.regionContextConfig();
        RegionContextConfig regionContextConfig = new RegionContextConfig(
                readInt(
                        yaml,
                        "region.min_name_length",
                        defaultRegionContextConfig.minNameLength
                ),
                readInt(
                        yaml,
                        "region.max_name_length",
                        defaultRegionContextConfig.maxNameLength
                ),
                readOverlapMode(
                        yaml,
                        "region.permissions_overlap_mode",
                        defaultRegionContextConfig.permissionsOverlapMode
                ),
                readInt(
                        yaml,
                        "region.display_duration_seconds",
                        defaultRegionContextConfig.regionDisplayDurationSeconds
                ),
                readCellSize(
                        yaml,
                        "region.cache.cell_size",
                        defaultRegionContextConfig.cellSize
                ),
                readInt(
                        yaml,
                        "region.cache.capacity",
                        defaultRegionContextConfig.cacheCapacity
                ),
                readLong(
                        yaml,
                        "region.player_sampling.period_ticks",
                        defaultRegionContextConfig.playerSamplingPeriodTicks
                ),
                readDouble(
                        yaml,
                        "region.player_sampling.movement_tolerance",
                        defaultRegionContextConfig.playerMovementTolerance
                )
        );
        RegionEnterTitleConfig regionEnterTitle = new RegionEnterTitleConfig(
                readBoolean(
                        yaml,
                        "region.enter_title.enabled",
                        DEFAULT.regionEnterTitle().enabled()
                ),
                readInt(
                        yaml,
                        "region.enter_title.fade_in",
                        DEFAULT.regionEnterTitle().fadeIn()
                ),
                readInt(
                        yaml,
                        "region.enter_title.stay",
                        DEFAULT.regionEnterTitle().stay()
                ),
                readInt(
                        yaml,
                        "region.enter_title.fade_out",
                        DEFAULT.regionEnterTitle().fadeOut()
                )
        );
        RegionFeatureMaterialsConfig regionFeatureMaterials = new RegionFeatureMaterialsConfig(
                readMaterialSet(
                        yaml,
                        "region.enforced_buttons",
                        DEFAULT.regionFeatureMaterials().enforcedButtons()
                ),
                readMaterialSet(
                        yaml,
                        "region.lever_locker_blocks",
                        DEFAULT.regionFeatureMaterials().leverLockerBlocks()
                )
        );

        return new Config(
                directionalSelectorItem,
                allowedDeedsRequestItems,
                regionContextConfig,
                regionEnterTitle,
                regionFeatureMaterials
        );
    }

    private static File ensureFileExists(JavaPlugin plugin, String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        if (file.exists()) {
            return file;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.error("Could not create directories for config path '{}'.", file.getAbsolutePath());
        }

        plugin.saveResource(relativePath, false);
        LOGGER.info("Config file '{}' was missing from the file system and has been copied from the jar.", relativePath);
        return file;
    }

    private static Material readMaterial(YamlConfiguration yaml, String path, Material defaultValue) {
        String raw = readString(yaml, path, defaultValue.name());
        try {
            return Material.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            LOGGER.error(
                    "Invalid material '{}' for config key '{}'. Using default '{}'.",
                    raw,
                    path,
                    defaultValue
            );
            return defaultValue;
        }
    }

    private static Material readBlockMaterial(YamlConfiguration yaml, String path, Material defaultValue) {
        Material material = readMaterial(yaml, path, defaultValue);
        if (material.isBlock()) {
            return material;
        }

        LOGGER.error(
                "Invalid material '{}' for config key '{}'. Directional selector items must be blocks. Using default '{}'.",
                material,
                path,
                defaultValue
        );
        return defaultValue;
    }

    private static Set<Material> readMaterialSet(YamlConfiguration yaml, String path, Set<Material> defaultValue) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        if (!(raw instanceof List<?> rawList)) {
            LOGGER.error("Invalid config value type for key '{}'. Using default '{}'.", path, defaultValue);
            return defaultValue;
        }

        LinkedHashSet<Material> out = new LinkedHashSet<>();
        for (Object element : rawList) {
            if (!(element instanceof String rawMaterial)) {
                LOGGER.error("Ignoring non-string entry '{}' in config key '{}'.", element, path);
                continue;
            }
            try {
                out.add(Material.valueOf(rawMaterial));
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Ignoring invalid material '{}' in config key '{}'.", rawMaterial, path);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static boolean readBoolean(YamlConfiguration yaml, String path, boolean defaultValue) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }

        LOGGER.error("Invalid boolean '{}' for config key '{}'. Using default '{}'.", raw, path, defaultValue);
        return defaultValue;
    }

    private static int readInt(YamlConfiguration yaml, String path, int defaultValue) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                // Fall through to the common error log below.
            }
        }

        LOGGER.error("Invalid integer '{}' for config key '{}'. Using default '{}'.", raw, path, defaultValue);
        return defaultValue;
    }

    private static long readLong(YamlConfiguration yaml, String path, long defaultValue) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                // Fall through to the common error log below.
            }
        }

        LOGGER.error("Invalid long '{}' for config key '{}'. Using default '{}'.", raw, path, defaultValue);
        return defaultValue;
    }

    private static double readDouble(YamlConfiguration yaml, String path, double defaultValue) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                // Fall through to the common error log below.
            }
        }

        LOGGER.error("Invalid double '{}' for config key '{}'. Using default '{}'.", raw, path, defaultValue);
        return defaultValue;
    }

    private static String readString(YamlConfiguration yaml, String path, String defaultValue) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        if (raw instanceof String text) {
            return text;
        }

        LOGGER.error("Invalid string '{}' for config key '{}'. Using default '{}'.", raw, path, defaultValue);
        return defaultValue;
    }

    private static Permission.OverlapMode readOverlapMode(
            YamlConfiguration yaml,
            String path,
            Permission.OverlapMode defaultValue
    ) {
        String raw = readString(yaml, path, defaultValue.name());
        try {
            return Permission.OverlapMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            LOGGER.error(
                    "Invalid overlap mode '{}' for config key '{}'. Using default '{}'.",
                    raw,
                    path,
                    defaultValue
            );
            return defaultValue;
        }
    }

    private static Grid.CellSize readCellSize(
            YamlConfiguration yaml,
            String path,
            Grid.CellSize defaultValue
    ) {
        Object raw = yaml.get(path);
        if (raw == null) {
            logMissing(path, defaultValue.getSize());
            return defaultValue;
        }
        try {
            if (raw instanceof Number number) {
                return Grid.CellSize.of(number.intValue());
            }
            if (raw instanceof String text) {
                String normalized = text.trim();
                if (normalized.startsWith("SIZE_")) {
                    normalized = normalized.substring("SIZE_".length());
                }
                return Grid.CellSize.of(Integer.parseInt(normalized));
            }
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Invalid cell size '{}' for config key '{}'. Using default '{}'.",
                    raw,
                    path,
                    defaultValue.getSize()
            );
            return defaultValue;
        }

        LOGGER.error(
                "Invalid cell size '{}' for config key '{}'. Using default '{}'.",
                raw,
                path,
                defaultValue.getSize()
        );
        return defaultValue;
    }

    private static void logMissing(String path, Object defaultValue) {
        LOGGER.error("Missing config key '{}'. Using default '{}'.", path, defaultValue);
    }

    private static Set<Material> immutableLinkedSet(List<Material> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
