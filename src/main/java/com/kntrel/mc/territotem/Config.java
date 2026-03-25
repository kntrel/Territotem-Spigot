package com.kntrel.mc.territotem;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.NBTList;
import com.kntrel.mc.nbt.NBTTag;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.util.Grid;
import com.kntrel.mc.territotem.region.RegionEnterTitleConfig;
import com.kntrel.mc.territotem.region.RegionFeatureMaterialsConfig;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
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
        ExpansionTable expansionTable,
        Set<Material> allowedDeedsRequestItems,
        RegionContextConfig regionContextConfig,
        RegionEnterTitleConfig regionEnterTitle,
        RegionFeatureMaterialsConfig regionFeatureMaterials
) {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private static final Config DEFAULT = new Config(
            Material.AMETHYST_BLOCK,
            new ExpansionTable(List.of(
                    new ExpansionTable.Row(
                            Material.DIAMOND,
                            1,
                            null,
                            6d,
                            6d
                    )
            )),
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
        return read(yaml);
    }

    static Config read(YamlConfiguration yaml) {
        Material directionalSelectorItem = readBlockMaterial(
                yaml,
                "totem.directional_selector_item",
                DEFAULT.directionalSelectorItem()
        );
        ExpansionTable expansionTable = readExpansionTable(
                yaml,
                "totem.expansion",
                DEFAULT.expansionTable()
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
                expansionTable,
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
        try {
            if (material.isBlock()) {
                return material;
            }
        } catch (Throwable ignored) {
            // Material#isBlock() requires a live Bukkit registry on recent Spigot versions.
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

    private static ExpansionTable readExpansionTable(
            YamlConfiguration yaml,
            String path,
            ExpansionTable defaultValue
    ) {
        Object raw = yaml.get(path);
        if (raw == null) {
            LOGGER.error("Missing config key '{}'. Using default expansion table.", path);
            return defaultValue;
        }
        if (!(raw instanceof List<?> rawList)) {
            LOGGER.error("Invalid config value type for key '{}'. Using default expansion table.", path);
            return defaultValue;
        }

        List<ExpansionTable.Row> rows = new java.util.ArrayList<>();
        int index = 0;
        for (Object element : rawList) {
            String rowPath = path + "[" + index + "]";
            index++;

            java.util.Map<?, ?> rowMap;
            if (element instanceof org.bukkit.configuration.ConfigurationSection section) {
                rowMap = section.getValues(false);
            } else if (element instanceof java.util.Map<?, ?> rawMap) {
                rowMap = rawMap;
            } else {
                LOGGER.error("Ignoring non-section expansion row at '{}'.", rowPath);
                continue;
            }

            Material item = readMaterial(rowMap, rowPath + ".item");
            if (item == null) {
                continue;
            }

            Integer consumption = readPositiveInt(rowMap, rowPath + ".consume");
            Double min = readNonNegativeDouble(rowMap, rowPath + ".min");
            Double max = readNonNegativeDouble(rowMap, rowPath + ".max");
            if (consumption == null || min == null || max == null) {
                continue;
            }
            if (min > max) {
                LOGGER.error(
                        "Ignoring expansion row at '{}'. 'min' ({}) must be <= 'max' ({}).",
                        rowPath,
                        min,
                        max
                );
                continue;
            }

            NBTCompound nbt = null;
            if (rowMap.containsKey("nbt")) {
                nbt = readNbtCompound(rowMap, rowPath + ".nbt");
            }
            if (rowMap.containsKey("nbt") && nbt == null) {
                continue;
            }

            rows.add(new ExpansionTable.Row(item, consumption, nbt, min, max));
        }
        return new ExpansionTable(rows);
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

    private static Material readMaterial(java.util.Map<?, ?> section, String path) {
        Object raw = section.get(keyName(path));
        if (raw == null) {
            LOGGER.error("Missing config key '{}'. Ignoring row.", path);
            return null;
        }
        if (!(raw instanceof String text)) {
            LOGGER.error("Invalid material '{}' for config key '{}'. Ignoring row.", raw, path);
            return null;
        }
        try {
            return Material.valueOf(text);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Invalid material '{}' for config key '{}'. Ignoring row.", text, path);
            return null;
        }
    }

    private static Integer readPositiveInt(java.util.Map<?, ?> section, String path) {
        Object raw = section.get(keyName(path));
        if (raw == null) {
            LOGGER.error("Missing config key '{}'. Ignoring row.", path);
            return null;
        }
        Integer value = coerceInt(raw);
        if (value == null || value < 1) {
            LOGGER.error("Invalid positive integer '{}' for config key '{}'. Ignoring row.", raw, path);
            return null;
        }
        return value;
    }

    private static Double readNonNegativeDouble(java.util.Map<?, ?> section, String path) {
        Object raw = section.get(keyName(path));
        if (raw == null) {
            LOGGER.error("Missing config key '{}'. Ignoring row.", path);
            return null;
        }
        Double value = coerceDouble(raw);
        if (value == null || value < 0d) {
            LOGGER.error("Invalid non-negative decimal '{}' for config key '{}'. Ignoring row.", raw, path);
            return null;
        }
        return value;
    }

    private static NBTCompound readNbtCompound(java.util.Map<?, ?> section, String path) {
        Object raw = section.get(keyName(path));
        if (raw instanceof org.bukkit.configuration.ConfigurationSection configurationSection) {
            raw = configurationSection.getValues(false);
        }
        if (raw == null) {
            LOGGER.error("Invalid null NBT value for config key '{}'. Expected a map. Ignoring row.", path);
            return null;
        }
        if (!(raw instanceof java.util.Map<?, ?> rawMap)) {
            LOGGER.error("Invalid NBT value '{}' for config key '{}'. Expected a map. Ignoring row.", raw, path);
            return null;
        }

        NBTTag tag = toNbt(rawMap, path);
        if (tag instanceof NBTCompound compound) {
            return compound;
        }

        LOGGER.error("Invalid NBT value '{}' for config key '{}'. Expected a compound. Ignoring row.", raw, path);
        return null;
    }

    private static NBTTag toNbt(Object raw, String path) {
        if (raw instanceof org.bukkit.configuration.ConfigurationSection section) {
            raw = section.getValues(false);
        }

        if (raw instanceof java.util.Map<?, ?> rawMap) {
            NBTCompound compound = NBTCompound.create();
            for (var entry : rawMap.entrySet()) {
                Object key = entry.getKey();
                if (key == null) {
                    LOGGER.error("Invalid null NBT key at '{}'.", path);
                    return null;
                }
                NBTTag child = toNbt(entry.getValue(), path + "." + key);
                if (child == null) {
                    return null;
                }
                compound.put(key.toString(), child);
            }
            return compound;
        }

        if (raw instanceof List<?> rawList) {
            NBTList list = NBTList.create();
            int index = 0;
            for (Object element : rawList) {
                NBTTag child = toNbt(element, path + "[" + index + "]");
                if (child == null) {
                    return null;
                }
                list.add(child);
                index++;
            }
            return list;
        }

        if (raw == null) {
            LOGGER.error("Invalid null NBT value at '{}'.", path);
            return null;
        }

        try {
            return NBTTag.asTag(raw);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Invalid NBT value '{}' at '{}'.", raw, path);
            return null;
        }
    }

    private static String keyName(String path) {
        int index = path.lastIndexOf('.');
        return (index < 0) ? path : path.substring(index + 1);
    }

    private static Integer coerceInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double coerceDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
