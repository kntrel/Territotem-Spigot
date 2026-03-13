package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.util.BoundingBox;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public sealed interface RegionPlaceResult permits RegionPlaceResult.Unplaceable, RegionPlaceResult.Placed {

    record Unplaceable(List<Region> overlappingRegions) implements RegionPlaceResult {

        public Unplaceable(Collection<Region> overlappingRegions) {
            this(List.copyOf(overlappingRegions));
        }
    }

    record Placed(BoundingBox proposed, BoundingBox resulting, Region region) implements RegionPlaceResult {

        public Placed {
            if (proposed == null) {
                throw new IllegalArgumentException("proposed cannot be null");
            }
            if (resulting == null) {
                throw new IllegalArgumentException("resulting cannot be null");
            }
            if (region == null) {
                throw new IllegalArgumentException("region cannot be null");
            }
            proposed = proposed.clone();
            resulting = resulting.clone();
        }
    }

    static Unplaceable unplaceable(Collection<Region> overlappingRegions) {
        return new Unplaceable(overlappingRegions);
    }

    static Placed placed(BoundingBox proposed, BoundingBox resulting, Region region) {
        return new Placed(proposed, resulting, region);
    }

    default boolean isPlaced() {
        return this instanceof Placed;
    }

    default Optional<Placed> getPlaced() {
        if (this instanceof Placed placed) {
            return Optional.of(placed);
        }
        return Optional.empty();
    }

    default Optional<Unplaceable> getUnplaceable() {
        if (this instanceof Unplaceable unplaceable) {
            return Optional.of(unplaceable);
        }
        return Optional.empty();
    }
}
