package com.kntrel.mc.state;

import org.bukkit.block.data.BlockData;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class State extends HashMap<String, State.Value> {

    //CONSTANTS
    private static final Pattern BRACKETS = Pattern.compile("\\[(?<inside>[^\\]]*)\\]");


    //FACTORY
    public static State from(String blockState) {
        Matcher m = BRACKETS.matcher(blockState);
        if (!m.find()) { return new State(); }

        String inside = m.group("inside").trim();
        State out = new State();

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

    public static State from(BlockData blockData) {
        return from(blockData.getAsString());
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
    }
}