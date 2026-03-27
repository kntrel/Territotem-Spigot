package com.kntrel.mc.territotem.region;

import com.kntrel.mc.runical.core.placeholder.Translatable;
import com.kntrel.mc.runical.core.placeholder.TranslationProperty;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Translatable
public final class RegionColor {

    //CONSTANTS
    private static final String HEX_PREFIX = "#";
    private static final int RGB_MASK = 0xFFFFFF;
    private static final List<PaletteColor<org.bukkit.ChatColor>> LEGACY_COLORS = List.of(
            new PaletteColor<>(org.bukkit.ChatColor.BLACK, 0x000000),
            new PaletteColor<>(org.bukkit.ChatColor.DARK_BLUE, 0x0000AA),
            new PaletteColor<>(org.bukkit.ChatColor.DARK_GREEN, 0x00AA00),
            new PaletteColor<>(org.bukkit.ChatColor.DARK_AQUA, 0x00AAAA),
            new PaletteColor<>(org.bukkit.ChatColor.DARK_RED, 0xAA0000),
            new PaletteColor<>(org.bukkit.ChatColor.DARK_PURPLE, 0xAA00AA),
            new PaletteColor<>(org.bukkit.ChatColor.GOLD, 0xFFAA00),
            new PaletteColor<>(org.bukkit.ChatColor.GRAY, 0xAAAAAA),
            new PaletteColor<>(org.bukkit.ChatColor.DARK_GRAY, 0x555555),
            new PaletteColor<>(org.bukkit.ChatColor.BLUE, 0x5555FF),
            new PaletteColor<>(org.bukkit.ChatColor.GREEN, 0x55FF55),
            new PaletteColor<>(org.bukkit.ChatColor.AQUA, 0x55FFFF),
            new PaletteColor<>(org.bukkit.ChatColor.RED, 0xFF5555),
            new PaletteColor<>(org.bukkit.ChatColor.LIGHT_PURPLE, 0xFF55FF),
            new PaletteColor<>(org.bukkit.ChatColor.YELLOW, 0xFFFF55),
            new PaletteColor<>(org.bukkit.ChatColor.WHITE, 0xFFFFFF)
    );
    private static final List<PaletteColor<DyeColor>> DYE_COLORS = Stream.of(DyeColor.values())
            .map(color -> new PaletteColor<>(color, color.getColor().asRGB() & RGB_MASK))
            .toList();
    public static final RegionColor DEFAULT = fromLegacyColor(org.bukkit.ChatColor.GREEN);


