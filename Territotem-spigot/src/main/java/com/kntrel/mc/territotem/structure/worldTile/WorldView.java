package com.kntrel.mc.territotem.structure.worldTile;

import org.bukkit.World;
import org.bukkit.NamespacedKey;

import java.util.UUID;

public interface WorldView {

    //FACTORY
    static WorldView of(World world) { return new WorldViewImpl(world); }


    //CONTRACT
    UUID id();
    String name();
    NamespacedKey key();
    World.Environment dimension();
}
