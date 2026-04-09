package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public class RegionAllocator {

    private static final double EPSILON = 1.0E-9;
    private static final double MIN_COORDINATE = Integer.MIN_VALUE;
    private static final double MAX_COORDINATE = Integer.MAX_VALUE + 1d;
    private static final List<TotemCore.Direction> SIDES = List.of(
            TotemCore.Direction.UP,
            TotemCore.Direction.DOWN,
            TotemCore.Direction.SOUTH,
            TotemCore.Direction.NORTH,
            TotemCore.Direction.EAST,
            TotemCore.Direction.WEST
    );

    private final RegionContext regionContext_;
    private final Condition domainCondition_;

    public RegionAllocator(RegionContext regionContext, Condition domainCondition) {
        this.regionContext_ = Objects.requireNonNull(regionContext);
        this.domainCondition_ = Objects.requireNonNull(domainCondition);
    }

    public ExpansionResult expand(Region region, Expansion expansion) {
        Objects.requireNonNull(region);
        Objects.requireNonNull(expansion);

        if (expansion.isZero()) {
            return new ExpansionResult(expansion, Expansion.none());
        }

        LinkedHashSet<Region> blockers = new LinkedHashSet<>();
        Allocation allocation = this.allocate(
                region.getBoundingBox(),
                expansion,
                region.getWorld(),
                region.getId(),
                blockers,
                usesRedistribution(expansion)
        );
        if (allocation.result().hasGrowth()) {
            region.resize(allocation.bounds());
        }
        return allocation.result();
    }

    public void contract(Region region, Expansion expansion) {
        Objects.requireNonNull(region);
        Objects.requireNonNull(expansion);

        if (expansion.isZero()) {
            return;
        }

        BoundingBox contracted = region.getBoundingBox().clone();
        applyContraction(contracted, expansion);
        region.resize(contracted);
    }

    public RegionPlaceResult place(World world, BoundingBox bounds, BoundingBox critical, String name, Hierarchy hierarchy) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(bounds);
        Objects.requireNonNull(critical);
        Objects.requireNonNull(name);
        Objects.requireNonNull(hierarchy);

        BoundingBox proposed = bounds.clone();
        BoundingBox anchor = critical.clone();
        if (!proposed.contains(anchor)) {
            throw new IllegalArgumentException("bounds must fully contain the critical bounding box");
        }

        if (!this.isWithinWorldLimits(world, anchor)) {
            return RegionPlaceResult.unplaceable(List.of());
        }

        LinkedHashSet<Region> blockers = new LinkedHashSet<>(this.findCollidingRegions(world, anchor, null));
        if (!blockers.isEmpty()) {
            return RegionPlaceResult.unplaceable(blockers);
        }

        Expansion intended = expansionBetween(anchor, proposed);
        Allocation allocation = this.allocate(anchor, intended, world, null, blockers, true);
        if (allocation.result().unachievedTotal() > EPSILON) {
            return RegionPlaceResult.unplaceable(blockers);
        }

        BoundingBox resulting = allocation.bounds();
        Region placed = this.regionContext_.create(resulting.clone(), world, name, hierarchy);
        return RegionPlaceResult.placed(proposed, resulting, placed);
    }

    private Allocation allocate(
            BoundingBox start,
            Expansion intended,
            World world,
            Long ignoredRegionId,
            Collection<Region> blockers,
            boolean redistributeShortages
    ) {
        BoundingBox current = start.clone();
        EnumMap<TotemCore.Direction, Double> accomplished = this.newDirectionalMap();

        if (redistributeShortages) {
            this.allocateRedistributed(world, current, intended, ignoredRegionId, accomplished, blockers);
        } else {
            this.allocateDirect(world, current, intended, ignoredRegionId, accomplished, blockers);
        }

        return new Allocation(current, new ExpansionResult(intended, toExpansion(accomplished), blockers));
    }

    private void allocateDirect(
            World world,
            BoundingBox current,
            Expansion intended,
            Long ignoredRegionId,
            EnumMap<TotemCore.Direction, Double> accomplished,
            Collection<Region> blockers
    ) {
        for (TotemCore.Direction side : SIDES) {
            double requested = intended.get(side);
            if (requested <= EPSILON) {
                continue;
            }

            double grown = this.grow(world, current, ignoredRegionId, side, requested, blockers);
            if (grown > EPSILON) {
                accomplished.put(side, grown);
            }
        }
    }

    private void allocateRedistributed(
            World world,
            BoundingBox current,
            Expansion intended,
            Long ignoredRegionId,
            EnumMap<TotemCore.Direction, Double> accomplished,
            Collection<Region> blockers
    ) {
        EnumMap<TotemCore.Direction, Double> shortages = this.newDirectionalMap();

        for (TotemCore.Direction side : SIDES) {
            double requested = intended.get(side);
            double grown = this.grow(world, current, ignoredRegionId, side, requested, blockers);
            if (grown > EPSILON) {
                accomplished.put(side, grown);
            }
            shortages.put(side, Math.max(0d, requested - grown));
        }

        double spill = 0d;
        for (TotemCore.Direction side : SIDES) {
            double shortage = shortages.get(side);
            if (shortage <= EPSILON) {
                continue;
            }

            TotemCore.Direction opposite = oppositeOf(side);
            double grown = this.grow(world, current, ignoredRegionId, opposite, shortage, blockers);
            if (grown > EPSILON) {
                accomplished.compute(opposite, (ignored, previous) -> previous + grown);
            }
            spill += Math.max(0d, shortage - grown);
        }

        this.redistribute(world, current, ignoredRegionId, accomplished, blockers, spill);
    }

    private void redistribute(
            World world,
            BoundingBox current,
            Long ignoredRegionId,
            EnumMap<TotemCore.Direction, Double> accomplished,
            Collection<Region> blockers,
            double spill
    ) {
        double remaining = spill;
        List<TotemCore.Direction> freeSides = SIDES;
        while (remaining > EPSILON && !freeSides.isEmpty()) {
            double share = remaining / freeSides.size();
            double nextRemaining = 0d;
            double progress = 0d;
            EnumMap<TotemCore.Direction, Boolean> keepers = new EnumMap<>(TotemCore.Direction.class);

            for (TotemCore.Direction side : freeSides) {
                double grown = this.grow(world, current, ignoredRegionId, side, share, blockers);
                if (grown > EPSILON) {
                    accomplished.compute(side, (ignored, previous) -> previous + grown);
                }

                progress += grown;
                nextRemaining += Math.max(0d, share - grown);
                if (grown + EPSILON >= share) {
                    keepers.put(side, true);
                }
            }

            if (progress <= EPSILON) {
                return;
            }

            remaining = nextRemaining;
            freeSides = keepers.keySet().stream().toList();
        }
    }

    private double grow(
            World world,
            BoundingBox current,
            Long ignoredRegionId,
            TotemCore.Direction side,
            double amount,
            Collection<Region> blockers
    ) {
        if (amount <= EPSILON) {
            return 0d;
        }

        double maxAllowed = this.maxAllowedGrowth(world, current, ignoredRegionId, side, amount, blockers);
        double achieved = Math.min(amount, maxAllowed);
        if (achieved <= EPSILON) {
            return 0d;
        }

        applyExpansion(current, side, achieved);
        return achieved;
    }

    private double maxAllowedGrowth(
            World world,
            BoundingBox current,
            Long ignoredRegionId,
            TotemCore.Direction side,
            double requested,
            Collection<Region> blockers
    ) {
        double maxAllowed = Math.min(requested, this.maxAllowedWorldGrowth(world, current, side));
        if (maxAllowed <= EPSILON) {
            return 0d;
        }

        BoundingBox candidate = current.clone();
        applyExpansion(candidate, side, maxAllowed);

        for (Region other : this.findCollidingRegions(world, candidate, ignoredRegionId)) {
            if (blockers != null) {
                blockers.add(other);
            }

            double distance = distanceToTouch(current, other, side);
            if (distance < maxAllowed) {
                maxAllowed = distance;
            }
        }
        return Math.max(0d, maxAllowed);
    }

    private double maxAllowedWorldGrowth(World world, BoundingBox current, TotemCore.Direction side) {
        WorldLimits limits = worldLimits(world);
        return switch (side) {
            case UP -> Math.max(0d, limits.maxY() - current.getMaxY());
            case DOWN -> Math.max(0d, current.getMinY() - limits.minY());
            case NORTH -> Math.max(0d, current.getMinZ() - limits.minZ());
            case SOUTH -> Math.max(0d, limits.maxZ() - current.getMaxZ());
            case EAST -> Math.max(0d, limits.maxX() - current.getMaxX());
            case WEST -> Math.max(0d, current.getMinX() - limits.minX());
            case ALL -> throw new IllegalArgumentException("ALL is not a concrete side");
        };
    }

    private boolean isWithinWorldLimits(World world, BoundingBox bounds) {
        return worldLimits(world).contains(bounds);
    }

    private static WorldLimits worldLimits(World world) {
        double minX = MIN_COORDINATE;
        double maxX = MAX_COORDINATE;
        double minZ = MIN_COORDINATE;
        double maxZ = MAX_COORDINATE;

        WorldBorder border = world.getWorldBorder();
        if (border != null) {
            Location center = border.getCenter();
            if (center != null) {
                double halfSize = border.getSize() / 2d;
                minX = Math.max(minX, center.getX() - halfSize);
                maxX = Math.min(maxX, center.getX() + halfSize);
                minZ = Math.max(minZ, center.getZ() - halfSize);
                maxZ = Math.min(maxZ, center.getZ() + halfSize);
            }
        }

        return new WorldLimits(
                minX,
                maxX,
                Math.max(world.getMinHeight(), MIN_COORDINATE),
                Math.min(world.getMaxHeight(), MAX_COORDINATE),
                minZ,
                maxZ
        );
    }

    private List<Region> findCollidingRegions(World world, BoundingBox bounds, Long ignoredRegionId) {
        Condition condition = Condition.in(bounds, world).and(this.domainCondition_);
        if (ignoredRegionId != null) {
            condition = condition.and(Condition.notEqual(RegionField.ID, ignoredRegionId));
        }
        RegionRepository repository = this.regionContext_.getRegionRepository();
        return repository.get(condition);
    }

    private EnumMap<TotemCore.Direction, Double> newDirectionalMap() {
        EnumMap<TotemCore.Direction, Double> out = new EnumMap<>(TotemCore.Direction.class);
        for (TotemCore.Direction side : SIDES) {
            out.put(side, 0d);
        }
        return out;
    }

    private static boolean usesRedistribution(Expansion expansion) {
        return expansion.up() > EPSILON && expansion.isUniform();
    }

    private static Expansion expansionBetween(BoundingBox inner, BoundingBox outer) {
        return new Expansion(
                nonNegativeDifference(outer.getMaxY() - inner.getMaxY(), "up"),
                nonNegativeDifference(inner.getMinY() - outer.getMinY(), "down"),
                nonNegativeDifference(inner.getMinZ() - outer.getMinZ(), "north"),
                nonNegativeDifference(outer.getMaxZ() - inner.getMaxZ(), "south"),
                nonNegativeDifference(outer.getMaxX() - inner.getMaxX(), "east"),
                nonNegativeDifference(inner.getMinX() - outer.getMinX(), "west")
        );
    }

    private static double nonNegativeDifference(double value, String side) {
        if (value < -EPSILON) {
            throw new IllegalArgumentException(side + " expansion cannot be negative");
        }
        return Math.max(0d, value);
    }

    private static Expansion toExpansion(EnumMap<TotemCore.Direction, Double> values) {
        return new Expansion(
                values.get(TotemCore.Direction.UP),
                values.get(TotemCore.Direction.DOWN),
                values.get(TotemCore.Direction.NORTH),
                values.get(TotemCore.Direction.SOUTH),
                values.get(TotemCore.Direction.EAST),
                values.get(TotemCore.Direction.WEST)
        );
    }

    private static TotemCore.Direction oppositeOf(TotemCore.Direction direction) {
        return switch (direction) {
            case UP -> TotemCore.Direction.DOWN;
            case DOWN -> TotemCore.Direction.UP;
            case NORTH -> TotemCore.Direction.SOUTH;
            case SOUTH -> TotemCore.Direction.NORTH;
            case EAST -> TotemCore.Direction.WEST;
            case WEST -> TotemCore.Direction.EAST;
            case ALL -> throw new IllegalArgumentException("ALL does not have a single opposite side");
        };
    }

    private static double distanceToTouch(BoundingBox current, Region other, TotemCore.Direction side) {
        BoundingBox obstacle = other.getBoundingBox();
        return switch (side) {
            case UP -> obstacle.getMinY() - current.getMaxY();
            case DOWN -> current.getMinY() - obstacle.getMaxY();
            case NORTH -> current.getMinZ() - obstacle.getMaxZ();
            case SOUTH -> obstacle.getMinZ() - current.getMaxZ();
            case EAST -> obstacle.getMinX() - current.getMaxX();
            case WEST -> current.getMinX() - obstacle.getMaxX();
            case ALL -> throw new IllegalArgumentException("ALL is not a concrete side");
        };
    }

    private static void applyExpansion(BoundingBox boundingBox, TotemCore.Direction side, double amount) {
        switch (side) {
            case UP -> boundingBox.resize(
                    boundingBox.getMinX(),
                    boundingBox.getMinY(),
                    boundingBox.getMinZ(),
                    boundingBox.getMaxX(),
                    boundingBox.getMaxY() + amount,
                    boundingBox.getMaxZ()
            );
            case DOWN -> boundingBox.resize(
                    boundingBox.getMinX(),
                    boundingBox.getMinY() - amount,
                    boundingBox.getMinZ(),
                    boundingBox.getMaxX(),
                    boundingBox.getMaxY(),
                    boundingBox.getMaxZ()
            );
            case NORTH -> boundingBox.resize(
                    boundingBox.getMinX(),
                    boundingBox.getMinY(),
                    boundingBox.getMinZ() - amount,
                    boundingBox.getMaxX(),
                    boundingBox.getMaxY(),
                    boundingBox.getMaxZ()
            );
            case SOUTH -> boundingBox.resize(
                    boundingBox.getMinX(),
                    boundingBox.getMinY(),
                    boundingBox.getMinZ(),
                    boundingBox.getMaxX(),
                    boundingBox.getMaxY(),
                    boundingBox.getMaxZ() + amount
            );
            case EAST -> boundingBox.resize(
                    boundingBox.getMinX(),
                    boundingBox.getMinY(),
                    boundingBox.getMinZ(),
                    boundingBox.getMaxX() + amount,
                    boundingBox.getMaxY(),
                    boundingBox.getMaxZ()
            );
            case WEST -> boundingBox.resize(
                    boundingBox.getMinX() - amount,
                    boundingBox.getMinY(),
                    boundingBox.getMinZ(),
                    boundingBox.getMaxX(),
                    boundingBox.getMaxY(),
                    boundingBox.getMaxZ()
            );
            case ALL -> throw new IllegalArgumentException("ALL is not a concrete side");
        }
    }

    private static void applyContraction(BoundingBox boundingBox, Expansion expansion) {
        double minX = boundingBox.getMinX() + expansion.west();
        double minY = boundingBox.getMinY() + expansion.down();
        double minZ = boundingBox.getMinZ() + expansion.north();
        double maxX = boundingBox.getMaxX() - expansion.east();
        double maxY = boundingBox.getMaxY() - expansion.up();
        double maxZ = boundingBox.getMaxZ() - expansion.south();

        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            throw new IllegalArgumentException("Expansion would contract region into an invalid bounding box");
        }

        boundingBox.resize(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private record Allocation(BoundingBox bounds, ExpansionResult result) {
        private Allocation {
            bounds = bounds.clone();
        }
    }

    private record WorldLimits(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {

        private boolean contains(BoundingBox bounds) {
            return bounds.getMinX() >= this.minX - EPSILON
                    && bounds.getMaxX() <= this.maxX + EPSILON
                    && bounds.getMinY() >= this.minY - EPSILON
                    && bounds.getMaxY() <= this.maxY + EPSILON
                    && bounds.getMinZ() >= this.minZ - EPSILON
                    && bounds.getMaxZ() <= this.maxZ + EPSILON;
        }
    }
}
