package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.test.mock.MockBlueprints;
import com.kntrel.mc.territotem.test.mock.MockPlugin;
import com.kntrel.mc.territotem.test.mock.MockServer;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlueprintTracker Tests")
class StructureTest {

    //FIELDS
    private Blueprint blueprint;
    private Server server;
    private World world;
    private StructureService service;


    //SETUP
    @BeforeEach
    void setUp() {
        this.blueprint = MockBlueprints.square4();
        this.server = new MockServer("mock_server");
        this.world = this.server.getWorlds().getFirst();

        Plugin plugin = new MockPlugin("mock_plugin", this.server);
        this.service = new MockStructureService(plugin);
        this.service.registerBlueprint(blueprint).doNotTrack();
    }


    //TESTS
    @Test
    @DisplayName("Full scan marks tracker complete when all blocks match")
    void fullScanCompletesWhenAllBlocksMatch() {
        Vec3i origin = new Vec3i(10, 64, 20);
        placeStructure(origin);

        Structure tracker = this.newTracker(origin);

        assertFalse(tracker.isParked());
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Full scan parks tracker when at least half of known blocks mismatch")
    void fullScanParksWhenHalfOrMoreMismatch() {
        Vec3i origin = Vec3i.zeroes();
        partiallyPlaceStructure(origin);

        Structure tracker = this.newTracker(origin);

        assertTrue(tracker.isParked());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("Parked tracker ignores updates that are not for parked offset")
    void parkedTrackerIgnoresUnrelatedUpdates() {
        Vec3i origin = Vec3i.zeroes();
        partiallyPlaceStructure(origin);

        Structure tracker = this.newTracker(origin);
        assertTrue(tracker.isParked());

        tracker.update(new Vec3i(1, 0, 1));

        assertTrue(tracker.isParked());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("Parked tracker unparks and rescans when parked offset receives matching update")
    void parkedTrackerUnparksAndRescansOnMatchingUpdate() {
        Vec3i origin = Vec3i.zeroes();
        partiallyPlaceStructure(origin);

        Structure tracker = this.newTracker(origin);
        assertTrue(tracker.isParked());

        setBlock(origin, Material.LIGHTNING_ROD);
        setBlock(origin.add(new Vec3i(1, 0, 0)), Material.OBSIDIAN);

        Vec3i parkedAt = tracker.parkedAt().orElse(null);
        assertNotNull(parkedAt);

        Material parkedMaterial = null;
        if (parkedAt.equals(Vec3i.zeroes())) { parkedMaterial = Material.LIGHTNING_ROD; }
        else if (parkedAt.equals(new Vec3i(1, 0, 0))) {parkedMaterial = Material.OBSIDIAN; }
        assertNotNull(parkedMaterial);

        tracker.update(parkedAt);

        assertFalse(tracker.isParked());
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Update transitions tracker between complete and incomplete when not parked")
    void updateChangesCompletionStateWhenNotParked() {
        Vec3i origin = Vec3i.zeroes();
        placeStructure(origin);

        Structure tracker = this.newTracker(origin);
        assertTrue(tracker.isComplete());

        Vec3i mod = new Vec3i(1, 0, 1);
        this.setBlock(mod, Material.DIRT);
        tracker.update(mod);;
        assertFalse(tracker.isComplete());
        assertFalse(tracker.isParked());

        this.setBlock(mod, Material.GOLD_BLOCK);
        tracker.update(mod);;
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Dropped tracker ignores updates")
    void lockedTrackerIgnoresUpdates() {
        Vec3i origin = Vec3i.zeroes();
        placeStructure(origin);

        Structure tracker = this.newTracker(origin);
        tracker.drop();

        tracker.update(new Vec3i(1, 0, 1));;

        assertTrue(tracker.isLocked());
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("State correctly transitions")
    void testStateTransitions() {
        Vec3i origin = Vec3i.zeroes();
        Structure tracker = this.newTracker(origin);

        assertEquals(Structure.State.EMPTY, tracker.getState());
        assertTrue(tracker.isParked());
        tracker.fullScan();

        placeStructure(origin);
        tracker.fullScan();
        assertEquals(Structure.State.COMPLETE, tracker.getState());
        assertFalse(tracker.isParked());

        Vec3i offset = new Vec3i(1, 0, 1);
        setBlock(offset, Material.DIRT);

        tracker.update(offset);
        assertEquals(Structure.State.IN_PROGRESS, tracker.getState());

        setBlock(origin, Material.AIR);
        setBlock(new Vec3i(1, 0, 0), Material.AIR);
        setBlock(new Vec3i(0, 0, 1), Material.AIR);

        tracker.update(origin);
        assertEquals(Structure.State.IN_PROGRESS, tracker.getState());
        assertTrue(tracker.isParked());

        tracker.fullScan();
        assertEquals(Structure.State.EMPTY, tracker.getState());
        assertTrue(tracker.isParked());
    }


    //HELPERS
    private Structure newTracker(Vec3i origin) {
        return this.service.track(this.blueprint, origin, this.world, null);
    }
    private void placeStructure(Vec3i origin) {
        this.service.placeAt(this.blueprint, origin, this.world);
    }
    private void setBlock(Vec3i coordinates, Material material) {
        this.world.getBlockAt(coordinates.x(), coordinates.y(), coordinates.z()).setType(material);
    }
    private void partiallyPlaceStructure(Vec3i origin) {
        setBlock(origin, Material.DIRT);
        setBlock(origin.add(new Vec3i(1, 0, 0)), Material.DIRT);
        setBlock(origin.add(new Vec3i(0, 0, 1)), Material.CRYING_OBSIDIAN);
        setBlock(origin.add(new Vec3i(1, 0, 1)), Material.GOLD_BLOCK);
    }
}
