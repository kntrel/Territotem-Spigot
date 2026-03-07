package com.kntrel.mc.territotem.totem;

import com.google.gson.*;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.util.Vec3i;
import java.util.UUID;

public record TotemClaim(UUID structureId, int chunkX, int chunkZ) {

    public static TotemClaim of(Totem totem) {
        Structure structure = totem.structure();
        Vec3i vec = structure.origin();
        int chunkX = vec.x() >> Constants.CHUNK_SHIFT, chunkZ = vec.z() >> Constants.CHUNK_SHIFT;
        return new TotemClaim(structure.id(), chunkX, chunkZ);
    }
    public static JsonSerializer<TotemClaim> serializer() { return SERIALIZER; }
    public static JsonDeserializer<TotemClaim> deserializer() { return DESERIALIZER; }


    private static final JsonSerializer<TotemClaim> SERIALIZER = (src, type, ctx) -> {
        JsonObject object = new JsonObject();
        object.addProperty("structure_id", src.structureId.toString());
        object.addProperty("chunkX", src.chunkX);
        object.addProperty("chunkZ", src.chunkZ);
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

        return new TotemClaim(id, chunkX, chunkZ);
    };

}

