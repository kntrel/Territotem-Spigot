package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.nbt.NBTCompound;
import com.kntrel.mc.nbt.test.MockNBTCompound;
import com.kntrel.mc.state.StateMap;
import com.kntrel.mc.territotem.test.mock.MockBlock;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import java.util.UUID;

public class MockWorldTile extends WorldTileImpl implements WorldTileWriter {

    public MockWorldTile(Vec3i coordinates, World world) {
        super(coordinates, world);
    }

    @Override public StateMap blockState() {
        return new StateMap();
    }
    @Override public NBTCompound blockNbt() {
        if (this.actualBlock() instanceof MockBlock b) {
            return b.getNbt();
        }
        return new MockNBTCompound();
    }

    @Override
    public void setBlock(Material material, @Nullable StateMap state, @Nullable NBTCompound nbt) {
        Block block = this.world_.getBlockAt(this.x(), this.y(), this.z());
        block.setType(material);

        if (nbt == null) { return; }
        if (!(block instanceof MockBlock b)) { return; }

        b.setNbt(nbt);
    }

    @Override
    public void setBlock(BlockData block) {

    }

    @Override public void killEntity(UUID id) {

    }
    @Override public void killEntities() {
        this.actualEntities().forEach(Entity::remove);
    }
    @Override public void spawnEntity(EntityType type, Vector offset, @Nullable NBTCompound nbt) {

    }
    @Override public void moveEntity(UUID id, Vector offset) {

    }
    @Override public void editEntity(UUID id, NBTCompound nbt) {

    }
}
