package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.impl.RTagNBTCompound;
import com.saicone.rtag.RtagEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.UUID;

record EntityStateImpl(
        Class<? extends Entity> entityClass,
        EntityType type,
        UUID id,
        Location location,
        NBTCompound nbt
) implements EntityState {

    static EntityState fromLive(Entity entity) {

        Object rawNbt = new RtagEntity(entity).getTag();
        return new EntityStateImpl(
                entity.getClass(),
                entity.getType(),
                entity.getUniqueId(),
                entity.getLocation(),
                new RTagNBTCompound(rawNbt)
        );
    }

}