    //FACTORY
    public static RegionColor ofRgb(int red, int green, int blue) {
        validateChannel(red, "red");
        validateChannel(green, "green");
        validateChannel(blue, "blue");
        return new RegionColor((red << 16) | (green << 8) | blue);
    }
    public static RegionColor ofPackedRgb(int rgb24) {
        if ((rgb24 & ~RGB_MASK) != 0) {
            throw new IllegalArgumentException("Packed RGB values must fit in 24 bits");
        }
        return new RegionColor(rgb24);
    }
    public static RegionColor parseHex(String hex) {
        Objects.requireNonNull(hex, "hex");
        String normalized = normalizeHex(hex);
        try {
            return ofPackedRgb(Integer.parseInt(normalized, 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color '" + hex + "'", e);
        }
    }
    public static RegionColor deserialize(String raw) {
        return parseHex(raw);
    }
    public static RegionColor fromDyeColor(DyeColor dyeColor) {
        Objects.requireNonNull(dyeColor, "dyeColor");
        return ofPackedRgb(dyeColor.getColor().asRGB() & RGB_MASK);
    }
    public static RegionColor fromLegacyColor(org.bukkit.ChatColor legacyColor) {
        Objects.requireNonNull(legacyColor, "legacyColor");
        if (!legacyColor.isColor()) {
            throw new IllegalArgumentException("Legacy chat color must be a color, not a format: " + legacyColor);
        }

        for (PaletteColor<org.bukkit.ChatColor> candidate : LEGACY_COLORS) {
            if (candidate.value() == legacyColor) {
                return ofPackedRgb(candidate.rgb());
            }
        }

        throw new IllegalArgumentException("Unsupported legacy chat color: " + legacyColor);
    }


    //FIELDS
    private final int rgb_;


    //CONSTRUCTOR
    private RegionColor(int rgb) {
        this.rgb_ = rgb;
    }


    //GETTERS
    public int packedRgb() {
        return this.rgb_;
    }
    public int red() {
        return (this.rgb_ >>> 16) & 0xFF;
    }
    public int green() {
        return (this.rgb_ >>> 8) & 0xFF;
    }
    public int blue() {
        return this.rgb_ & 0xFF;
    }
    @TranslationProperty
    public String hex() {
        return HEX_PREFIX + String.format(Locale.ROOT, "%06X", this.rgb_);
    }
    public String serialize() {
        return this.hex();
    }
    public Color bukkitColor() {
        return Color.fromRGB(this.rgb_);
    }
    public net.md_5.bungee.api.ChatColor chatColor() {
        return net.md_5.bungee.api.ChatColor.of(this.hex());
    }
    @TranslationProperty(value = "code", root = true)
    public String minecraftCode() {
        char[] digits = String.format(Locale.ROOT, "%06X", this.rgb_).toCharArray();
        StringBuilder builder = new StringBuilder(14);
        builder.append(org.bukkit.ChatColor.COLOR_CHAR).append('x');
        for (char digit : digits) {
            builder.append(org.bukkit.ChatColor.COLOR_CHAR).append(digit);
        }
        return builder.toString();
    }
    public org.bukkit.ChatColor legacyChatColor() {
        return nearestOf(LEGACY_COLORS).value();
    }
    @TranslationProperty("legacyCode")
    public String legacyCode() {
        return this.legacyChatColor().toString();
    }
    public DyeColor dyeColor() {
        return nearestOf(DYE_COLORS).value();
    }
    public String prefix() {
        return this.minecraftCode();
    }
    public String legacyPrefix() {
        return this.legacyCode();
    }
    public String apply(String text) {
        return this.prefix() + Objects.toString(text, "") + org.bukkit.ChatColor.RESET;
    }


    //IMPLEMENTATION
    @Override public String toString() {
        return this.hex();
    }
    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RegionColor color)) {
            return false;
        }
        return this.rgb_ == color.rgb_;
    }
    @Override public int hashCode() {
        return Integer.hashCode(this.rgb_);
    }


    //SUBTYPES
    private record PaletteColor<T>(T value, int rgb) {}


    //HELPERS
    private static String normalizeHex(String hex) {
        String normalized = hex.trim();
        if (normalized.startsWith(HEX_PREFIX)) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("Hex colors must contain exactly 6 digits");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
    private <T> PaletteColor<T> nearestOf(List<PaletteColor<T>> palette) {
        PaletteColor<T> best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PaletteColor<T> candidate : palette) {
            double distance = weightedDistance(this.rgb_, candidate.rgb());
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return Objects.requireNonNull(best, "palette");
    }
    private static double weightedDistance(int leftRgb, int rightRgb) {
        int leftRed = (leftRgb >>> 16) & 0xFF;
        int leftGreen = (leftRgb >>> 8) & 0xFF;
        int leftBlue = leftRgb & 0xFF;

        int rightRed = (rightRgb >>> 16) & 0xFF;
        int rightGreen = (rightRgb >>> 8) & 0xFF;
        int rightBlue = rightRgb & 0xFF;

        int deltaRed = leftRed - rightRed;
        int deltaGreen = leftGreen - rightGreen;
        int deltaBlue = leftBlue - rightBlue;

        return (0.299d * deltaRed * deltaRed)
                + (0.587d * deltaGreen * deltaGreen)
                + (0.114d * deltaBlue * deltaBlue);
    }
    private static void validateChannel(int value, String channel) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(channel + " channel must be between 0 and 255");
        }
    }
}
