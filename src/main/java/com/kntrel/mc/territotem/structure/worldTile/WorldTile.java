package com.kntrel.mc.territotem.structure.worldTile;


import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.state.StateMap;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;

public interface WorldTile {

    //FACTORY
    static WorldTile of(Vec3i coordinates, World world) { return new WorldTileImpl(coordinates, world); }


    //CONTRACT
    Vec3i coordinates();
    Material blockType();
    StateMap blockState();
    NBTCompound blockNbt();
    List<EntityState> entities();


    //DEFAULTS
    default int x() { return this.coordinates().x(); }
    default int y() { return this.coordinates().y(); }
    default int z() { return this.coordinates().z(); }
}
