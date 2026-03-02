package com.kntrel.mc.territotem.structure.worldTile;

import com.kntrel.mc.territotem.test.mock.MockWorld;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldTileImplTest {

    @Test
    void worldReturnsReadOnlyWorldInfo() {
        MockWorld world = new MockWorld("world-tile-world");
        WorldTile tile = WorldTile.of(new Vec3i(0, 64, 0), world);

        WorldView worldView = tile.world();

        assertEquals(world.getUID(), worldView.id());
        assertEquals(world.getName(), worldView.name());
        assertEquals(world.getKey(), worldView.key());
        assertEquals(World.Environment.NORMAL, worldView.dimension());
    }
}
