package com.kntrel.mc.territotem.totem.region;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionAllocatorTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void expandsDirectionalGrowthWithoutCollision() {
        World world = mock(World.class);
        Region region = region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        InMemoryRegionRepository repository = new InMemoryRegionRepository(region);
        RegionAllocator allocator = new RegionAllocator(repository, Condition.TRUE);

        ExpansionResult result = allocator.expand(region, Expansion.forDirection(TotemCore.Direction.EAST, 6d));

        assertTrue(result.hasGrowth());
        assertEquals(6d, result.accomplished().east(), DELTA);
        assertEquals(0d, result.unachievedTotal(), DELTA);
        assertEquals(7d, region.getMaxX(), DELTA);
    }

    @Test
    void snapsDirectionalGrowthToTheNearestCollision() {
        World world = mock(World.class);
        Region region = region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Region obstacle = region(2L, world, new BoundingBox(4.5, 0, 0, 6, 1, 1));
        InMemoryRegionRepository repository = new InMemoryRegionRepository(region, obstacle);
        RegionAllocator allocator = new RegionAllocator(repository, Condition.TRUE);

        ExpansionResult result = allocator.expand(region, Expansion.forDirection(TotemCore.Direction.EAST, 6d));

        assertTrue(result.hasGrowth());
        assertEquals(3.5d, result.accomplished().east(), DELTA);
        assertEquals(2.5d, result.unachievedTotal(), DELTA);
        assertEquals(4.5d, region.getMaxX(), DELTA);
    }

    @Test
    void leavesDirectionalGrowthUntouchedWhenAlreadyTouching() {
        World world = mock(World.class);
        Region region = region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Region obstacle = region(2L, world, new BoundingBox(1, 0, 0, 2, 1, 1));
        InMemoryRegionRepository repository = new InMemoryRegionRepository(region, obstacle);
        RegionAllocator allocator = new RegionAllocator(repository, Condition.TRUE);

        ExpansionResult result = allocator.expand(region, Expansion.forDirection(TotemCore.Direction.EAST, 6d));

        assertFalse(result.hasGrowth());
        assertEquals(6d, result.unachievedTotal(), DELTA);
        assertEquals(1d, region.getMaxX(), DELTA);
    }

    @Test
    void redistributesOmnidirectionalShortageToTheOppositeSide() {
        World world = mock(World.class);
        Region region = region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Region obstacle = region(2L, world, new BoundingBox(1.25, 0, 0, 2, 1, 1));
        InMemoryRegionRepository repository = new InMemoryRegionRepository(region, obstacle);
        RegionAllocator allocator = new RegionAllocator(repository, Condition.TRUE);

        ExpansionResult result = allocator.expand(region, Expansion.all(1d));

        assertEquals(0.25d, result.accomplished().east(), DELTA);
        assertEquals(1.75d, result.accomplished().west(), DELTA);
        assertEquals(1d, result.accomplished().up(), DELTA);
        assertEquals(1d, result.accomplished().down(), DELTA);
        assertEquals(1d, result.accomplished().north(), DELTA);
        assertEquals(1d, result.accomplished().south(), DELTA);
        assertEquals(0d, result.unachievedTotal(), DELTA);
        assertEquals(1.25d, region.getMaxX(), DELTA);
        assertEquals(-1.75d, region.getMinX(), DELTA);
    }

    @Test
    void redistributesOmnidirectionalShortageAcrossRemainingFreeSides() {
        World world = mock(World.class);
        Region region = region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Region eastObstacle = region(2L, world, new BoundingBox(1.2, 0, 0, 2, 1, 1));
        Region westObstacle = region(3L, world, new BoundingBox(-1, 0, 0, 0, 1, 1));
        InMemoryRegionRepository repository = new InMemoryRegionRepository(region, eastObstacle, westObstacle);
        RegionAllocator allocator = new RegionAllocator(repository, Condition.TRUE);

        ExpansionResult result = allocator.expand(region, Expansion.all(1d));

        assertEquals(0.2d, result.accomplished().east(), DELTA);
        assertEquals(0d, result.accomplished().west(), DELTA);
        assertEquals(1.45d, result.accomplished().up(), DELTA);
        assertEquals(1.45d, result.accomplished().down(), DELTA);
        assertEquals(1.45d, result.accomplished().north(), DELTA);
        assertEquals(1.45d, result.accomplished().south(), DELTA);
        assertEquals(0d, result.unachievedTotal(), DELTA);
    }

    @Test
    void reportsUnachievedQuotaWhenTheRegionIsTrapped() {
        World world = mock(World.class);
        Region region = region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        InMemoryRegionRepository repository = new InMemoryRegionRepository(
                region,
                region(2L, world, new BoundingBox(1.5, 0, 0, 2.5, 1, 1)),
                region(3L, world, new BoundingBox(-1.5, 0, 0, -0.5, 1, 1)),
                region(4L, world, new BoundingBox(0, 1.5, 0, 1, 2.5, 1)),
                region(5L, world, new BoundingBox(0, -1.5, 0, 1, -0.5, 1)),
                region(6L, world, new BoundingBox(0, 0, -1.5, 1, 1, -0.5)),
                region(7L, world, new BoundingBox(0, 0, 1.5, 1, 1, 2.5))
        );
        RegionAllocator allocator = new RegionAllocator(repository, Condition.TRUE);

        ExpansionResult result = allocator.expand(region, Expansion.all(1d));

        assertEquals(0.5d, result.accomplished().east(), DELTA);
        assertEquals(0.5d, result.accomplished().west(), DELTA);
        assertEquals(0.5d, result.accomplished().up(), DELTA);
        assertEquals(0.5d, result.accomplished().down(), DELTA);
        assertEquals(0.5d, result.accomplished().north(), DELTA);
        assertEquals(0.5d, result.accomplished().south(), DELTA);
        assertEquals(3d, result.unachievedTotal(), DELTA);
    }

    private static Region region(long id, World world, BoundingBox box) {
        RegionContext context = mock(RegionContext.class);
        when(context.getConfig()).thenReturn(RegionContextConfig.defaultConfig());

        Region region = new Region(context, box, world, "region-" + id, mock(Hierarchy.class));
        region.setId(id);
        return region;
    }

    private static final class InMemoryRegionRepository implements RegionRepository {

        private final List<Region> regions_ = new ArrayList<>();

        private InMemoryRegionRepository(Region... regions) {
            this.regions_.addAll(Arrays.asList(regions));
        }

        @Override
        public List<Region> get(Query query) {
            return this.regions_.stream()
                    .filter(region -> query.includesDestroyed() || !region.isDestroyed())
                    .filter(query.getCondition())
                    .toList();
        }

        @Override
        public void save(Region... regions) {
            this.regions_.addAll(Arrays.asList(regions));
        }
    }
}
