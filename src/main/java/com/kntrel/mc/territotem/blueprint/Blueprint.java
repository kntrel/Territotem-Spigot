package com.kntrel.mc.territotem.blueprint;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Blueprint {

    //FIELDS
    private final long id_;
    private final String name_;
    private final Map<Vec3i, BlueprintElement> elementMap_;
    private final BlueprintCoreTile core_;
    private final Vec3i dimensions_;
    private final Hierarchy hierarchy_;
    private final Set<RuleValue<?>> ruleValues_;
    private final BoundingBox initialRegionBounds_;
    private final Set<Material> materials_;
    private final Set<EntityType> entityTypes_;


    //CONSTRUCTORS
    public Blueprint(
            long id,
            String name,
            Iterable<BlueprintTile> elements,
            BoundingBox initialRegionBounds,
            Hierarchy hierarchy,
            Iterable<RuleValue<?>> ruleValues
    ) {
        this.id_ = id;
        this.name_ = name;
        this.hierarchy_ = hierarchy;
        this.ruleValues_ = StreamSupport.stream(ruleValues.spliterator(), false).collect(Collectors.toSet());

        Processed processed = process(elements, initialRegionBounds);
        this.dimensions_ = processed.dimensions();
        this.core_ = processed.core();
        this.elementMap_ = processed.elementMap();
        this.initialRegionBounds_ = processed.initialBounds();

        Pair<Stream<Material>, Stream<EntityType>> distinct = distinctTypes(this.elementMap_.values());
        this.materials_ = distinct.first().collect(Collectors.toSet());
        this.entityTypes_ = distinct.second().collect(Collectors.toSet());
    }


    //API
    public long id() {
        return this.id_;
    }
    public String name() {
        return this.name_;
    }
    public List<BlueprintTile> elements() {
        return this.elementMap_.entrySet().stream()
                .map(e -> new BlueprintTile(e.getKey(), e.getValue()))
                .toList();
    }
    public BlueprintCoreTile core() {
        return this.core_;
    }
    public Vec3i dimensions() {
        return this.dimensions_;
    }
    public Hierarchy hierarchy() {
        return this.hierarchy_;
    }
    public Set<RuleValue<?>> ruleValues() {
        return this.ruleValues_;
    }
    public BoundingBox initialRegionBounds() {
        return this.initialRegionBounds_.clone();
    }
    public int elementCount() {
        return this.elementMap_.size();
    }
    public BlueprintElement getByOffset(Vec3i offset) {
        this.checkBounds(offset);
        return this.elementMap_.getOrDefault(offset, new BlueprintElement.Any());
    }
    public BlueprintElement getByOffset(int x, int y, int z) {
        return this.getByOffset(new Vec3i(x, y, z));
    }
    public Map<Vec3i, BlueprintElement> elementsByOffset() {
        return this.elementMap_;
    }
    public Set<Material> containedMaterials() {
        return this.materials_;
    }
    public Set<EntityType> containedEntityTypes() {
        return this.entityTypes_;
    }
    public boolean isEmptyAt(Vec3i offset) {
        return !this.elementMap_.containsKey(offset);
    }
    public boolean isEmptyAt(int x, int y, int z) {
        return this.isEmptyAt(new Vec3i(x, y, z));
    }
    public boolean matchesAt(Vec3i offset, BlueprintElement element) {
        BlueprintElement other = this.getByOffset(offset);
        return other != null && other.matches(element);
    }
    public boolean matchesAt(int x, int y, int z, BlueprintElement element) {
        return this.matchesAt(new Vec3i(x, y, z), element);
    }


    //HELPERS
    private void checkBounds(Vec3i offset) {
        if (!(   offset.x() >= 0 && offset.x() < this.dimensions_.x()
              && offset.y() >= 0 && offset.y() < this.dimensions_.y()
              && offset.z() >= 0 && offset.z() < this.dimensions_.z()
        )) {
            throw new IndexOutOfBoundsException("Offset " + offset + " is out of bounds for blueprint dimensions " + this.dimensions_);
        }
    }
    private static Processed process(Iterable<BlueprintTile> elements, BoundingBox regionBounds) {
        Iterator<BlueprintTile> i = elements.iterator();
        if (!i.hasNext()) {
            throw new InvalidBlueprintException("Blueprint must contain at least one element");
        }

        List<BlueprintTile> trimmed = new ArrayList<>();
        BlueprintTile tile = i.next();
        trimmed.add(tile);
        int minX = tile.x(), minY = tile.y(), minZ = tile.z(),
            maxX = tile.x(), maxY = tile.y(), maxZ = tile.z();
        BlueprintCoreTile core = (tile.element() instanceof BlueprintElement.Core c) ? new BlueprintCoreTile(tile.offset(), c) : null;
        boolean containsBlocks = tile.element() instanceof BlueprintElement.Block;

        while (i.hasNext()) {
            tile = i.next();
            int x = tile.x(), y = tile.y(), z = tile.z();

            if (x < minX) { minX = x; }
            if (x > maxX) { maxX = x; }
            if (y < minY) { minY = y; }
            if (y > maxY) { maxY = y; }
            if (z < minZ) { minZ = z; }
            if (z > maxZ) { maxZ = z; }

            BlueprintElement elm = tile.element();
            if (elm instanceof BlueprintElement.Block) { containsBlocks = true; }

            if (elm instanceof BlueprintElement.Core c) {
                if (core != null) {
                    throw new InvalidBlueprintException("Blueprint cannot contain more than one core element");
                }
                core = new BlueprintCoreTile(tile.offset(), c);
            }

            if (!(elm instanceof BlueprintElement.Any)) { trimmed.add(tile); }
        }

        if (!containsBlocks) {
            throw new InvalidBlueprintException("Blueprint must contain at least one block negated");
        }
        if (core == null) {
            throw new InvalidBlueprintException("Blueprint must contain one core element");
        }

        Vec3i correction = new Vec3i(minX, minY, minZ);
        BoundingBox correctedBox = regionBounds;
        if (!correction.equals(Vec3i.zeroes())) {
            trimmed = trimmed.stream()
                    .map(e ->
                        e.withOffset(e.offset().subtract(correction))
                    )
                    .toList();
            maxX -= correction.x(); maxY -= correction.y(); maxZ -= correction.z();
            core = core.withOffset(core.offset().subtract(correction));
            correctedBox = correctedBox.shift(-correction.x(), -correction.y(), -correction.z());
        }

        Map<Vec3i, BlueprintElement> elementMap = trimmed.stream().collect(Collectors.toMap(
                BlueprintTile::offset,
                BlueprintTile::element
        ));

        Vec3i dimensions = new Vec3i(maxX + 1, maxY + 1, maxZ + 1);

        return new Processed(dimensions, core, elementMap, correctedBox);
    }
    private static Pair<Stream<Material>, Stream<EntityType>> distinctTypes(BlueprintElement element) {
        return switch (element) {
            case BlueprintElement.Block block -> Pair.of(Stream.of(block.type()), Stream.empty());
            //case BlueprintElement.Entity entity -> Pair.of(Stream.empty(), Stream.of(entity.type()));
            case BlueprintElement.Either either -> distinctTypes(Arrays.asList(either.options()));
            default -> Pair.of(Stream.empty(), Stream.empty());
        };
    }
    private static Pair<Stream<Material>, Stream<EntityType>> distinctTypes(Iterable<? extends BlueprintElement> elements) {
        List<Stream<Material>> materials = new ArrayList<>();
        List<Stream<EntityType>> entities = new ArrayList<>();
        for (BlueprintElement elm : elements) {
            Pair<Stream<Material>, Stream<EntityType>> pair = distinctTypes(elm);
            materials.add(pair.first());
            entities.add(pair.second());
        }

        return Pair.of(
            materials.stream().flatMap(Function.identity()),
            entities.stream().flatMap(Function.identity())
        );
    }


    //SUBTYPES
    private record Processed(Vec3i dimensions, BlueprintCoreTile core, Map<Vec3i, BlueprintElement> elementMap, BoundingBox initialBounds) {}
}
