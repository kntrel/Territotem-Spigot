package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.territotem.totem.core.TotemCore;

public record Expansion(double up, double down, double north, double south, double east, double west) {

    public Expansion {
        validate("up", up);
        validate("down", down);
        validate("north", north);
        validate("south", south);
        validate("east", east);
        validate("west", west);
    }

    public static Expansion none() {
        return new Expansion(0, 0, 0, 0, 0, 0);
    }

    public static Expansion all(double amountPerSide) {
        return new Expansion(amountPerSide, amountPerSide, amountPerSide, amountPerSide, amountPerSide, amountPerSide);
    }

    public static Expansion forDirection(TotemCore.Direction direction, double amount) {
        if (direction == TotemCore.Direction.ALL) {
            throw new IllegalArgumentException("Use Expansion.all(...) for omni-directional growth");
        }

        return switch (direction) {
            case UP -> new Expansion(amount, 0, 0, 0, 0, 0);
            case DOWN -> new Expansion(0, amount, 0, 0, 0, 0);
            case NORTH -> new Expansion(0, 0, amount, 0, 0, 0);
            case SOUTH -> new Expansion(0, 0, 0, amount, 0, 0);
            case EAST -> new Expansion(0, 0, 0, 0, amount, 0);
            case WEST -> new Expansion(0, 0, 0, 0, 0, amount);
            case ALL -> throw new IllegalArgumentException("Use Expansion.all(...) for omni-directional growth");
        };
    }

    public double get(TotemCore.Direction direction) {
        return switch (direction) {
            case UP -> this.up;
            case DOWN -> this.down;
            case NORTH -> this.north;
            case SOUTH -> this.south;
            case EAST -> this.east;
            case WEST -> this.west;
            case ALL -> throw new IllegalArgumentException("ALL is not a concrete side");
        };
    }

    public double total() {
        return this.up + this.down + this.north + this.south + this.east + this.west;
    }

    public boolean isZero() {
        return this.total() == 0d;
    }

    public boolean isUniform() {
        return this.up == this.down
                && this.up == this.north
                && this.up == this.south
                && this.up == this.east
                && this.up == this.west;
    }

    private static void validate(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0d) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }
}
