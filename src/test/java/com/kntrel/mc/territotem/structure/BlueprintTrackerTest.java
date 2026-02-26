package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.test.mock.MockBlueprints;
import com.kntrel.mc.territotem.test.mock.MockBlockWorld;
import com.kntrel.mc.territotem.test.mock.TestStructureService;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlueprintTracker Tests")
class BlueprintTrackerTest {

    //FIELDS
    private Blueprint blueprint;
    private TestStructureService service;
    private MockBlockWorld worldData;


    //SETUP
    @BeforeEach
    void setUp() {
        this.blueprint = MockBlueprints.square4();
        this.service = new TestStructureService();
        this.worldData = new MockBlockWorld();
    }


    //TESTS
    @Test
    @DisplayName("Full scan marks tracker complete when all blocks match")
    void fullScanCompletesWhenAllBlocksMatch() {
        Vec3i origin = new Vec3i(10, 64, 20);
        setWorldFromBlueprint(this.worldData, this.blueprint, origin);

        BlueprintTracker tracker = this.newTracker(origin);

        assertFalse(tracker.isParked());
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Full scan parks tracker when at least half of known blocks mismatch")
    void fullScanParksWhenHalfOrMoreMismatch() {
        Vec3i origin = Vec3i.zeroes();
        setPartiallyBrokenWorld(origin);

        BlueprintTracker tracker = this.newTracker(origin);

        assertTrue(tracker.isParked());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("Parked tracker ignores updates that are not for parked offset")
    void parkedTrackerIgnoresUnrelatedUpdates() {
        Vec3i origin = Vec3i.zeroes();
        setPartiallyBrokenWorld(origin);

        BlueprintTracker tracker = this.newTracker(origin);
        assertTrue(tracker.isParked());

        tracker.update(new Vec3i(1, 0, 1));

        assertTrue(tracker.isParked());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("Parked tracker unparks and rescans when parked offset receives matching update")
    void parkedTrackerUnparksAndRescansOnMatchingUpdate() {
        Vec3i origin = Vec3i.zeroes();
        setPartiallyBrokenWorld(origin);

        BlueprintTracker tracker = this.newTracker(origin);
        assertTrue(tracker.isParked());

        this.worldData.set(origin, Material.LIGHTNING_ROD);
        this.worldData.set(origin.add(new Vec3i(1, 0, 0)), Material.OBSIDIAN);

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
        setWorldFromBlueprint(this.worldData, this.blueprint, origin);

        BlueprintTracker tracker = this.newTracker(origin);
        assertTrue(tracker.isComplete());

        tracker.update(new Vec3i(1, 0, 1));;
        assertFalse(tracker.isComplete());
        assertFalse(tracker.isParked());

        tracker.update(new Vec3i(1, 0, 1));;
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Locked tracker ignores updates")
    void lockedTrackerIgnoresUpdates() {
        Vec3i origin = Vec3i.zeroes();
        setWorldFromBlueprint(this.worldData, this.blueprint, origin);

        BlueprintTracker tracker = this.newTracker(origin);
        tracker.lock();

        tracker.update(new Vec3i(1, 0, 1));;

        assertTrue(tracker.isLocked());
        assertTrue(tracker.isComplete());
    }


    //HELPERS
    private BlueprintTracker newTracker(Vec3i origin) {
        return new BlueprintTracker(this.service, this.blueprint, this.worldData.asWorld(), origin);
    }
    private void setWorldFromBlueprint(MockBlockWorld worldData, Blueprint blueprint, Vec3i origin) {
        this.service.placeAt(blueprint, origin, worldData.asWorld());
    }
    private void setPartiallyBrokenWorld(Vec3i origin) {
        this.worldData.set(origin, Material.DIRT);
        this.worldData.set(origin.add(new Vec3i(1, 0, 0)), Material.DIRT);
        this.worldData.set(origin.add(new Vec3i(0, 0, 1)), Material.CRYING_OBSIDIAN);
        this.worldData.set(origin.add(new Vec3i(1, 0, 1)), Material.GOLD_BLOCK);
    }
}
