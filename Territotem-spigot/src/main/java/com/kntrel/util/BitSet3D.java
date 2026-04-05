package com.kntrel.util;

import java.util.BitSet;
import java.util.Objects;

public class BitSet3D {

    //FIELDS
    private final int sizeX_, sizeY_, sizeZ_, size_;
    private final BitSet bitSet_;


    //CONSTRUCTORS
    public BitSet3D(int sizeX, int sizeY, int sizeZ) {
        this.sizeX_ = sizeX;
        this.sizeY_ = sizeY;
        this.sizeZ_ = sizeZ;
        this.size_ = sizeX * sizeY * sizeZ;
        this.bitSet_ = new BitSet(sizeX * sizeY * sizeZ);
    }

    public BitSet3D(Vec3i size) {
        this(size.x(), size.y(), size.z());
    }


    //API
    public void set(int x, int y, int z) {
        this.checkBounds(x, y, z);
        this.bitSet_.set(this.index(x, y, z));
    }
    public void set(Vec3i pos) {
        this.set(pos.x(), pos.y(), pos.z());
    }
    public void set(int x, int y, int z, boolean value) {
        this.checkBounds(x, y, z);
        this.bitSet_.set(this.index(x, y, z), value);
    }
    public void set(Vec3i pos, boolean value) {
        this.set(pos.x(), pos.y(), pos.z(), value);
    }
    public void clear(int x, int y, int z) {
        this.bitSet_.clear(this.index(x, y, z));
    }
    public void clear(Vec3i pos) {
        this.clear(pos.x(), pos.y(), pos.z());
    }
    public void clear() {
        this.bitSet_.clear();
    }
    public boolean get(int x, int y, int z) {
        this.checkBounds(x, y, z);
        return this.bitSet_.get(this.index(x, y, z));
    }
    public boolean get(Vec3i pos) {
        return this.get(pos.x(), pos.y(), pos.z());
    }
    public boolean isEmpty() {
        return this.bitSet_.isEmpty();
    }
    public Vec3i size() {
        return new Vec3i(this.sizeX_, this.sizeY_, this.sizeZ_);
    }
    public int flatSize() {
        return this.size_;
    }
    public int cardinality() {
        return this.bitSet_.cardinality();
    }
    public BitSet3D or(BitSet3D other) {
        if (!this.size().equals(other.size())) {
            throw new IllegalArgumentException("BitSet3D sizes must match for OR operation.");
        }
        BitSet3D result = new BitSet3D(this.size());
        result.bitSet_.or(this.bitSet_);
        result.bitSet_.or(other.bitSet_);
        return result;
    }
    public BitSet3D and(BitSet3D other) {
        if (!this.size().equals(other.size())) {
            throw new IllegalArgumentException("BitSet3D sizes must match for AND operation.");
        }
        BitSet3D result = new BitSet3D(this.size());
        result.bitSet_.or(this.bitSet_);
        result.bitSet_.and(other.bitSet_);
        return result;
    }


    //IMPLEMENTATION
    @Override public int hashCode() {
        return Objects.hash(this.sizeX_, this.sizeY_, this.sizeZ_, this.bitSet_);
    }
    @Override public boolean equals(Object o) {
        if (o == null) { return false; }
        if (this == o) { return true; }
        if (!(o instanceof BitSet3D other)) { return false; }
        return     this.sizeX_ == other.sizeX_
                && this.sizeY_ == other.sizeY_
                && this.sizeZ_ == other.sizeZ_
                && this.bitSet_.equals(other.bitSet_);
    }
    @Override public BitSet3D clone() {
        BitSet3D clone = new BitSet3D(this.sizeX_, this.sizeY_, this.sizeZ_);
        clone.bitSet_.or(this.bitSet_);
        return clone;
    }


    //HELPERS
    public void checkBounds(int x, int y, int z) {
        if (x < 0 || x >= this.sizeX_ || y < 0 || y >= this.sizeY_ || z < 0 || z >= this.sizeZ_) {
            throw new IndexOutOfBoundsException(String.format(
                    "Coordinates (%d, %d, %d) are out of bounds for size (%d, %d, %d)",
                    x, y, z,
                    this.sizeX_,
                    this.sizeY_,
                    this.sizeZ_
            ));
        }
    }
    private int index(int x, int y, int z) {
        return x * this.sizeY_ * this.sizeZ_ + y * this.sizeZ_ + z;
    }
}
