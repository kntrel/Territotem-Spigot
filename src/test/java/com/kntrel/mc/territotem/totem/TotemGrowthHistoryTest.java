package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.territotem.totem.region.Expansion;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TotemGrowthHistoryTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void writeAndReadRoundTripPreservesBaseBoundsAndHistory() {
        Fixture fixture = fixture();
        World world = mock(World.class);
        Region region = fixture.region(1L, world, new BoundingBox(0, 0, 0, 10, 10, 10));
        TotemGrowthHistory.State expected = new TotemGrowthHistory.State(
                new BoundingBox(1, 2, 3, 8, 9, 10),
                List.of(
                        new Expansion(1, 0, 0, 0, 0, 0),
                        new Expansion(0, 0.5, 0, 0.25, 0, 0.75)
                )
        );

        TotemGrowthHistory.write(region, expected);

        TotemGrowthHistory.State restored = TotemGrowthHistory.read(region);
        assertNotNull(restored);
        assertEquals(expected.history(), restored.history());
        assertEquals(expected.baseBounds().getMinX(), restored.baseBounds().getMinX(), DELTA);
        assertEquals(expected.baseBounds().getMinY(), restored.baseBounds().getMinY(), DELTA);
        assertEquals(expected.baseBounds().getMinZ(), restored.baseBounds().getMinZ(), DELTA);
        assertEquals(expected.baseBounds().getMaxX(), restored.baseBounds().getMaxX(), DELTA);
        assertEquals(expected.baseBounds().getMaxY(), restored.baseBounds().getMaxY(), DELTA);
        assertEquals(expected.baseBounds().getMaxZ(), restored.baseBounds().getMaxZ(), DELTA);
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
