package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.regionLib.region.Region;

import java.util.Collection;
import java.util.List;

public record ExpansionResult(Expansion intended, Expansion accomplished, List<Region> blockingRegions) {

    public ExpansionResult(Expansion intended, Expansion accomplished) {
        this(intended, accomplished, List.of());
    }

    public ExpansionResult {
        if (intended == null) {
            throw new IllegalArgumentException("intended cannot be null");
        }
        if (accomplished == null) {
            throw new IllegalArgumentException("accomplished cannot be null");
        }
        blockingRegions = (blockingRegions == null) ? List.of() : List.copyOf(blockingRegions);
    }

    public ExpansionResult(Expansion intended, Expansion accomplished, Collection<Region> blockingRegions) {
        this(intended, accomplished, List.copyOf(blockingRegions));
    }

    public boolean hasGrowth() {
        return this.accomplished.total() > 0d;
    }

    public double unachievedTotal() {
        return Math.max(0d, this.intended.total() - this.accomplished.total());
    }
}
