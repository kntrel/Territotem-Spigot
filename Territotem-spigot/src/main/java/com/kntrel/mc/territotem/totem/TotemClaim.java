package com.kntrel.mc.territotem.totem;

import com.google.gson.*;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.util.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public record TotemClaim(UUID structureId, int chunkX, int chunkZ, int deedsVersion) {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemClaim.class);
    static final String DATA_KEY = "totemData";
    private static final JsonSerializer<TotemClaim> SERIALIZER = (src, type, ctx) -> {
        JsonObject object = new JsonObject();
        object.addProperty("structure_id", src.structureId.toString());
        object.addProperty("chunkX", src.chunkX);
        object.addProperty("chunkZ", src.chunkZ);
        object.addProperty("deeds_version", src.deedsVersion);
        return object;
    };
    private static final JsonDeserializer<TotemClaim> DESERIALIZER = (elm, type, ctx) -> {
        if (!elm.isJsonObject()) { return null; }
        JsonObject object = elm.getAsJsonObject();

        JsonElement idElm = object.get("structure_id");
        if (idElm == null || !idElm.isJsonPrimitive()) { return null; }

        JsonElement cxElm = object.get("chunkX");
        if (cxElm == null || !cxElm.isJsonPrimitive()) { return null; }

        JsonElement czElm = object.get("chunkZ");
        if (czElm == null || !cxElm.isJsonPrimitive()) { return null; }

        JsonElement deedsElm = object.get("deeds_version");
        int deedsVer = 0;
        if (deedsElm != null && deedsElm.isJsonPrimitive()) {
            JsonPrimitive deedsPrimitive = deedsElm.getAsJsonPrimitive();
            if (deedsPrimitive.isNumber()) {
                deedsVer = deedsPrimitive.getAsInt();
            }
        }

        UUID id;
        try {
            id = UUID.fromString(idElm.getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }

        int chunkX, chunkZ;
        try {
            chunkX = cxElm.getAsInt();
            chunkZ = czElm.getAsInt();
        } catch (NumberFormatException e) {
            return null;
        }

        return new TotemClaim(id, chunkX, chunkZ, deedsVer);
    };
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TotemClaim.class, TotemClaim.serializer())
            .registerTypeAdapter(TotemClaim.class, TotemClaim.deserializer())
            .create();



    //FACTORY
    public static TotemClaim of(Totem totem) {
        Structure structure = totem.structure();
        Vec3i vec = structure.origin();
        int chunkX = vec.x() >> Constants.CHUNK_SHIFT, chunkZ = vec.z() >> Constants.CHUNK_SHIFT;
        return new TotemClaim(structure.id(), chunkX, chunkZ, totem.getDeedsVersion());
    }


    //UTIL
    public static TotemClaim read(Region region) {
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null || !dataContainer.has(DATA_KEY)) {
            return null;
        }

        RegionData raw = dataContainer.get(DATA_KEY);
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
    public static void write(Region region, TotemClaim claim) {
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null) {
            return;
        }

        dataContainer.remove(DATA_KEY);
        dataContainer.add(new RegionData(DATA_KEY, GSON.toJsonTree(claim)));
        region.save();
    }
    public static JsonSerializer<TotemClaim> serializer() { return SERIALIZER; }
    public static JsonDeserializer<TotemClaim> deserializer() { return DESERIALIZER; }
}

