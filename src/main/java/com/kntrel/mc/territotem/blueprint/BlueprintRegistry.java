package com.kntrel.mc.territotem.blueprint;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.totem.Totem;
import com.kntrel.util.BitSet3D;
import com.kntrel.util.SetMap;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlueprintRegistry {

    //CONSTANTS
    private static final Map<Long, BitSet3D> BITSET_MAP = new HashMap<>();


    //FIELDS
    private final Plugin plugin_;
    private final RegionContext regionContext_;
    private final Map<Long, Blueprint> blueprintMap_;


    //SUPPORT DATA STRUCTURES
    private final SetMap<Material, Tile> materialTileMap_;
    private final Set<Material> starterMaterials_;
    private final Map<Pair<Long, Vec3i>, Candidate> activeCandidates_;



    //CONSTRUCTORS
    public BlueprintRegistry(Plugin plugin, RegionContext context) {
        this.plugin_ = plugin;
        this.regionContext_ = context;
        this.blueprintMap_ = new HashMap<>();
        this.materialTileMap_ = new SetMap<>();
        this.starterMaterials_ = new HashSet<>();
        this.activeCandidates_ = new HashMap<>();
    }


    //API
    public void register(Blueprint blueprint) {
        if (this.blueprintMap_.containsKey(blueprint.id())) {
            throw new RuntimeException("Blueprint with id " + blueprint.id() + " is already registered");
        }

        boolean keyed = blueprint.hasKeyElements();
        this.blueprintMap_.put(blueprint.id(), blueprint);
        for (BlueprintElement element : blueprint.elements()) switch (element) {
            case BlueprintElement.Block block -> {
                Material material = block.type();
                Vec3i offset = new Vec3i(block.x(), block.y(), block.z());
                this.materialTileMap_.putInto(material, new Tile(blueprint, offset, element));

                if (!keyed || blueprint.isKeyElement(element)) {
                    this.starterMaterials_.add(material);
                }
            }
            default -> {}
        }
    }


    //HELPERS
    private static BitSet3D bitSetFromBlueprint(Blueprint blueprint) {
        BitSet3D bitSet = BITSET_MAP.get(blueprint.id());
        if (bitSet != null) { return bitSet; }

        Vec3i dimensions = blueprint.dimensions();
        bitSet = new BitSet3D(dimensions.x(), dimensions.y(), dimensions.z());

        for (BlueprintElement element : blueprint.elements()) switch (element) {
            case BlueprintElement.Block b -> bitSet.set(b.x(), b.y(), b.z());
            default -> {}
        }

        BITSET_MAP.put(blueprint.id(), bitSet);
        return bitSet;
    }
    private static void newRegion(Entity creator, String name, Totem totem, RegionContext context) {

        Vector vec = totem.regionOrigin();
        BoundingBox base = totem.blueprint().initialRegionBounds();
        BoundingBox regionBox = new BoundingBox(
            vec.getX() + base.getMinX(), vec.getY() + base.getMinY(), vec.getZ() + base.getMinZ(),
            vec.getX() + base.getMaxX(), vec.getY() + base.getMaxY(), vec.getZ() + base.getMaxZ()
        );

        context.create(creator, regionBox, totem.world(), name, totem.blueprint().hierarchy());
    }



    //SUBTYPES
    private record Tile(Blueprint blueprint, Vec3i offset, BlueprintElement element) {}
    private static class Candidate {

        //FIELDS
        private final Blueprint blueprint_;
        private final Vec3i origin_;
        private final BitSet3D baseBitSet_, bitSet_;


        //CONSTRUCTOR
        Candidate(Blueprint blueprint, Vec3i origin) {
            this.blueprint_ = blueprint;
            this.origin_ = origin;
            this.baseBitSet_ = bitSetFromBlueprint(blueprint);
            this.bitSet_ = this.baseBitSet_.clone();
        }


        //GETTERS
        public Blueprint blueprint() { return this.blueprint_; }
        public Vec3i origin() { return this.origin_; }


        //UTILITY
        public void mark(Vec3i offset) {
            this.bitSet_.clear(offset);
        }
        public void unMark(Vec3i offset) {
            if (this.baseBitSet_.get(offset)) {
                this.bitSet_.set(offset);
            }
        }
        public boolean isComplete() {
            return this.bitSet_.isEmpty();
        }
    }
}
