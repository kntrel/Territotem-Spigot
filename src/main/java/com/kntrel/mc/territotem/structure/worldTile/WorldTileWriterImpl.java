package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.util.Vec3i;
import com.saicone.rtag.RtagBlock;
import com.saicone.rtag.RtagEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

class WorldTileWriterImpl extends WorldTileImpl implements WorldTileWriter {

    //CONSTANTS
    private static final Vector MIN_VEC = new Vector(0, 0, 0), MAX_VEC = new Vector(1, 1, 1);


    //CONSTRUCTOR
    WorldTileWriterImpl(Vec3i coordinates, World world) {
        super(coordinates, world);
    }


    //IMPLEMENTATION
    @Override public void setBlock(Material material, @Nullable NBTCompound nbt) {
        Block block = this.world_.getBlockAt(this.x(), this.y(), this.z());
        block.setType(material);

        if (nbt == null) { return; }

        RtagBlock rtag = new RtagBlock(block);
        rtag.set(nbt.handle());
    }
    @Override public void killEntity(UUID id) {
        this.getEntity(id).ifPresent(Entity::remove);
    }
    @Override public void killEntities() {
        this.actualEntities().forEach(Entity::remove);
    }
    @Override public void spawnEntity(EntityType type, Vector offset, @Nullable NBTCompound nbt) {
        Entity entity = this.world_.spawnEntity(this.getLocation(offset), type);
        if (nbt != null) {
            new RtagEntity(entity).set(nbt.handle());
        }
    }
    @Override public void moveEntity(UUID id, Vector offset) {
        this.getEntity(id).ifPresent(e -> e.teleport(this.getLocation(offset)));
    }
    @Override public void editEntity(UUID id, NBTCompound nbt) {
        this.getEntity(id).ifPresent(e -> new RtagEntity(e).set(nbt.handle()));
    }


    //HELPERS
    private Optional<Entity> getEntity(UUID id) {
        return this.actualEntities()
                .filter(e -> e.getUniqueId().equals(id))
                .findFirst();
    }
    private Location getLocation(Vector offset) {
        Vector  min = Vector.getMinimum(offset, MAX_VEC),
                max = Vector.getMaximum(min, MIN_VEC),
                vec = this.coordinates().toDouble().add(max);
        return new Location(this.world_, vec.getX(), vec.getY(), vec.getZ());
    }
}
