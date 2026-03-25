package com.kntrel.mc.territotem.config;

import com.kntrel.mc.regionLib.util.Grid;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ConfigNode {

    private final Logger logger_;
    private final String path_;
    private final Map<String, Object> values_;

    private ConfigNode(Logger logger, String path, Map<String, Object> values) {
        this.logger_ = logger;
        this.path_ = path;
        this.values_ = values;
    }

    static ConfigNode root(YamlConfiguration yaml, Logger logger) {
        return new ConfigNode(logger, "", normalizeMap(yaml.getValues(false)));
    }

    static ConfigNode fromMap(Map<?, ?> values, Logger logger, String path) {
        return new ConfigNode(logger, path, normalizeMap(values));
    }

    ConfigNode child(String key) {
        Object raw = this.raw(key);
        if (raw instanceof Map<?, ?> map) {
            return ConfigNode.fromMap(map, this.logger_, this.pathOf(key));
        }
        return new ConfigNode(this.logger_, this.pathOf(key), Map.of());
    }

    int intValue(String key, int defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
            return defaultValue;
        }
        Integer parsed = coerceInt(raw);
        if (parsed != null) {
            return parsed;
        }

        this.logger_.error(
                "Invalid integer '{}' for config key '{}'. Using default '{}'.",
                raw,
                this.pathOf(key),
                defaultValue
        );
        return defaultValue;
    }

    long longValue(String key, long defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
            return defaultValue;
        }
        Long parsed = coerceLong(raw);
        if (parsed != null) {
            return parsed;
        }

        this.logger_.error(
                "Invalid long '{}' for config key '{}'. Using default '{}'.",
                raw,
                this.pathOf(key),
                defaultValue
        );
        return defaultValue;
    }

    double doubleValue(String key, double defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
            return defaultValue;
        }
        Double parsed = coerceDouble(raw);
        if (parsed != null) {
            return parsed;
        }

        this.logger_.error(
                "Invalid double '{}' for config key '{}'. Using default '{}'.",
                raw,
                this.pathOf(key),
                defaultValue
        );
        return defaultValue;
    }

    boolean booleanValue(String key, boolean defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
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

        this.logger_.error(
                "Invalid boolean '{}' for config key '{}'. Using default '{}'.",
                raw,
                this.pathOf(key),
                defaultValue
        );
        return defaultValue;
    }

    Material material(String key, Material defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
            return defaultValue;
        }
        if (!(raw instanceof String text)) {
            this.logger_.error(
                    "Invalid material '{}' for config key '{}'. Using default '{}'.",
                    raw,
                    this.pathOf(key),
                    defaultValue
            );
            return defaultValue;
        }

        try {
            return Material.valueOf(text);
        } catch (IllegalArgumentException ex) {
            this.logger_.error(
                    "Invalid material '{}' for config key '{}'. Using default '{}'.",
                    text,
                    this.pathOf(key),
                    defaultValue
            );
            return defaultValue;
        }
    }

    Material blockMaterial(String key, Material defaultValue) {
        Material material = this.material(key, defaultValue);
        try {
            if (material.isBlock()) {
                return material;
            }
        } catch (Throwable ignored) {
            // Material#isBlock() requires a live Bukkit registry on recent Spigot versions.
            return material;
        }

        this.logger_.error(
                "Invalid material '{}' for config key '{}'. Directional selector items must be blocks. Using default '{}'.",
                material,
                this.pathOf(key),
                defaultValue
        );
        return defaultValue;
    }

    Set<Material> materialSet(String key, Set<Material> defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
            return defaultValue;
        }
        if (!(raw instanceof List<?> rawList)) {
            this.logger_.error(
                    "Invalid config value type for key '{}'. Using default '{}'.",
                    this.pathOf(key),
                    defaultValue
            );
            return defaultValue;
        }

        LinkedHashSet<Material> out = new LinkedHashSet<>();
        for (Object element : rawList) {
            if (!(element instanceof String text)) {
                this.logger_.error(
                        "Ignoring non-string entry '{}' in config key '{}'.",
                        element,
                        this.pathOf(key)
                );
                continue;
            }
            try {
                out.add(Material.valueOf(text));
            } catch (IllegalArgumentException ex) {
                this.logger_.error(
                        "Ignoring invalid material '{}' in config key '{}'.",
                        text,
                        this.pathOf(key)
                );
            }
        }
        return Collections.unmodifiableSet(out);
    }

    <T extends Enum<T>> T enumValue(String key, Class<T> type, T defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue);
            return defaultValue;
        }
        if (!(raw instanceof String text)) {
            this.logger_.error(
                    "Invalid enum value '{}' for config key '{}'. Using default '{}'.",
                    raw,
                    this.pathOf(key),
                    defaultValue
            );
            return defaultValue;
        }

        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            this.logger_.error(
                    "Invalid enum value '{}' for config key '{}'. Using default '{}'.",
                    text,
                    this.pathOf(key),
                    defaultValue
            );
            return defaultValue;
        }
    }

    Grid.CellSize cellSize(String key, Grid.CellSize defaultValue) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logMissing(key, defaultValue.getSize());
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
            this.logger_.error(
                    "Invalid cell size '{}' for config key '{}'. Using default '{}'.",
                    raw,
                    this.pathOf(key),
                    defaultValue.getSize()
            );
            return defaultValue;
        }

        this.logger_.error(
                "Invalid cell size '{}' for config key '{}'. Using default '{}'.",
                raw,
                this.pathOf(key),
                defaultValue.getSize()
        );
        return defaultValue;
    }

    @Nullable Material requiredMaterial(String key) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logIgnoredRowMissing(key);
            return null;
        }
        if (!(raw instanceof String text)) {
            this.logger_.error(
                    "Invalid material '{}' for config key '{}'. Ignoring row.",
                    raw,
                    this.pathOf(key)
            );
            return null;
        }
        try {
            return Material.valueOf(text);
        } catch (IllegalArgumentException ex) {
            this.logger_.error(
                    "Invalid material '{}' for config key '{}'. Ignoring row.",
                    text,
                    this.pathOf(key)
            );
            return null;
        }
    }

    @Nullable Integer requiredPositiveInt(String key) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logIgnoredRowMissing(key);
            return null;
        }
        Integer parsed = coerceInt(raw);
        if (parsed != null && parsed >= 1) {
            return parsed;
        }

        this.logger_.error(
                "Invalid positive integer '{}' for config key '{}'. Ignoring row.",
                raw,
                this.pathOf(key)
        );
        return null;
    }

    @Nullable Double requiredNonNegativeDouble(String key) {
        Object raw = this.raw(key);
        if (raw == null) {
            this.logIgnoredRowMissing(key);
            return null;
        }
        Double parsed = coerceDouble(raw);
        if (parsed != null && parsed >= 0d) {
            return parsed;
        }

        this.logger_.error(
                "Invalid non-negative decimal '{}' for config key '{}'. Ignoring row.",
                raw,
                this.pathOf(key)
        );
        return null;
    }

    boolean has(String key) {
        return this.values_.containsKey(key);
    }

    @Nullable Object raw(String key) {
        return this.values_.get(key);
    }

    String pathOf(String key) {
        if (this.path_.isBlank()) {
            return key;
        }
        return this.path_ + "." + key;
    }

    Logger logger() {
        return this.logger_;
    }

    private void logMissing(String key, Object defaultValue) {
        this.logger_.error(
                "Missing config key '{}'. Using default '{}'.",
                this.pathOf(key),
                defaultValue
        );
    }

    private void logIgnoredRowMissing(String key) {
        this.logger_.error("Missing config key '{}'. Ignoring row.", this.pathOf(key));
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> rawMap) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            out.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Object normalizeValue(Object raw) {
        if (raw instanceof ConfigurationSection section) {
            return normalizeMap(section.getValues(false));
        }
        if (raw instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        if (raw instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object value : list) {
                normalized.add(normalizeValue(value));
            }
            return List.copyOf(normalized);
        }
        return raw;
    }

    private static @Nullable Integer coerceInt(Object raw) {
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

    private static @Nullable Long coerceLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static @Nullable Double coerceDouble(Object raw) {
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
