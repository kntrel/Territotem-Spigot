package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class RegionColors {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionColors.class);

    public static final String DATA_KEY = "regionColor";

    private RegionColors() {}

    public static @Nullable RegionColor get(Region region) {
        Objects.requireNonNull(region, "region");
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null || !dataContainer.has(DATA_KEY)) {
            return null;
        }

        RegionData raw = dataContainer.get(DATA_KEY);
        if (raw == null) {
            return null;
        }

        try {
            return RegionColor.deserialize(raw.getAsString());
        } catch (Exception ex) {
            LOGGER.warn(
                    "Region {} has an invalid {} entry. Falling back to default region color.",
                    region.getId(),
                    DATA_KEY,
                    ex
            );
            return null;
        }
    }

    public static RegionColor getOrDefault(Region region) {
        RegionColor color = get(region);
        return (color == null) ? RegionColor.DEFAULT : color;
    }

    public static void set(Region region, RegionColor color) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(color, "color");
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null) {
            return;
        }

        dataContainer.remove(DATA_KEY);
        dataContainer.add(new RegionData(DATA_KEY, color.serialize()));
    }

    public static void clear(Region region) {
        Objects.requireNonNull(region, "region");
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null) {
            return;
        }
        dataContainer.remove(DATA_KEY);
    }

    public static String colorize(Region region, String text) {
        return getOrDefault(region).apply(text);
    }

    public static String displayName(Region region) {
        return colorize(region, region.getName());
    }

    public static String displayName(Region region, String plainName) {
        return colorize(region, plainName);
    }

    public static String displayNameBold(Region region) {
        return displayNameBold(region, region.getName());
    }

    public static String displayNameBold(Region region, String plainName) {
        return getOrDefault(region).prefix() + org.bukkit.ChatColor.BOLD + plainName + org.bukkit.ChatColor.RESET;
    }
}
