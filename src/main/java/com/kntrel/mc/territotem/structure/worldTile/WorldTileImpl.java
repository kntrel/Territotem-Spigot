package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.util.Vec3i;
import com.saicone.rtag.RtagBlock;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Hanging;
import org.bukkit.util.BoundingBox;
import java.util.List;

class WorldTileImpl implements WorldTile {

    //FIELDS
    private final Vec3i coordinates_;
    private final World world_;


    //CONSTRUCTOR
    WorldTileImpl(Vec3i coordinates, World world) {
        this.coordinates_ = coordinates;
        this.world_ = world;
    }


    //IMPLEMENTATION
    @Override public Vec3i coordinates() {
        return this.coordinates_;
    }
    @Override public BlockState block() {
        return this.actualBlock().getBlockData().createBlockState();
    }
    @Override public NBTCompound blockNbt() {
        RtagBlock rtagBlock = new RtagBlock(this.actualBlock());
        Object rawTag = rtagBlock.getTag();
        return new NBTCompound(rawTag);
    }
    @Override public List<EntityState> entities() {
        return this.world_.getNearbyEntities(this.boundingBox()).stream()
                .filter(e ->
                           e instanceof Hanging
                        || e instanceof ArmorStand
                        || e instanceof EnderCrystal
                )
                .map(EntityStateImpl::fromLive)
                .toList();
    }


    //HELPERS
    private Block actualBlock() {
         return this.world_.getBlockAt(this.coordinates_.x(), this.coordinates_.y(), this.coordinates_.z());
    }
    public BoundingBox boundingBox() {
        return new BoundingBox(
                this.coordinates_.x(),
                this.coordinates_.y(),
                this.coordinates_.z(),
                this.coordinates_.x() + 1,
                this.coordinates_.y() + 1,
                this.coordinates_.z() + 1
        );
    }
}
