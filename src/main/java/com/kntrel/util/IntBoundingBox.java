package com.kntrel.util;

import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public record IntBoundingBox(Vec3i min, Vec3i max) implements Iterable<Vec3i> {

    public IntBoundingBox(Vec3i min, Vec3i max) {
        this.min = Vec3i.min(min, max);
        this.max = Vec3i.max(min, max);
    }
    public IntBoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        this(new Vec3i(x1, y1, z1), new Vec3i(x2, y2, z2));
    }

    public int minX() { return this.min.x(); }
    public int minY() { return this.min.y(); }
    public int minZ() { return this.min.z(); }
    public int maxX() { return this.max.x(); }
    public int maxY() { return this.max.y(); }
    public int maxZ() { return this.max.z(); }
    public int sizeX() { return this.maxX() - this.minX() + 1; }
    public int sizeY() { return this.maxY() - this.minY() + 1; }
    public int sizeZ() { return this.maxZ() - this.minZ() + 1; }
    public int volume() { return this.sizeX() * this.sizeY() * this.sizeZ(); }
    public Vec3i dimensions() {
        return new Vec3i(this.sizeX(), this.sizeY(), this.sizeZ());
    }

    public boolean contains(Vec3i vec) {
        return     vec.x() >= this.minX()
                && vec.x() <= this.maxX()
                && vec.y() >= this.minY()
                && vec.y() <= this.maxY()
                && vec.z() >= this.minZ()
                && vec.z() <= this.maxZ();
    }

    @Override
    public @NonNull Iterator<Vec3i> iterator() {
        return new IntBoundingBoxIterator(this);
    }

    //ITERATOR
    private static class IntBoundingBoxIterator implements Iterator<Vec3i> {
        private final IntBoundingBox box_;
        private int posX_, posY_, posZ_;

        public IntBoundingBoxIterator(IntBoundingBox box) {
            this.box_ = box;
            this.posX_ = 0;
            this.posY_ = 0;
            this.posZ_ = 0;
        }

        @Override
        public boolean hasNext() {
            return this.posZ_ < this.box_.sizeZ();
        }

        @Override
        public Vec3i next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            Vec3i next = new Vec3i(
                    this.box_.minX() + this.posX_,
                    this.box_.minY() + this.posY_,
                    this.box_.minZ() + this.posZ_
            );
            if (++this.posX_ >= this.box_.sizeX()) {
                this.posX_ = 0;
                if (++this.posY_ >= this.box_.sizeY()) {
                    this.posY_ = 0;
                    this.posZ_++;
                }
            }
            return next;
        }
    }
}
