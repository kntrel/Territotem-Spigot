package com.kntrel.mc.territotem.util;

import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.World;
import java.util.UUID;

import static com.kntrel.mc.regionLib.Constants.CHUNK_SHIFT;

//SUBTYPES
public record ChunkKey(int x, int z, UUID world) {

    public ChunkKey(int x, int y, World world) {
        this(x, y, world.getUID());
    }

    public ChunkKey(Chunk chunk) {
        this(chunk.getX(), chunk.getZ(), chunk.getWorld().getUID());
    }

    public static ChunkKey ofBlock(Vec3i location, UUID world) {
        return new ChunkKey(location.x() >> CHUNK_SHIFT, location.z() >> CHUNK_SHIFT, world);
    }
}
