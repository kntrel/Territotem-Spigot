package com.kntrel.util;

public record Vec2i(int x, int y) implements Comparable<Vec2i> {

    //CONSTANTS
    private static final Vec2i ZEROES = new Vec2i(0, 0);


    //FACTORY
    public static Vec2i zeroes() {
        return ZEROES;
    }
    public static Vec2i min(Vec2i a, Vec2i b) {
        return new Vec2i(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()));
    }
    public static Vec2i max(Vec2i a, Vec2i b) {
        return new Vec2i(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()));
    }


    public Vec2i add(Vec2i other) {
        return new Vec2i(this.x + other.x, this.y + other.y);
    }
    public Vec2i subtract(Vec2i other) {
        return new Vec2i(this.x - other.x, this.y - other.y);
    }
    public Vec2i addX(int x) {
        return new Vec2i(this.x + x, this.y);
    }
    public Vec2i addY(int y) {
        return new Vec2i(this.x, this.y + y);
    }
    public Vec2i subtractX(int x) {
        return this.addX(-x);
    }
    public Vec2i subtractY(int y) {
        return this.addY(-y);
    }
    public Vec2i multiply(int scalar) {
        return new Vec2i(this.x * scalar, this.y * scalar);
    }
    public Vec2i invert() {
        return this.multiply(-1);
    }
    public Vec2i shiftLeft(int shift) {
        return new Vec2i(this.x << shift, this.y << shift);
    }
    public Vec2i shiftRight(int shift) {
        return new Vec2i(this.x >> shift, this.y >> shift);
    }

    @Override public String toString() {
        return String.format("Vec2i(%d, %d)", this.x, this.y);
    }
    @Override public int compareTo(Vec2i o) {
        int cmp = Integer.compare(this.y(), o.y());
        if (cmp != 0) return cmp;
        return Integer.compare(this.x(), o.x());
    }

}
