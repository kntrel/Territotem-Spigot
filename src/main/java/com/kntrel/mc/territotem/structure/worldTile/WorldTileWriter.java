package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import java.util.UUID;

public interface WorldTileWriter extends WorldTile {

    //FACTORY
    static WorldTileWriter of(Vec3i coordinates, World world) { return new WorldTileWriterImpl(coordinates, world); }


    //CONTRACT
    void setBlock(Material material, @Nullable NBTCompound nbt);
    void killEntity(UUID id);
    void killEntities();
    void spawnEntity(EntityType type, Vector offset, @Nullable NBTCompound nbt);
    void moveEntity(UUID id, Vector offset);
    void editEntity(UUID id, NBTCompound ntb);


    //DEFAULTS
    default void setBlock(Material material) { this.setBlock(material, null); }
    default void editBlock(NBTCompound nbt) { this.setBlock(this.block().getType(), nbt); }
    default void breakBlock() { this.setBlock(Material.AIR); }
    default void killEntity(EntityState entity) { this.killEntity(entity.id()); }
    default void spawnEntity(EntityType type, Vector offset) { this.spawnEntity(type, offset, null); }
    default void spawnEntity(EntityType type, @Nullable NBTCompound nbt) { this.spawnEntity(type, new Vector(.5, 0, .5), nbt); }
    default void spawnEntity(EntityType type) { this.spawnEntity(type, (NBTCompound) null); }
    default void moveEntity(EntityState entity, Vector offset) { this.moveEntity(entity.id(), offset); }
    default void editEntity(EntityState entity, NBTCompound nbt) { this.editEntity(entity.id(), nbt); }
    default void editEntity(UUID id, Vector offset, NBTCompound nbt) {
        this.moveEntity(id, offset);
        this.editEntity(id, nbt);
    }
    default void editEntity(EntityState entity, Vector offset, NBTCompound nbt) { this.editEntity(entity.id(), offset, nbt); }
}
