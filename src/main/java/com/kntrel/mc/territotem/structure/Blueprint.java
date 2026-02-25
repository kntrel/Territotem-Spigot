package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Blueprint {

    //FIELDS
    private final long id_;
    private final String name_;
    private final Map<Vec3i, Tile> pieceMap_;
    private final Vec3i dimensions_;
    private final Hierarchy hierarchy_;
    private final BoundingBox initialRegionBounds_;


    //CONSTRUCTORS
    public Blueprint(
            long id,
            String name,
            Iterable<Tile> elements,
            BoundingBox initialRegionBounds,
            Hierarchy hierarchy,
            Iterable<RuleValue<?>> ruleValues
    ) {
        this.id_ = id;
        this.name_ = name;
        this.hierarchy_ = hierarchy;

        Processed processed = process(elements, initialRegionBounds);
        this.dimensions_ = processed.dimensions();
        this.pieceMap_ = processed.elementMap();
        this.initialRegionBounds_ = processed.initialBounds();
    }


    //API
    public long id() {
        return this.id_;
    }
    public String name() {
        return this.name_;
    }
    public List<Tile> pieces() {
        return this.pieceMap_.entrySet().stream()
                .map(e -> new Tile(e.getKey(), e.getValue()))
                .toList();
    }
    public Vec3i dimensions() {
        return this.dimensions_;
    }
    public Hierarchy hierarchy() {
        return this.hierarchy_;
    }
    public BoundingBox initialRegionBounds() {
        return this.initialRegionBounds_.clone();
    }
    public int elementCount() {
        return this.pieceMap_.size();
    }
    public Piece getByOffset(Vec3i offset) {
        this.checkBounds(offset);
        return this.pieceMap_.getOrDefault(offset, new Tile(offset, Piece.any()));
    }
    public Piece getByOffset(int x, int y, int z) {
        return this.getByOffset(new Vec3i(x, y, z));
    }
    public Map<Vec3i, Tile> piecesByOffset() {
        return this.pieceMap_;
    }
    public boolean isEmptyAt(Vec3i offset) {
        return !this.pieceMap_.containsKey(offset);
    }
    public boolean isEmptyAt(int x, int y, int z) {
        return this.isEmptyAt(new Vec3i(x, y, z));
    }
    public boolean matchesAt(Vec3i offset, WorldTile worldTile) {
        Piece other = this.getByOffset(offset);
        return other != null && other.matches(worldTile);
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
    private static Processed process(Iterable<Tile> tiles, BoundingBox regionBounds) {
        Iterator<Tile> i = tiles.iterator();
        if (!i.hasNext()) {
            throw new InvalidBlueprintException("Blueprint must contain at least one piece");
        }

        List<Tile> trimmed = new ArrayList<>();
        Tile tile = i.next();
        trimmed.add(tile);
        int minX = tile.x(), minY = tile.y(), minZ = tile.z(),
            maxX = tile.x(), maxY = tile.y(), maxZ = tile.z();

        while (i.hasNext()) {
            tile = i.next();
            int x = tile.x(), y = tile.y(), z = tile.z();

            if (x < minX) { minX = x; }
            if (x > maxX) { maxX = x; }
            if (y < minY) { minY = y; }
            if (y > maxY) { maxY = y; }
            if (z < minZ) { minZ = z; }
            if (z > maxZ) { maxZ = z; }

            trimmed.add(tile);
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
            correctedBox = correctedBox.shift(-correction.x(), -correction.y(), -correction.z());
        }

        Map<Vec3i, Tile> elementMap = trimmed.stream().collect(Collectors.toMap(
                Tile::offset,
                Function.identity()
        ));

        Vec3i dimensions = new Vec3i(maxX + 1, maxY + 1, maxZ + 1);

        return new Processed(dimensions, elementMap, correctedBox);
    }

    //SUBTYPES
    private record Processed(Vec3i dimensions, Map<Vec3i, Tile> elementMap, BoundingBox initialBounds) {}
}
