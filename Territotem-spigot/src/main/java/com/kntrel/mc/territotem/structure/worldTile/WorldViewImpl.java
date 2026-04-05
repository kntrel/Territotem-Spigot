package com.kntrel.mc.territotem.structure.worldTile;

import org.bukkit.World;
import org.bukkit.NamespacedKey;

import java.util.UUID;

class WorldViewImpl implements WorldView {

    //FIELDS
    private final World world_;


    //CONSTRUCTOR
    WorldViewImpl(World world) {
        this.world_ = world;
    }


    //IMPLEMENTATION
    @Override public UUID id() {
        return this.world_.getUID();
    }
    @Override public String name() {
        return this.world_.getName();
    }
    @Override public NamespacedKey key() {
        return this.world_.getKey();
    }
    @Override public World.Environment dimension() {
        return this.world_.getEnvironment();
    }
}
