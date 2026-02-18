package com.kntrel.mc.territotem.blueprint;

import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;

public sealed interface BlueprintElement {
    
    //CONTRACT
    boolean matches(BlueprintElement other);
    boolean matchesAt(Vec3i location, World world);


    //SUB-INTERFACES
    sealed interface EitherOption extends BlueprintElement {}
    sealed interface Negatable extends BlueprintElement {}


    //TYPES
    record Any() implements BlueprintElement {

        @Override
        public boolean matches(BlueprintElement other) { return true; }
        @Override
        public boolean matchesAt(Vec3i location, World world) { return true; }
    }

    record Core(Material type) implements BlueprintElement {
        @Override
        public boolean matches(BlueprintElement other) {
            if (other == null) { return false; }
            if (other == this) { return true; }
            if (other instanceof Core c) {
                return c.type == this.type;
            }
            if (other instanceof Block b) {
                return b.type == this.type;
            }
            return false;
        }

        @Override
        public boolean matchesAt(Vec3i location, World world) {
            Material blockType = world.getBlockAt(location.x(), location.y(), location.z()).getType();
            return this.type() == blockType;
        }
    }

    record Block(Material type) implements BlueprintElement, EitherOption, Negatable {

        @Override
        public boolean matches(BlueprintElement other) {
            return this.equals(other);
        }
        @Override
        public boolean matchesAt(Vec3i location, World world) {
            Material blockType = world.getBlockAt(location.x(), location.y(), location.z()).getType();
            return this.type() == blockType;
        }
    }

    record Either(EitherOption... options) implements BlueprintElement, Negatable {

        @Override
        public boolean matches(BlueprintElement other) {
            if (other == null) { return false; }
            if (this.options.length < 1) { return false; }
            for (BlueprintElement option : this.options()) {
                if (option.matches(other)) { return true; }
            }
            return false;
        }
        @Override
        public boolean matchesAt(Vec3i location, World world) {
            if (this.options.length < 1) { return false; }
            for (BlueprintElement option : this.options()) {
                if (option.matchesAt(location, world)) { return true; }
            }
            return false;
        }
    }

    record Not(Negatable negated) implements BlueprintElement {

        @Override
        public boolean matches(BlueprintElement other) {
            if (other == null) { return false; }
            return !this.negated().matches(other);
        }
        @Override
        public boolean matchesAt(Vec3i location, World world) {
            return !this.negated().matchesAt(location, world);
        }
    }
}
