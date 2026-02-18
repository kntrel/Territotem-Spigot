package com.kntrel.mc.territotem.blueprint;

import com.kntrel.util.Vec3i;

public class BlueprintTile implements Comparable<BlueprintTile> {

    //FIELDS
    private final Vec3i offset_;
    private final BlueprintElement element_;


    //CONSTRUCTOR
    public BlueprintTile(Vec3i offset, BlueprintElement element) {
        this.offset_ = offset;
        this.element_ = element;
    }
    public BlueprintTile(int x, int y, int z, BlueprintElement element) {
        this(new Vec3i(x, y ,z), element);
    }


    //GETTERS
    public int x() { return this.offset_.x(); }
    public int y() { return this.offset_.y(); }
    public int z() { return this.offset_.z(); }
    public Vec3i offset() { return this.offset_; }
    public BlueprintElement element() { return this.element_; }
    public BlueprintTile withOffset(Vec3i offset) { return new BlueprintTile(offset, this.element_); }


    //IMPLEMENTATION
    @Override public int compareTo(BlueprintTile o) {
        return this.offset_.compareTo(o.offset_);
    }
}
