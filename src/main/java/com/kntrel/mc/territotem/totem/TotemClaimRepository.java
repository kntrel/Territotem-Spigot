package com.kntrel.mc.territotem.totem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TotemClaimRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotemClaimRepository.class);
    static final String TOTEM_DATA_KEY = "totemData";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TotemClaim.class, TotemClaim.serializer())
            .registerTypeAdapter(TotemClaim.class, TotemClaim.deserializer())
            .create();

    public TotemClaim read(Region region) {
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null || !dataContainer.has(TOTEM_DATA_KEY)) {
            return null;
        }

        RegionData raw = dataContainer.get(TOTEM_DATA_KEY);
        if (raw == null) {
            return null;
        }

        JsonElement value = raw.getValue();
        if (value == null || value.isJsonNull()) {
            return null;
        }

        try {
            TotemClaim claim = GSON.fromJson(value, TotemClaim.class);
            if (claim != null) {
                return claim;
            }
        } catch (Exception ignored) {}

        LOGGER.warn(
                "Region {} is totemized, but its totemData entry is corrupted or invalid. Destroying\nEntry: '{}'",
                region.getId(),
                value
        );
        region.destroy();
        return null;
    }

    public void write(Region region, TotemClaim claim) {
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null) {
            return;
        }

        dataContainer.remove(TOTEM_DATA_KEY);
        dataContainer.add(new RegionData(TOTEM_DATA_KEY, GSON.toJsonTree(claim)));
        region.save();
    }
}
