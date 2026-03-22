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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionAllocatorTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void expandsDirectionalGrowthWithoutCollision() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));

        ExpansionResult result = fixture.allocator().expand(region, Expansion.forDirection(TotemCore.Direction.EAST, 6d));

        assertTrue(result.hasGrowth());
        assertEquals(6d, result.accomplished().east(), DELTA);
        assertEquals(0d, result.unachievedTotal(), DELTA);
        assertEquals(7d, region.getMaxX(), DELTA);
    }

    @Test
    void snapsDirectionalGrowthToTheNearestCollision() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Region blocker = fixture.region(2L, world, new BoundingBox(4.5, 0, 0, 6, 1, 1));

        ExpansionResult result = fixture.allocator().expand(region, Expansion.forDirection(TotemCore.Direction.EAST, 6d));

        assertTrue(result.hasGrowth());
        assertEquals(3.5d, result.accomplished().east(), DELTA);
        assertEquals(2.5d, result.unachievedTotal(), DELTA);
        assertEquals(4.5d, region.getMaxX(), DELTA);
        assertTrue(result.blockingRegions().contains(blocker));
    }

    @Test
    void leavesDirectionalGrowthUntouchedWhenAlreadyTouching() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Region blocker = fixture.region(2L, world, new BoundingBox(1, 0, 0, 2, 1, 1));

        ExpansionResult result = fixture.allocator().expand(region, Expansion.forDirection(TotemCore.Direction.EAST, 6d));

        assertFalse(result.hasGrowth());
        assertEquals(6d, result.unachievedTotal(), DELTA);
        assertEquals(1d, region.getMaxX(), DELTA);
        assertTrue(result.blockingRegions().contains(blocker));
    }

    @Test
    void redistributesOmnidirectionalShortageToTheOppositeSide() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        fixture.region(2L, world, new BoundingBox(1.25, 0, 0, 2, 1, 1));

        ExpansionResult result = fixture.allocator().expand(region, Expansion.all(1d));

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
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        fixture.region(2L, world, new BoundingBox(1.2, 0, 0, 2, 1, 1));
        fixture.region(3L, world, new BoundingBox(-1, 0, 0, 0, 1, 1));

        ExpansionResult result = fixture.allocator().expand(region, Expansion.all(1d));

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
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        fixture.region(2L, world, new BoundingBox(1.5, 0, 0, 2.5, 1, 1));
        fixture.region(3L, world, new BoundingBox(-1.5, 0, 0, -0.5, 1, 1));
        fixture.region(4L, world, new BoundingBox(0, 1.5, 0, 1, 2.5, 1));
        fixture.region(5L, world, new BoundingBox(0, -1.5, 0, 1, -0.5, 1));
        fixture.region(6L, world, new BoundingBox(0, 0, -1.5, 1, 1, -0.5));
        fixture.region(7L, world, new BoundingBox(0, 0, 1.5, 1, 1, 2.5));

        ExpansionResult result = fixture.allocator().expand(region, Expansion.all(1d));

        assertEquals(0.5d, result.accomplished().east(), DELTA);
        assertEquals(0.5d, result.accomplished().west(), DELTA);
        assertEquals(0.5d, result.accomplished().up(), DELTA);
        assertEquals(0.5d, result.accomplished().down(), DELTA);
        assertEquals(0.5d, result.accomplished().north(), DELTA);
        assertEquals(0.5d, result.accomplished().south(), DELTA);
        assertEquals(3d, result.unachievedTotal(), DELTA);
        assertEquals(6, result.blockingRegions().size());
    }

    @Test
    void contractsPreviouslyAppliedGrowthExactly() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 1, 1, 1));
        Expansion growth = new Expansion(2d, 1d, 0.5d, 3d, 4d, 1.5d);

        fixture.allocator().expand(region, growth);
        fixture.allocator().contract(region, growth);

        assertEquals(0d, region.getMinX(), DELTA);
        assertEquals(0d, region.getMinY(), DELTA);
        assertEquals(0d, region.getMinZ(), DELTA);
        assertEquals(1d, region.getMaxX(), DELTA);
        assertEquals(1d, region.getMaxY(), DELTA);
        assertEquals(1d, region.getMaxZ(), DELTA);
    }

    @Test
    void placesRegionAsRequestedWhenNothingOverlaps() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        BoundingBox critical = new BoundingBox(0, 0, 0, 1, 1, 1);
        BoundingBox proposed = new BoundingBox(-2, -1, -2, 3, 2, 3);

        RegionPlaceResult result = fixture.allocator().place(world, proposed, critical, "placed-region", fixture.hierarchy());
        RegionPlaceResult.Placed placed = result.getPlaced().orElseThrow();

        assertTrue(result.isPlaced());
        assertEquals(proposed.getMinX(), placed.resulting().getMinX(), DELTA);
        assertEquals(proposed.getMaxX(), placed.resulting().getMaxX(), DELTA);
        assertEquals(proposed.getMinY(), placed.resulting().getMinY(), DELTA);
        assertEquals(proposed.getMaxY(), placed.resulting().getMaxY(), DELTA);
        assertEquals(proposed.getMinZ(), placed.resulting().getMinZ(), DELTA);
        assertEquals(proposed.getMaxZ(), placed.resulting().getMaxZ(), DELTA);
        assertEquals(placed.resulting().getMaxX(), placed.region().getMaxX(), DELTA);
    }

    @Test
    void rejectsPlacementWhenCriticalBoxAlreadyCollides() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region blocker = fixture.region(1L, world, new BoundingBox(0.5, 0, 0, 1.5, 1, 1));

        RegionPlaceResult result = fixture.allocator().place(
                world,
                new BoundingBox(-2, 0, 0, 3, 1, 1),
                new BoundingBox(0, 0, 0, 1, 1, 1),
                "critical-overlap",
                fixture.hierarchy()
        );

        assertFalse(result.isPlaced());
        assertTrue(result.getUnplaceable().orElseThrow().overlappingRegions().contains(blocker));
    }

    @Test
    void shiftsPlacementTowardsTheOppositeSide() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        fixture.region(1L, world, new BoundingBox(2.5, 0, 0, 4, 1, 1));

        RegionPlaceResult result = fixture.allocator().place(
                world,
                new BoundingBox(-2, 0, 0, 3, 1, 1),
                new BoundingBox(0, 0, 0, 1, 1, 1),
                "shifted-region",
                fixture.hierarchy()
        );
        RegionPlaceResult.Placed placed = result.getPlaced().orElseThrow();

        assertTrue(result.isPlaced());
        assertEquals(2.5d, placed.resulting().getMaxX(), DELTA);
        assertEquals(-2.5d, placed.resulting().getMinX(), DELTA);
    }

    @Test
    void rejectsPlacementWhenTheFullBoundsCannotBeRedistributed() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        fixture.region(1L, world, new BoundingBox(1.5, 0, 0, 2.5, 1, 1));
        fixture.region(2L, world, new BoundingBox(-1.5, 0, 0, -0.5, 1, 1));
        fixture.region(3L, world, new BoundingBox(0, 1.5, 0, 1, 2.5, 1));
        fixture.region(4L, world, new BoundingBox(0, -1.5, 0, 1, -0.5, 1));
        fixture.region(5L, world, new BoundingBox(0, 0, -1.5, 1, 1, -0.5));
        fixture.region(6L, world, new BoundingBox(0, 0, 1.5, 1, 1, 2.5));

        RegionPlaceResult result = fixture.allocator().place(
                world,
                new BoundingBox(-1, -1, -1, 2, 2, 2),
                new BoundingBox(0, 0, 0, 1, 1, 1),
                "trapped-placement",
                fixture.hierarchy()
        );

        assertFalse(result.isPlaced());
        assertEquals(6, result.getUnplaceable().orElseThrow().overlappingRegions().size());
    }

    private static Fixture fixture() {
        InMemoryRegionRepository repository = new InMemoryRegionRepository();
        Hierarchy hierarchy = mock(Hierarchy.class);
        RegionContext context = mock(RegionContext.class);

        when(context.getConfig()).thenReturn(RegionContextConfig.defaultConfig());
        when(context.getRegionRepository()).thenReturn(repository);
        when(context.create(any(BoundingBox.class), any(World.class), anyString(), any(Hierarchy.class))).thenAnswer(invocation -> {
            BoundingBox bounds = invocation.getArgument(0, BoundingBox.class);
            World world = invocation.getArgument(1, World.class);
            String name = invocation.getArgument(2, String.class);
            Hierarchy targetHierarchy = invocation.getArgument(3, Hierarchy.class);

            Region region = new Region(context, bounds, world, name, targetHierarchy);
            region.setId(repository.nextId());
            repository.save(region);
            return region;
        });

        return new Fixture(context, repository, hierarchy);
    }

    private record Fixture(RegionContext context, InMemoryRegionRepository repository, Hierarchy hierarchy) {

        private RegionAllocator allocator() {
            return new RegionAllocator(this.context, Condition.TRUE);
        }

        private Region region(long id, World world, BoundingBox box) {
            Region region = new Region(this.context, box, world, "region-" + id, this.hierarchy);
            region.setId(id);
            this.repository.save(region);
            return region;
        }
    }

    private static final class InMemoryRegionRepository implements RegionRepository {

        private final List<Region> regions_ = new ArrayList<>();
        private long nextId_ = 1L;

        @Override
        public List<Region> get(Query query) {
            return this.regions_.stream()
                    .filter(region -> query.includesDestroyed() || !region.isDestroyed())
                    .filter(query.getCondition())
                    .toList();
        }

        @Override
        public void save(Region... regions) {
            for (Region region : regions) {
                this.regions_.removeIf(existing -> existing == region || sameId(existing, region));
                this.regions_.add(region);

                Long id = region.getId();
                if (id != null && id >= this.nextId_) {
                    this.nextId_ = id + 1;
                }
            }
        }

        private long nextId() {
            return this.nextId_++;
        }

        private static boolean sameId(Region a, Region b) {
            if (a.getId() == null || b.getId() == null) {
                return false;
            }
            return a.getId().equals(b.getId());
        }
    }
}
