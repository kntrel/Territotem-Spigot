package com.kntrel.mc.territotem.test.mock;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kntrel.mc.territotem.test.mock.Mock.*;

public class MockBlockState implements BlockState {

    //FIELDS
    private final Block block_;
    private Material type_;
    private BlockData blockData_;
    private MaterialData data_;
    private World world_;
    private int x_;
    private int y_;
    private int z_;
    private boolean placed_;
    private byte lightLevel_;
    private byte rawData_;
    private final Map<String, List<MetadataValue>> metadata_;


    //CONSTRUCTOR
    MockBlockState(Block block) {
        this.block_ = block;
        this.metadata_ = new HashMap<>();
        this.placed_ = true;
        this.lightLevel_ = 0;
        this.rawData_ = 0;
        
        // Initialize from block if available
        if (block != null) {
            this.type_ = block.getType();
            this.blockData_ = block.getBlockData();
            this.world_ = block.getWorld();
            this.x_ = block.getX();
            this.y_ = block.getY();
            this.z_ = block.getZ();
            this.lightLevel_ = block.getLightLevel();
        }
    }


    @Override public Block getBlock() {
        return this.block_;
    }
    @Override public MaterialData getData() {
        return this.data_;
    }
    @Override public BlockData getBlockData() {
        return this.blockData_;
    }
    @Override public BlockState copy() {
        MockBlockState copy = new MockBlockState(null);
        copy.type_ = this.type_;
        copy.blockData_ = this.blockData_;
        copy.data_ = this.data_;
        copy.rawData_ = this.rawData_;
        copy.placed_ = false;
        return copy;
    }
    @Override public BlockState copy(Location location) {
        MockBlockState copy = new MockBlockState(null);
        copy.type_ = this.type_;
        copy.blockData_ = this.blockData_;
        copy.data_ = this.data_;
        copy.rawData_ = this.rawData_;
        copy.world_ = location.getWorld();
        copy.x_ = location.getBlockX();
        copy.y_ = location.getBlockY();
        copy.z_ = location.getBlockZ();
        copy.placed_ = false;
        return copy;
    }
    @Override public Material getType() {
        return this.type_;
    }
    @Override public byte getLightLevel() {
        return this.lightLevel_;
    }
    @Override public World getWorld() {
        return this.world_;
    }
    @Override public int getX() {
        return this.x_;
    }
    @Override public int getY() {
        return this.y_;
    }
    @Override public int getZ() {
        return this.z_;
    }
    @Override public Location getLocation() {
        if (this.world_ == null) {
            return new Location(null, this.x_, this.y_, this.z_);
        }
        return new Location(this.world_, this.x_, this.y_, this.z_);
    }
    @Override public Location getLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        loc.setWorld(this.world_);
        loc.setX(this.x_);
        loc.setY(this.y_);
        loc.setZ(this.z_);
        return loc;
    }
    @Override public Chunk getChunk() {
        unimplemented();
        return null;
    }
    @Override public void setData(MaterialData data) {
        this.data_ = data;
    }
    @Override public void setBlockData(BlockData data) {
        this.blockData_ = data;
    }
    @Override public void setType(Material type) {
        this.type_ = type;
    }
    @Override public boolean update() {
        return update(false);
    }
    @Override public boolean update(boolean force) {
        return update(force, true);
    }
    @Override public boolean update(boolean force, boolean applyPhysics) {
        if (!this.placed_) {
            return true;
        }
        
        if (force && this.block_ != null) {
            this.block_.setType(this.type_);
            if (this.blockData_ != null) {
                this.block_.setBlockData(this.blockData_);
            }
            return true;
        }
        
        return this.block_ != null && this.block_.getType() == this.type_;
    }
    @Override public byte getRawData() {
        return this.rawData_;
    }
    @Override public void setRawData(byte data) {
        this.rawData_ = data;
    }
    @Override public boolean isPlaced() {
        return this.placed_;
    }
    @Override public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        this.metadata_.put(metadataKey, List.of(newMetadataValue));
    }
    @Override public List<MetadataValue> getMetadata(String metadataKey) {
        return this.metadata_.getOrDefault(metadataKey, List.of());
    }
    @Override public boolean hasMetadata(String metadataKey) {
        return this.metadata_.containsKey(metadataKey);
    }
    @Override public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        this.metadata_.remove(metadataKey);
    }
}
