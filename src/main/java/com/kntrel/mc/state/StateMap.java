package com.kntrel.mc.state;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StateMap extends HashMap<String, StateMap.Value> {

    //CONSTANTS
    private static final Pattern BRACKETS = Pattern.compile("\\[(?<inside>[^\\]]*)\\]");


    //FACTORY
    public static StateMap from(String blockState) {
        Matcher m = BRACKETS.matcher(blockState);
        if (!m.find()) { return new StateMap(); }

        String inside = m.group("inside").trim();
        StateMap out = new StateMap();

        if (inside.isEmpty()) { return out; }

        for (String entry : inside.split(",")) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) { throw new InvalidStateException(blockState); }

            String key = parts[0].trim();
            String val = parts[1].trim();

            if (key.isEmpty()) { throw new InvalidStateException(blockState); };

            out.put(key, new Value(val));
        }

        return out;
    }

    public static StateMap from(BlockData blockData) {
        return from(blockData.getAsString());
    }
    @SafeVarargs public static StateMap of(Map.Entry<String, String>... entries) {
        StateMap out = new StateMap();
        for (Map.Entry<String, String> entry : entries) {
            out.put(entry.getKey(), new Value(entry.getValue()));
        }
        return out;
    }


    //EXTENSION
    public BlockData createBlockData(Material blockType) {
        String props = this.entrySet().stream()
                .map(e -> e.getKey().trim() + "=" + e.getValue().getAsString().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));

        String full = blockType.getKeyOrThrow() + (props.isEmpty() ? "" : "[" + props + "]");

        return Bukkit.createBlockData(full);
    }
    public Value put(String key, String val) {
        return this.put(key, new Value(val));
    }


    //SUBTYPES
    public static class Value {

        private final String val_;
        private final Number num_;

        public Value(String value) {
            Number num;
            try {
                double d = Double.parseDouble(value);
                num = (Double.isInfinite(d) || Double.isNaN(d)) ? null : d;
            } catch (NumberFormatException ignored) {
                num = null;
            }
            this.num_ = num;
            this.val_ = value;
        }

        public String getAsString() {
            return this.val_;
        }
        public boolean isNumeric() {
            return this.num_ != null;
        }
        public Number getAsNumber() {
            return (this.num_ != null) ? this.num_ : Double.NaN;
        }
        public boolean getAsBoolean() {
            return Boolean.parseBoolean(this.val_);
        }
    }
}