package com.kntrel.mc.territotem.blueprint;

import com.kntrel.util.Vec3i;
import org.bukkit.Material;

public sealed abstract class BlueprintElement implements Comparable<BlueprintElement> {

    //FIELDS
    private final Vec3i offset_;


    //CONSTRUCTORS
    public BlueprintElement(Vec3i offset) {
        this.offset_ = offset;
    }

    public BlueprintElement(int x, int y, int z) {
        this(new Vec3i(x, y, z));
    }


    //DEFAULTS
    public int x() {
        return this.offset_.x();
    }

    public int y() {
        return this.offset_.y();
    }

    public int z() {
        return this.offset_.z();
    }

    public Vec3i offset() {
        return this.offset_;
    }

    public BlueprintElement withOffset(int x, int y, int z) {
        return this.withOffset(new Vec3i(x, y, z));
    }


    //CONTRACT
    public abstract BlueprintElement withOffset(Vec3i offset);


    //IMPLEMENTATION
    @Override
    public int compareTo(BlueprintElement o) {
        return this.offset_.compareTo(o.offset_);
    }


    //TYPES
    public final static class Block extends BlueprintElement {

        //FIELDS
        private final Material type_;


        //CONSTRUCTORS
        public Block(Vec3i offset, Material type) {
            super(offset);
            this.type_ = type;
        }

        public Block(int x, int y, int z, Material type) {
            this(new Vec3i(x, y, z), type);
        }


        //GETTERS
        public Material type() {
            return this.type_;
        }


        //IMPLEMENTATION
        @Override
        public BlueprintElement withOffset(Vec3i offset) {
            return new Block(offset, this.type_);
        }
    }
}
