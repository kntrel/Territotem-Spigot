package com.kntrel.mc.territotem.blueprint;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Blueprint {

    //FIELDS
    private final long id_;
    private final Map<Vec3i, BlueprintElement> elementMap_;
    private final Material keyMaterial_;
    private final Set<BlueprintElement> keyElements_;
    private final Vector regionOrigin_;
    private final Vec3i dimensions_;
    private final Hierarchy hierarchy_;
    private final Set<RuleValue<?>> ruleValues_;
    private final BoundingBox initialRegionBounds_;


    //CONSTRUCTORS
    public Blueprint(
            long id,
            Iterable<BlueprintElement> elements,
            @Nullable Material keyMaterial,
            @Nullable Vector regionOrigin,
            BoundingBox initialRegionBounds,
            Hierarchy hierarchy,
            Iterable<RuleValue<?>> ruleValues
    ) {
        this.id_ = id;
        this.keyMaterial_ = keyMaterial;
        this.initialRegionBounds_ = initialRegionBounds;
        this.hierarchy_ = hierarchy;
        this.ruleValues_ = StreamSupport.stream(ruleValues.spliterator(), false).collect(Collectors.toSet());

        Processed processed = process(elements, regionOrigin, keyMaterial);
        this.dimensions_ = processed.dimensions();
        this.keyElements_ = processed.keyElements();
        this.elementMap_ = processed.elementMap();
        this.regionOrigin_ = processed.regionOrigin();
    }


    //API
    public long id() {
        return this.id_;
    }
    public List<BlueprintElement> elements() {
        return this.elementMap_.values().stream().toList();
    }
    public Vec3i dimensions() {
        return this.dimensions_;
    }
    public Vector regionOrigin() {
        return this.regionOrigin_;
    }
    public Hierarchy hierarchy() {
        return this.hierarchy_;
    }
    public Set<RuleValue<?>> ruleValues() {
        return this.ruleValues_;
    }
    public BoundingBox initialRegionBounds() {
        return this.initialRegionBounds_;
    }
    public int elementCount() {
        return this.elementMap_.size();
    }
    public @Nullable BlueprintElement getByOffset(Vec3i offset) {
        return this.elementMap_.get(offset);
    }
    public @Nullable BlueprintElement getByOffset(int x, int y, int z) {
        return this.getByOffset(new Vec3i(x, y, z));
    }
    public Map<Vec3i, BlueprintElement> elementsByOffset() {
        return Collections.unmodifiableMap(this.elementMap_);
    }
    public @Nullable Material keyMaterial() {
        return this.keyMaterial_;
    }
    public boolean hasKeyElements() {
        return this.keyMaterial_ != null;
    }
    public Set<BlueprintElement> keyElements() {
        return this.keyElements_;
    }
    public Set<Vec3i> keyElementOffsets() {
        return this.keyElements_.stream().map(BlueprintElement::offset).collect(Collectors.toSet());
    }
    public Set<Material> containedMaterials() {
        return this.elementMap_.values().stream()
                .filter(e -> e instanceof BlueprintElement.Block)
                .map(e -> ((BlueprintElement.Block) e).type())
                .collect(Collectors.toSet());
    }
    public boolean isEmptyAt(Vec3i offset) {
        return !this.elementMap_.containsKey(offset);
    }
    public boolean isEmptyAt(int x, int y, int z) {
        return this.isEmptyAt(new Vec3i(x, y, z));
    }
    public boolean isKeyElementAt(Vec3i offset) {
        return this.keyElementOffsets().contains(offset);
    }
    public boolean isKeyElement(BlueprintElement element) {
        return     this.hasKeyElements()
                && this.isKeyElementAt(element.offset())
                && element instanceof BlueprintElement.Block b
                && b.type() == this.keyMaterial_;
    }


    //HELPERS
    private static Processed process(Iterable<BlueprintElement> elements, @Nullable Vector regionOrigin, @Nullable Material keyMaterial) {
        Iterator<BlueprintElement> i = elements.iterator();
        if (!i.hasNext()) { return new Processed(Vec3i.zeroes(), Set.of(), Map.of(), regionOrigin); }

        List<BlueprintElement> trimmed = new ArrayList<>();
        Set<BlueprintElement> keyElements = new HashSet<>();
        BlueprintElement elm = i.next();
        trimmed.add(elm);
        int minX = elm.x(), minY = elm.y(), minZ = elm.z(),
            maxX = elm.x(), maxY = elm.y(), maxZ = elm.z();

        while (i.hasNext()) {
            elm = i.next();
            int x = elm.x(), y = elm.y(), z = elm.z();

            if (x < minX) { minX = x; }
            if (x > maxX) { maxX = x; }
            if (y < minY) { minY = y; }
            if (y > maxY) { maxY = y; }
            if (z < minZ) { minZ = z; }
            if (z > maxZ) { maxZ = z; }

            if (keyMaterial != null && elm instanceof BlueprintElement.Block b && b.type() == keyMaterial) {
                keyElements.add(elm);
            }
            trimmed.add(elm);
        }

        if (keyMaterial != null && keyElements.isEmpty()) {
            throw new IllegalArgumentException("Blueprint must contain at least one element with the key material " + keyMaterial);
        }

        Vec3i correction = new Vec3i(minX, minY, minZ);
        Vector correctedOrigin = (regionOrigin != null) ? regionOrigin : correction.toDouble();
        if (!correction.equals(Vec3i.zeroes())) {
            trimmed = trimmed.stream()
                    .map(e ->
                        e.withOffset(e.offset().subtract(correction))
                    )
                    .toList();
            maxX -= correction.x(); maxY -= correction.y(); maxZ -= correction.z();
            correctedOrigin = correctedOrigin.subtract(correction.toDouble());
        }

        Map<Vec3i, BlueprintElement> elementMap = trimmed.stream().collect(Collectors.toMap(
                BlueprintElement::offset,
                e -> e)
        );

        Vec3i dimensions = new Vec3i(maxX + 1, maxY + 1, maxZ + 1);

        return new Processed(dimensions, Collections.unmodifiableSet(keyElements), elementMap, correctedOrigin);
    }


    //SUBTYPES
    private record Processed(Vec3i dimensions, Set<BlueprintElement> keyElements, Map<Vec3i, BlueprintElement> elementMap, Vector regionOrigin) {}
}
