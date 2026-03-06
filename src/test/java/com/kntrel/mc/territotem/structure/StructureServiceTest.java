package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.mc.territotem.test.mock.MockBlueprints;
import com.kntrel.mc.territotem.test.mock.MockPlugin;
import com.kntrel.mc.territotem.test.mock.MockServer;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.kntrel.mc.territotem.test.TestTrackingEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("StructureService lifecycle tests")
class StructureServiceTest {

    private Server server;
    private World world;
    private StructureService service;

    @BeforeEach
    void setUp() {
        this.server = new MockServer("mock_server");
        this.world = this.server.getWorlds().getFirst();
        Plugin plugin = new MockPlugin("mock_plugin", this.server);
        this.service = new MockStructureService(plugin);
    }

    @AfterEach
    void tearDown() {
        HandlerList.unregisterAll();
    }

    @Test
    @DisplayName("Blueprint registrations create trackers when configured events fire")
    void registrationTracksOnConfiguredEvent() {
        var blueprint = MockBlueprints.square4();
        Vec3i origin = new Vec3i(32, 64, 32);
        Player player = mock(Player.class);

        this.world.getBlockAt(origin.x(), origin.y(), origin.z()).setType(Material.LIGHTNING_ROD);

        this.service.registerBlueprint(blueprint)
                .on(TestTrackingEvent.class)
                .when(TestTrackingEvent::shouldTrack)
                .track(e -> new TrackingInfo(e.origin(), e.world(), e.player()));

        this.server.getPluginManager().callEvent(new TestTrackingEvent(origin, this.world, player, false));
        assertTrue(this.service.getStructuresInChunk(ChunkKey.ofBlock(origin, this.world.getUID())).isEmpty());

        this.server.getPluginManager().callEvent(new TestTrackingEvent(origin, this.world, player, true));

        waitFor(() -> this.service.getStructuresInChunk(ChunkKey.ofBlock(origin, this.world.getUID())).size() == 1);

        Structure tracker = this.service.getStructuresInChunk(ChunkKey.ofBlock(origin, this.world.getUID()))
                .iterator()
                .next();
        assertEquals(origin, tracker.origin());
        assertFalse(tracker.isComplete());
    }

    @Test
    @DisplayName("BlockPlaceEvent updates a tracked candidate")
    void blockPlaceEventUpdatesTrackedCandidate() {
        var blueprint = MockBlueprints.square4();
        this.service.registerBlueprint(blueprint).doNotTrack();

        Vec3i origin = new Vec3i(16, 70, 16);
        this.world.getBlockAt(origin.x(), origin.y(), origin.z()).setType(Material.LIGHTNING_ROD);
        this.world.getBlockAt(origin.x() + 1, origin.y(), origin.z()).setType(Material.OBSIDIAN);
        this.world.getBlockAt(origin.x(), origin.y(), origin.z() + 1).setType(Material.CRYING_OBSIDIAN);

        Player player = mock(Player.class);
        Structure tracker = this.service.track(blueprint, origin, this.world, player);
        assertEquals(1, this.service.getStructuresInChunk(ChunkKey.ofBlock(origin, this.world.getUID())).size());
        assertFalse(tracker.isComplete());
        assertFalse(tracker.isParked());

        Block finalBlock = this.world.getBlockAt(origin.x() + 1, origin.y(), origin.z() + 1);
        finalBlock.setType(Material.GOLD_BLOCK);

        this.server.getPluginManager().callEvent(new BlockPlaceEvent(
                finalBlock,
                finalBlock.getState(),
                finalBlock,
                null,
                player,
                true,
                EquipmentSlot.HAND
        ));

        waitFor(tracker::isComplete);
        assertTrue(tracker.isComplete());
    }

    @Test
    @DisplayName("Completing a tracker calls registration onCompletion and emits StructureCompletedEvent")
    void completionCallsRegistrationAndEmitsEvent() {
        var blueprint = MockBlueprints.square4();
        AtomicInteger completionCalls = new AtomicInteger();
        AtomicReference<StructureCompletedEvent> completedEventRef = new AtomicReference<>();

        this.service.registerBlueprint(blueprint)
                .onCompletion((structure, entity) -> completionCalls.incrementAndGet())
                .on(TestTrackingEvent.class)
                .when(e -> false)
                .track(e -> null);

        this.server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onComplete(StructureCompletedEvent event) {
                completedEventRef.set(event);
            }
        }, this.service.getPlugin());

        Vec3i origin = new Vec3i(48, 75, 48);
        this.world.getBlockAt(origin.x(), origin.y(), origin.z()).setType(Material.LIGHTNING_ROD);
        this.world.getBlockAt(origin.x() + 1, origin.y(), origin.z()).setType(Material.OBSIDIAN);
        this.world.getBlockAt(origin.x(), origin.y(), origin.z() + 1).setType(Material.CRYING_OBSIDIAN);

        Player player = mock(Player.class);
        this.service.track(blueprint, origin, this.world, player);

        Vec3i completionPos = new Vec3i(origin.x() + 1, origin.y(), origin.z() + 1);
        Block completionBlock = this.world.getBlockAt(completionPos.x(), completionPos.y(), completionPos.z());
        completionBlock.setType(Material.GOLD_BLOCK);

        this.service.updateAt(player, completionPos, this.world);

        waitFor(() -> completionCalls.get() == 1 && completedEventRef.get() != null);

        assertEquals(player, completedEventRef.get().getCauser());
        assertEquals(blueprint.id(), completedEventRef.get().getStructure().blueprint().id());
        assertTrue(this.service.getStructuresInChunk(ChunkKey.ofBlock(origin, this.world.getUID())).isEmpty());
    }

    private static void waitFor(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(2));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(e);
            }
        }

        fail("Timed out waiting for condition.");
    }
}
