package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.nbt.NBTCompound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import java.util.UUID;

public interface EntityState {

    //CONTRACT
    Class<? extends Entity> entityClass();
    EntityType type();
    UUID id();
    Location location();
    NBTCompound nbt();

}
