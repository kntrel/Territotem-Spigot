package com.kntrel.mc.territotem.config.blueprint;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.totem.TotemBlueprint;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import com.kntrel.util.Vec3i;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TotemBlueprintYamlLoader {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemBlueprintYamlLoader.class);
    private static final int SUPPORTED_VERSION = 1;


    //FIELDS
    private final HierarchyRepository hierarchyRepository_;
    private final RuleRegistry ruleRegistry_;
    private final TotemPieceResolver pieceResolver_;


    //CONSTRUCTOR
    public TotemBlueprintYamlLoader(
            TotemCoreTracker coreTracker,
            HierarchyRepository hierarchyRepository,
            RuleRegistry ruleRegistry
    ) {
        this.hierarchyRepository_ = hierarchyRepository;
        this.ruleRegistry_ = ruleRegistry;
        this.pieceResolver_ = new TotemPieceResolver(coreTracker, LOGGER);
    }

    public TotemBlueprint load(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException ex) {
            throw new BlueprintValidationException(
                    "Could not parse blueprint file '" + file.getAbsolutePath() + "'.",
                    ex
            );
        }
        return this.read(yaml, file.getAbsolutePath());
    }

    TotemBlueprint read(YamlConfiguration yaml, String sourceName) {
        Map<String, Object> root = normalizeMap(yaml.getValues(false));

        int version = requireInt(root.get("version"), sourceName + ".version");
        if (version != SUPPORTED_VERSION) {
            throw new BlueprintValidationException(
                    "Unsupported blueprint version '" + version + "' in '" + sourceName + "'. Expected version " + SUPPORTED_VERSION + "."
            );
        }

        long id = requireLong(root.get("id"), sourceName + ".id");
        String name = requireString(root.get("name"), sourceName + ".name");
        long hierarchyId = requireLong(root.get("hierarchy-id"), sourceName + ".hierarchy-id");
        Hierarchy hierarchy = this.hierarchyRepository_.get(hierarchyId).orElseThrow(() ->
                new BlueprintValidationException(
                        "Unknown hierarchy ID '" + hierarchyId + "' in '" + sourceName + "'."
                )
        );

        BoundingBox initialBounds = parseBounds(root.get("initial-region-bounds"), sourceName + ".initial-region-bounds");
        List<TotemBlueprintRule> rules = this.parseRules(root.get("rules"), sourceName + ".rules");
        Map<String, Piece> palette = parsePalette(root.get("palette"), sourceName + ".palette");
        List<Tile> tiles = parseLayout(root.get("layout"), palette, sourceName + ".layout");

        return new TotemBlueprint(id, name, tiles, initialBounds, hierarchy, rules);
    }

    private Map<String, Piece> parsePalette(@Nullable Object rawPalette, String path) {
        Map<String, Object> paletteMap = requireMap(rawPalette, path);
        LinkedHashMap<String, Piece> palette = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : paletteMap.entrySet()) {
            String symbol = entry.getKey();
            if (symbol.length() != 1) {
                throw new BlueprintValidationException(
                        "Palette symbol '" + symbol + "' at '" + path + "' must be exactly one character."
                );
            }

            Map<String, Object> rawEntry = requireMap(entry.getValue(), path + "." + symbol);
            palette.put(symbol, this.pieceResolver_.resolve(rawEntry, path + "." + symbol));
        }

        if (palette.isEmpty()) {
            throw new BlueprintValidationException("Blueprint palette at '" + path + "' cannot be empty.");
        }
        return Collections.unmodifiableMap(palette);
    }

    private List<Tile> parseLayout(@Nullable Object rawLayout, Map<String, Piece> palette, String path) {
        Map<String, Object> layout = requireMap(rawLayout, path);
        String skip = requireString(layout.get("skip"), path + ".skip");
        if (skip.length() != 1) {
            throw new BlueprintValidationException("Layout skip token at '" + path + ".skip' must be exactly one character.");
        }
        if (palette.containsKey(skip)) {
            throw new BlueprintValidationException(
                    "Layout skip token '" + skip + "' at '" + path + ".skip' cannot also exist in the palette."
            );
        }

        List<Object> layers = requireList(layout.get("layers"), path + ".layers");
        if (layers.isEmpty()) {
            throw new BlueprintValidationException("Layout layers at '" + path + ".layers' cannot be empty.");
        }

        Integer width = null;
        Integer depth = null;
        LinkedHashMap<Vec3i, Tile> tileMap = new LinkedHashMap<>();

        for (int i = 0; i < layers.size(); i++) {
            Map<String, Object> layer = requireMap(layers.get(i), path + ".layers[" + i + "]");
            int y = requireInt(layer.get("y"), path + ".layers[" + i + "].y");
            List<Object> rows = requireList(layer.get("rows"), path + ".layers[" + i + "].rows");
            if (rows.isEmpty()) {
                throw new BlueprintValidationException(
                        "Layout rows at '" + path + ".layers[" + i + "].rows' cannot be empty."
                );
            }

            depth = ensureDimension(depth, rows.size(), path + ".layers[" + i + "].rows", "depth");

            for (int z = 0; z < rows.size(); z++) {
                Object rawRow = rows.get(z);
                if (!(rawRow instanceof String row)) {
                    throw new BlueprintValidationException(
                            "Layout row at '" + path + ".layers[" + i + "].rows[" + z + "]' must be a string."
                    );
                }

                width = ensureDimension(width, row.length(), path + ".layers[" + i + "].rows[" + z + "]", "width");
                for (int x = 0; x < row.length(); x++) {
                    String symbol = String.valueOf(row.charAt(x));
                    if (skip.equals(symbol)) {
                        continue;
                    }

                    Piece piece = palette.get(symbol);
                    if (piece == null) {
                        throw new BlueprintValidationException(
                                "Unknown palette symbol '" + symbol + "' at '" + path + ".layers[" + i + "].rows[" + z + "]'."
                        );
                    }

                    Vec3i offset = new Vec3i(x, y, z);
                    Tile tile = new Tile(offset, piece);
                    if (tileMap.putIfAbsent(offset, tile) != null) {
                        throw new BlueprintValidationException(
                                "Duplicate tile offset '" + offset + "' generated while parsing '" + path + "'."
                        );
                    }
                }
            }
        }

        if (tileMap.isEmpty()) {
            throw new BlueprintValidationException("Layout at '" + path + "' must define at least one tile.");
        }
        return List.copyOf(tileMap.values());
    }

    private List<TotemBlueprintRule> parseRules(@Nullable Object rawRules, String path) {
        if (rawRules == null) {
            return List.of();
        }

        Map<String, Object> map = requireMap(rawRules, path);
        List<TotemBlueprintRule> rules = new ArrayList<>(map.size());

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String ruleName = entry.getKey();
            if (ruleName.isBlank()) {
                throw new BlueprintValidationException("Rule name at '" + path + "' cannot be blank.");
            }

            Rule<?> rule = this.ruleRegistry_.get(ruleName).orElse(null);
            if (rule == null) {
                continue;
            }

            Object value = parseRuleValue(entry.getValue(), rule.valueType(), path + "." + ruleName);
            rules.add(new TotemBlueprintRule(rule.name(), value));
        }

        return List.copyOf(rules);
    }

    private static <T> T parseRuleValue(@Nullable Object rawValue, ValueType<T> valueType, String path) {
        if (rawValue == null) {
            throw new BlueprintValidationException("Rule value at '" + path + "' cannot be null.");
        }
        if (rawValue instanceof Map<?, ?> || rawValue instanceof List<?>) {
            throw new BlueprintValidationException(
                    "Rule value at '" + path + "' must be a scalar compatible with the registered rule type '"
                            + valueType.getType().getSimpleName() + "'."
            );
        }
        Class<T> targetType = valueType.getType();
        if (targetType.isInstance(rawValue)) {
            return targetType.cast(rawValue);
        }

        String serialized = rawValue.toString();
        try {
            return valueType.valueOf(serialized);
        } catch (RuntimeException ex) {
            throw new BlueprintValidationException(
                    "Rule value '" + serialized + "' at '" + path + "' is invalid for registered rule type '"
                            + targetType.getSimpleName() + "'.",
                    ex
            );
        }
    }

    private static BoundingBox parseBounds(@Nullable Object rawBounds, String path) {
        List<Object> vectors = requireList(rawBounds, path);
        if (vectors.size() != 2) {
            throw new BlueprintValidationException(
                    "Bounds at '" + path + "' must contain exactly two 3-value vectors."
            );
        }

        double[] a = parseVector(vectors.get(0), path + "[0]");
        double[] b = parseVector(vectors.get(1), path + "[1]");
        return new BoundingBox(
                Math.min(a[0], b[0]),
                Math.min(a[1], b[1]),
                Math.min(a[2], b[2]),
                Math.max(a[0], b[0]),
                Math.max(a[1], b[1]),
                Math.max(a[2], b[2])
        );
    }

    private static double[] parseVector(@Nullable Object rawVector, String path) {
        List<Object> list = requireList(rawVector, path);
        if (list.size() != 3) {
            throw new BlueprintValidationException(
                    "Vector at '" + path + "' must contain exactly three numeric values."
            );
        }

        return new double[] {
                requireDouble(list.get(0), path + "[0]"),
                requireDouble(list.get(1), path + "[1]"),
                requireDouble(list.get(2), path + "[2]")
        };
    }

    private static int ensureDimension(@Nullable Integer current, int candidate, String path, String label) {
        if (current == null) {
            if (candidate < 1) {
                throw new BlueprintValidationException(
                        "Layout " + label + " at '" + path + "' must be at least 1."
                );
            }
            return candidate;
        }
        if (current != candidate) {
            throw new BlueprintValidationException(
                    "Inconsistent layout " + label + " at '" + path + "'. Expected " + current + " but found " + candidate + "."
            );
        }
        return current;
    }

    private static Map<String, Object> requireMap(@Nullable Object raw, String path) {
        if (raw instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        throw new BlueprintValidationException("Expected a map at '" + path + "'.");
    }

    private static List<Object> requireList(@Nullable Object raw, String path) {
        if (raw instanceof List<?> list) {
            return List.copyOf(list);
        }
        throw new BlueprintValidationException("Expected a list at '" + path + "'.");
    }

    private static String requireString(@Nullable Object raw, String path) {
        if (raw instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new BlueprintValidationException("Expected a non-empty string at '" + path + "'.");
    }

    private static int requireInt(@Nullable Object raw, String path) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        throw new BlueprintValidationException("Expected an integer at '" + path + "'.");
    }

    private static long requireLong(@Nullable Object raw, String path) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        throw new BlueprintValidationException("Expected a long integer at '" + path + "'.");
    }

    private static double requireDouble(@Nullable Object raw, String path) {
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            if (Double.isFinite(value)) {
                return value;
            }
        }
        if (raw instanceof String text) {
            try {
                double value = Double.parseDouble(text.trim());
                if (Double.isFinite(value)) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        throw new BlueprintValidationException("Expected a finite decimal at '" + path + "'.");
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

    private static Object normalizeValue(@Nullable Object raw) {
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

}
