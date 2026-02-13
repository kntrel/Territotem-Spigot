package com.kntrel.util;

import com.kntrel.mc.territotem.blueprint.BlueprintElement;
import org.bukkit.util.Vector;

public record Vec3i(int x, int y, int z) implements Comparable<Vec3i> {

    //CONSTANTS
    private static final Vec3i ZEROES = new Vec3i(0, 0, 0);


    //FACTORY
    public static Vec3i zeroes() {
        return ZEROES;
    }
    public static Vec3i min(Vec3i a, Vec3i b) {
        return new Vec3i(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()));
    }
    public static Vec3i max(Vec3i a, Vec3i b) {
        return new Vec3i(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
    }


    public Vec3i add(Vec3i other) {
        return new Vec3i(this.x + other.x, this.y + other.y, this.z + other.z);
    }
    public Vec3i subtract(Vec3i other) {
        return new Vec3i(this.x - other.x, this.y - other.y, this.z - other.z);
    }
    public Vec3i addX(int x) {
        return new Vec3i(this.x + x, this.y, this.z);
    }
    public Vec3i addY(int y) {
        return new Vec3i(this.x, this.y + y, this.z);
    }
    public Vec3i addZ(int z) {
        return new Vec3i(this.x, this.y, this.z + z);
    }
    public Vec3i subtractX(int x) {
        return this.addX(-x);
    }
    public Vec3i subtractY(int y) {
        return this.addY(-y);
    }
    public Vec3i subtractZ(int z) {
        return this.addZ(-z);
    }
    public Vec3i multiply(int scalar) {
        return new Vec3i(this.x * scalar, this.y * scalar, this.z * scalar);
    }
    public Vec3i invert() {
        return this.multiply(-1);
    }
    public Vector toDouble() {
        return new Vector(this.x, this.y, this.z);
    }

    @Override public int compareTo(Vec3i o) {
        int cmp = Integer.compare(this.y(), o.y());
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.z(), o.z());
        if (cmp != 0) return cmp;
        return Integer.compare(this.x(), o.x());
    }

}
