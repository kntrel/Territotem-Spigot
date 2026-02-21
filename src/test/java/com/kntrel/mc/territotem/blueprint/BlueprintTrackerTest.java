package com.kntrel.mc.territotem.blueprint;

import com.kntrel.mc.territotem.mock.MockBlueprints;
import com.kntrel.mc.territotem.test.mock.MockBlockWorld;
import com.kntrel.mc.territotem.test.mock.TestBlueprintRegistry;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlueprintTracker Tests")
class BlueprintTrackerTest {

    private Blueprint blueprint;
    private TestBlueprintRegistry registry;
    private MockBlockWorld worldData;

    @BeforeEach
    void setUp() {
        this.blueprint = MockBlueprints.square4();
        this.registry = new TestBlueprintRegistry();
        this.worldData = new MockBlockWorld();
    }

    @Test
    @DisplayName("Full scan marks tracker complete when all blocks match")
    void fullScanCompletesWhenAllBlocksMatch() {
        Vec3i origin = new Vec3i(10, 64, 20);
        setWorldFromBlueprint(this.worldData, this.blueprint, origin);

        BlueprintTracker tracker = new BlueprintTracker(this.registry, this.blueprint, this.worldData.asWorld(), origin);

        assertFalse(tracker.isParked());
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Full scan parks tracker when at least half of known blocks mismatch")
    void fullScanParksWhenHalfOrMoreMismatch() {
        Vec3i origin = Vec3i.zeroes();
        setPartiallyBrokenWorld(origin);

        BlueprintTracker tracker = new BlueprintTracker(this.registry, this.blueprint, this.worldData.asWorld(), origin);

        assertTrue(tracker.isParked());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("Parked tracker ignores updates that are not for parked offset")
    void parkedTrackerIgnoresUnrelatedUpdates() {
        Vec3i origin = Vec3i.zeroes();
        setPartiallyBrokenWorld(origin);

        BlueprintTracker tracker = new BlueprintTracker(this.registry, this.blueprint, this.worldData.asWorld(), origin);
        assertTrue(tracker.isParked());

        tracker.update(new Vec3i(1, 0, 1), new BlueprintElement.Block(Material.GOLD_BLOCK));

        assertTrue(tracker.isParked());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("Parked tracker unparks and rescans when parked offset receives matching update")
    void parkedTrackerUnparksAndRescansOnMatchingUpdate() {
        Vec3i origin = Vec3i.zeroes();
        setPartiallyBrokenWorld(origin);

        BlueprintTracker tracker = new BlueprintTracker(this.registry, this.blueprint, this.worldData.asWorld(), origin);
        assertTrue(tracker.isParked());

        this.worldData.set(origin, Material.LIGHTNING_ROD);
        this.worldData.set(origin.add(new Vec3i(1, 0, 0)), Material.OBSIDIAN);

        tracker.update(new Vec3i(0, 0, 0), new BlueprintElement.Core(Material.LIGHTNING_ROD));

        assertFalse(tracker.isParked());
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Update transitions tracker between complete and incomplete when not parked")
    void updateChangesCompletionStateWhenNotParked() {
        Vec3i origin = Vec3i.zeroes();
        setWorldFromBlueprint(this.worldData, this.blueprint, origin);

        BlueprintTracker tracker = new BlueprintTracker(this.registry, this.blueprint, this.worldData.asWorld(), origin);
        assertTrue(tracker.isComplete());

        tracker.update(new Vec3i(1, 0, 1), new BlueprintElement.Block(Material.DIRT));
        assertFalse(tracker.isComplete());
        assertFalse(tracker.isParked());

        tracker.update(new Vec3i(1, 0, 1), new BlueprintElement.Block(Material.GOLD_BLOCK));
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Locked tracker ignores updates")
    void lockedTrackerIgnoresUpdates() {
        Vec3i origin = Vec3i.zeroes();
        setWorldFromBlueprint(this.worldData, this.blueprint, origin);

        BlueprintTracker tracker = new BlueprintTracker(this.registry, this.blueprint, this.worldData.asWorld(), origin);
        tracker.lock();

        tracker.update(new Vec3i(1, 0, 1), new BlueprintElement.Block(Material.DIRT));

        assertTrue(tracker.isLocked());
        assertTrue(tracker.isComplete());
    }

    private void setPartiallyBrokenWorld(Vec3i origin) {
        this.worldData.set(origin, Material.DIRT);
        this.worldData.set(origin.add(new Vec3i(1, 0, 0)), Material.DIRT);
        this.worldData.set(origin.add(new Vec3i(0, 0, 1)), Material.CRYING_OBSIDIAN);
        this.worldData.set(origin.add(new Vec3i(1, 0, 1)), Material.GOLD_BLOCK);
    }

    private static void setWorldFromBlueprint(MockBlockWorld worldData, Blueprint blueprint, Vec3i origin) {
        blueprint.elementsByOffset().forEach((offset, element) -> {
            Material material = Material.AIR;
            if (element instanceof BlueprintElement.Core core) {
                material = core.type();
            } else if (element instanceof BlueprintElement.Block block) {
                material = block.type();
            }
            worldData.set(origin.add(offset), material);
        });
    }
}
