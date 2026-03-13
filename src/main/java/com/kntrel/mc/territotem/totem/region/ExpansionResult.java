package com.kntrel.mc.territotem.totem.region;

public record ExpansionResult(Expansion intended, Expansion accomplished) {

    public ExpansionResult {
        if (intended == null) {
            throw new IllegalArgumentException("intended cannot be null");
        }
        if (accomplished == null) {
            throw new IllegalArgumentException("accomplished cannot be null");
        }
    }

    public boolean hasGrowth() {
        return this.accomplished.total() > 0d;
    }

    public double unachievedTotal() {
        return Math.max(0d, this.intended.total() - this.accomplished.total());
    }
}
