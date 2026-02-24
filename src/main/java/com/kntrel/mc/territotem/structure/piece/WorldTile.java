package com.kntrel.mc.territotem.structure.piece;


import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.util.Vec3i;
import org.bukkit.block.BlockState;
import java.util.List;

public interface WorldTile {

    //CONTRACT
    Vec3i coordinates();
    BlockState block();
    NBTCompound blockNbt();
    List<EntityState> entities();


    //DEFAULTS
    default int x() { return this.coordinates().x(); }
    default int y() { return this.coordinates().y(); }
    default int z() { return this.coordinates().z(); }
}
