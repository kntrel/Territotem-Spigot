package com.kntrel.mc.territotem.totem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.util.ItemStackInfo;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TotemGrowthHistory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotemGrowthHistory.class);

    static final String DATA_KEY = "totemGrowthHistory";

    private TotemGrowthHistory() {}

    static State of(Totem totem) {
        return new State(totem.baseBounds(), totem.growthHistory());
    }

    static @Nullable State read(Region region) {
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
            State state = deserialize(value);
            if (state != null) {
                return state;
            }
        } catch (Exception ignored) {}

        LOGGER.warn(
                "Region {} has an invalid {} entry. Ignoring stored totem growth history.\nEntry: '{}'",
                region.getId(),
                DATA_KEY,
                value
        );
        return null;
    }

    static void write(Region region, State state) {
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null) {
            return;
        }

        dataContainer.remove(DATA_KEY);
        dataContainer.add(new RegionData(DATA_KEY, serialize(state)));
    }

    static State bootstrap(Totem totem) {
        BoundingBox current = totem.region().getBoundingBox();
        BoundingBox candidate = totem.blueprint().initialRegionBounds().shift(totem.origin().toDouble());
        BoundingBox baseBounds = clipToContainedBounds(current, candidate);
        Expansion seeded = expansionBetween(baseBounds, current);
        if (seeded.isZero()) {
            return new State(baseBounds, List.of());
        }
        return new State(baseBounds, List.of(TotemGrowthEntry.nonRefundable(seeded)));
    }

    private static JsonObject serialize(State state) {
        JsonObject out = new JsonObject();
        out.add("base_bounds", serializeBounds(state.baseBounds()));

        JsonArray history = new JsonArray();
        for (TotemGrowthEntry growth : state.history()) {
            if (growth.expansion().isZero()) {
                continue;
            }
            history.add(serializeGrowthEntry(growth));
        }
        out.add("history", history);
        return out;
    }

    private static @Nullable State deserialize(JsonElement root) {
        if (!root.isJsonObject()) {
            return null;
        }

        JsonObject object = root.getAsJsonObject();
        BoundingBox baseBounds = deserializeBounds(object.get("base_bounds"));
        if (baseBounds == null) {
            return null;
        }

        JsonElement historyElement = object.get("history");
        if (historyElement != null && !historyElement.isJsonArray()) {
            return null;
        }

        List<TotemGrowthEntry> history = new ArrayList<>();
        if (historyElement != null) {
            for (JsonElement entry : historyElement.getAsJsonArray()) {
                TotemGrowthEntry growth = deserializeGrowthEntry(entry);
                if (growth == null) {
                    return null;
                }
                if (!growth.expansion().isZero()) {
                    history.add(growth);
                }
            }
        }

        return new State(baseBounds, history);
    }

    private static JsonObject serializeBounds(BoundingBox bounds) {
        JsonObject out = new JsonObject();
        out.addProperty("min_x", bounds.getMinX());
        out.addProperty("min_y", bounds.getMinY());
        out.addProperty("min_z", bounds.getMinZ());
        out.addProperty("max_x", bounds.getMaxX());
        out.addProperty("max_y", bounds.getMaxY());
        out.addProperty("max_z", bounds.getMaxZ());
        return out;
    }

    private static @Nullable BoundingBox deserializeBounds(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        Double minX = readDouble(object, "min_x");
        Double minY = readDouble(object, "min_y");
        Double minZ = readDouble(object, "min_z");
        Double maxX = readDouble(object, "max_x");
        Double maxY = readDouble(object, "max_y");
        Double maxZ = readDouble(object, "max_z");
        if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) {
            return null;
        }
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            return null;
        }
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static JsonObject serializeExpansion(Expansion expansion) {
        JsonObject out = new JsonObject();
        out.addProperty("up", expansion.up());
        out.addProperty("down", expansion.down());
        out.addProperty("north", expansion.north());
        out.addProperty("south", expansion.south());
        out.addProperty("east", expansion.east());
        out.addProperty("west", expansion.west());
        return out;
    }

    private static @Nullable Expansion deserializeExpansion(JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        Double up = readDouble(object, "up");
        Double down = readDouble(object, "down");
        Double north = readDouble(object, "north");
        Double south = readDouble(object, "south");
        Double east = readDouble(object, "east");
        Double west = readDouble(object, "west");
        if (up == null || down == null || north == null || south == null || east == null || west == null) {
            return null;
        }
        return new Expansion(up, down, north, south, east, west);
    }

    private static JsonObject serializeGrowthEntry(TotemGrowthEntry growth) {
        JsonObject out = new JsonObject();
        out.add("expansion", serializeExpansion(growth.expansion()));

        ItemStackInfo refundStack = growth.refundStack();
        if (refundStack != null) {
            out.add("refund_stack", serializeRefundStack(refundStack));
        }
        out.addProperty("drop_back_rate", growth.dropBackRate());
        return out;
    }

    private static @Nullable TotemGrowthEntry deserializeGrowthEntry(JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        if (!object.has("expansion")) {
            Expansion legacyExpansion = deserializeExpansion(element);
            return (legacyExpansion == null) ? null : TotemGrowthEntry.nonRefundable(legacyExpansion);
        }

        Expansion expansion = deserializeExpansion(object.get("expansion"));
        if (expansion == null) {
            return null;
        }

        ItemStackInfo refundStack = deserializeRefundStack(object.get("refund_stack"));
        Double rawDropBackRate = readDouble(object, "drop_back_rate");
        double dropBackRate = (rawDropBackRate == null) ? 0d : rawDropBackRate;
        if (dropBackRate < 0d || dropBackRate > 1d) {
            return null;
        }
        if (refundStack == null && dropBackRate > 0d) {
            return null;
        }
        return new TotemGrowthEntry(expansion, refundStack, dropBackRate);
    }

    private static @Nullable Double readDouble(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }

        double parsed = value.getAsDouble();
        if (!Double.isFinite(parsed)) {
            return null;
        }
        return parsed;
    }

    private static JsonObject serializeRefundStack(ItemStackInfo refundStack) {
        JsonObject out = new JsonObject();
        out.addProperty("material", refundStack.material().name());
        out.addProperty("amount", refundStack.amount());
        if (refundStack.itemSnbt() != null) {
            out.addProperty("item_snbt", refundStack.itemSnbt());
        }
        return out;
    }

    private static @Nullable ItemStackInfo deserializeRefundStack(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        JsonElement materialElement = object.get("material");
        if (materialElement == null || !materialElement.isJsonPrimitive() || !materialElement.getAsJsonPrimitive().isString()) {
            return null;
        }

        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(materialElement.getAsString());
        } catch (IllegalArgumentException ex) {
            return null;
        }

        Integer amount = readInt(object, "amount");
        if (amount == null || amount < 1) {
            return null;
        }

        JsonElement snbtElement = object.get("item_snbt");
        String itemSnbt = null;
        if (snbtElement != null && !snbtElement.isJsonNull()) {
            if (!snbtElement.isJsonPrimitive() || !snbtElement.getAsJsonPrimitive().isString()) {
                return null;
            }
            itemSnbt = snbtElement.getAsString();
        }

        return new ItemStackInfo(material, amount, itemSnbt);
    }

    private static @Nullable Integer readInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }

        double parsed = value.getAsDouble();
        if (!Double.isFinite(parsed) || parsed != Math.rint(parsed)) {
            return null;
        }
        return (int) parsed;
    }

    private static BoundingBox clipToContainedBounds(BoundingBox current, BoundingBox candidate) {
        if (contains(current, candidate)) {
            return candidate.clone();
        }

        double minX = Math.max(current.getMinX(), candidate.getMinX());
        double minY = Math.max(current.getMinY(), candidate.getMinY());
        double minZ = Math.max(current.getMinZ(), candidate.getMinZ());
        double maxX = Math.min(current.getMaxX(), candidate.getMaxX());
        double maxY = Math.min(current.getMaxY(), candidate.getMaxY());
        double maxZ = Math.min(current.getMaxZ(), candidate.getMaxZ());

        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            return current.clone();
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean contains(BoundingBox outer, BoundingBox inner) {
        return outer.getMinX() <= inner.getMinX()
                && outer.getMinY() <= inner.getMinY()
                && outer.getMinZ() <= inner.getMinZ()
                && outer.getMaxX() >= inner.getMaxX()
                && outer.getMaxY() >= inner.getMaxY()
                && outer.getMaxZ() >= inner.getMaxZ();
    }

    private static Expansion expansionBetween(BoundingBox inner, BoundingBox outer) {
        return new Expansion(
                Math.max(0d, outer.getMaxY() - inner.getMaxY()),
                Math.max(0d, inner.getMinY() - outer.getMinY()),
                Math.max(0d, inner.getMinZ() - outer.getMinZ()),
                Math.max(0d, outer.getMaxZ() - inner.getMaxZ()),
                Math.max(0d, outer.getMaxX() - inner.getMaxX()),
                Math.max(0d, inner.getMinX() - outer.getMinX())
        );
    }

    record State(BoundingBox baseBounds, List<TotemGrowthEntry> history) {
        State {
            Objects.requireNonNull(baseBounds, "baseBounds");
            baseBounds = baseBounds.clone();
            history = (history == null) ? List.of() : List.copyOf(history);
        }

        @Override
        public BoundingBox baseBounds() {
            return this.baseBounds.clone();
        }

        @Override
        public List<TotemGrowthEntry> history() {
            return List.copyOf(this.history);
        }
    }
}
