package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.util.BoundingBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public class RegionAllocator {

    private static final double EPSILON = 1.0E-9;
    private static final List<TotemCore.Direction> SIDES = List.of(
            TotemCore.Direction.UP,
            TotemCore.Direction.DOWN,
            TotemCore.Direction.SOUTH,
            TotemCore.Direction.NORTH,
            TotemCore.Direction.EAST,
            TotemCore.Direction.WEST
    );

    private final RegionRepository regionRepository_;
    private final Condition domainCondition_;

    public RegionAllocator(RegionRepository regionRepository, Condition domainCondition) {
        this.regionRepository_ = Objects.requireNonNull(regionRepository);
        this.domainCondition_ = Objects.requireNonNull(domainCondition);
    }

    public ExpansionResult expand(Region region, Expansion expansion) {
        Objects.requireNonNull(region);
        Objects.requireNonNull(expansion);

        if (expansion.isZero()) {
            return new ExpansionResult(expansion, Expansion.none());
        }

        BoundingBox current = region.getBoundingBox();
        EnumMap<TotemCore.Direction, Double> accomplished = this.newDirectionalMap();

        if (this.isOmnidirectional(expansion)) {
            this.expandOmnidirectional(region, current, expansion, accomplished);
        } else {
            this.expandDirect(region, current, expansion, accomplished);
        }

        Expansion accomplishedExpansion = toExpansion(accomplished);
        if (accomplishedExpansion.total() > EPSILON) {
            region.resize(current);
        }
        return new ExpansionResult(expansion, accomplishedExpansion);
    }

    private void expandDirect(
            Region region,
            BoundingBox current,
            Expansion expansion,
            EnumMap<TotemCore.Direction, Double> accomplished
    ) {
        for (TotemCore.Direction side : SIDES) {
            double requested = expansion.get(side);
            if (requested <= EPSILON) {
                continue;
            }

            double grown = this.grow(region, current, side, requested);
            if (grown > EPSILON) {
                accomplished.put(side, grown);
            }
        }
    }

    private void expandOmnidirectional(
            Region region,
            BoundingBox current,
            Expansion expansion,
            EnumMap<TotemCore.Direction, Double> accomplished
    ) {
        EnumMap<TotemCore.Direction, Double> shortages = this.newDirectionalMap();

        for (TotemCore.Direction side : SIDES) {
            double requested = expansion.get(side);
            double grown = this.grow(region, current, side, requested);
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
            double grown = this.grow(region, current, opposite, shortage);
            if (grown > EPSILON) {
                accomplished.compute(opposite, (ignored, previous) -> previous + grown);
            }
            spill += Math.max(0d, shortage - grown);
        }

        this.redistribute(region, current, accomplished, spill);
    }

    private void redistribute(
            Region region,
            BoundingBox current,
            EnumMap<TotemCore.Direction, Double> accomplished,
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
                double grown = this.grow(region, current, side, share);
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

    private double grow(Region region, BoundingBox current, TotemCore.Direction side, double amount) {
        if (amount <= EPSILON) {
            return 0d;
        }

        double maxAllowed = this.maxAllowedGrowth(region, current, side, amount);
        double achieved = Math.min(amount, maxAllowed);
        if (achieved <= EPSILON) {
            return 0d;
        }

        applyExpansion(current, side, achieved);
        return achieved;
    }

    private double maxAllowedGrowth(Region region, BoundingBox current, TotemCore.Direction side, double requested) {
        BoundingBox candidate = current.clone();
        applyExpansion(candidate, side, requested);

        Condition condition = Condition.in(candidate, region.getWorld()).and(this.domainCondition_);
        Long regionId = region.getId();
        if (regionId != null) {
            condition = condition.and(Condition.notEqual(RegionField.ID, regionId));
        }

        double maxAllowed = requested;
        for (Region other : this.regionRepository_.get(condition)) {
            if (other == region) {
                continue;
            }

            double distance = distanceToTouch(current, other, side);
            if (distance < maxAllowed) {
                maxAllowed = distance;
            }
        }
        return Math.max(0d, maxAllowed);
    }

    private boolean isOmnidirectional(Expansion expansion) {
        return expansion.up() > EPSILON
                && expansion.isUniform();
    }

    private EnumMap<TotemCore.Direction, Double> newDirectionalMap() {
        EnumMap<TotemCore.Direction, Double> out = new EnumMap<>(TotemCore.Direction.class);
        for (TotemCore.Direction side : SIDES) {
            out.put(side, 0d);
        }
        return out;
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
}
