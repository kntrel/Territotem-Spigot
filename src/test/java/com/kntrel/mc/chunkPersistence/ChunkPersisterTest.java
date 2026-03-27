package com.kntrel.mc.chunkPersistence;

import com.kntrel.mc.territotem.test.mock.MockWorld;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChunkPersisterTest {

    private Plugin plugin;
    private ChunkPersister persister;
    private MockWorld world;

    @BeforeEach
    void setUp() {
        this.world = new MockWorld("chunk_persister_test_world");
        this.plugin = mockPlugin(this.world);
        this.persister = new ChunkPersister(this.plugin, 40L);
    }

    @Test
    @DisplayName("persist tracks dirty inserts and retrieves pending value before unload")
    void persistTracksInsertAndRetrieveBeforeUnload() {
        TestChunk testChunk = createTestChunk(3, 7);

        NamespacedKey key = new NamespacedKey("test", "owner");
        this.persister.persist(testChunk.chunk(), key, PersistentDataType.STRING, "alpha");

        assertEquals("alpha", this.persister.retrieve(testChunk.chunk(), key, PersistentDataType.STRING));
        assertNull(testChunk.pdc().get(key, PersistentDataType.STRING));

        this.persister.onChunkUnload(new ChunkUnloadEvent(testChunk.chunk(), true));

        assertEquals("alpha", testChunk.pdc().get(key, PersistentDataType.STRING));
    }

    @Test
    @DisplayName("drop tracks removals and retrieve returns null even when value is currently persisted")
    void dropTracksRemovalAndRetrieveIsNull() {
        TestChunk testChunk = createTestChunk(1, 2);

        NamespacedKey key = new NamespacedKey("test", "deed");
        testChunk.pdc().set(key, PersistentDataType.STRING, "to-be-removed");

        this.persister.drop(testChunk.chunk(), key);

        assertNull(this.persister.retrieve(testChunk.chunk(), key, PersistentDataType.STRING));

        this.persister.onChunkUnload(new ChunkUnloadEvent(testChunk.chunk(), true));

        assertNull(testChunk.pdc().get(key, PersistentDataType.STRING));
    }

    @Test
    @DisplayName("persist after drop clears pending removal and stores replacement")
    void persistAfterDropClearsRemoval() {
        TestChunk testChunk = createTestChunk(-5, 12);

        NamespacedKey key = new NamespacedKey("test", "status");
        testChunk.pdc().set(key, PersistentDataType.STRING, "old");

        this.persister.drop(testChunk.chunk(), key);
        this.persister.persist(testChunk.chunk(), key, PersistentDataType.STRING, "new");

        assertEquals("new", this.persister.retrieve(testChunk.chunk(), key, PersistentDataType.STRING));

        this.persister.onChunkUnload(new ChunkUnloadEvent(testChunk.chunk(), true));

        assertEquals("new", testChunk.pdc().get(key, PersistentDataType.STRING));
    }

    @Test
    @DisplayName("drop after persist removes pending insert")
    void dropAfterPersistRemovesPendingInsert() {
        TestChunk testChunk = createTestChunk(0, 0);

        NamespacedKey key = new NamespacedKey("test", "flag");

        this.persister.persist(testChunk.chunk(), key, PersistentDataType.INTEGER, 11);
        this.persister.drop(testChunk.chunk(), key);

        assertNull(this.persister.retrieve(testChunk.chunk(), key, PersistentDataType.INTEGER));

        this.persister.onChunkUnload(new ChunkUnloadEvent(testChunk.chunk(), true));

        assertNull(testChunk.pdc().get(key, PersistentDataType.INTEGER));
    }

    @Test
    @DisplayName("dirty state is tracked per chunk and unloading one chunk does not flush another")
    void dirtyTrackingIsIndependentPerChunk() {
        TestChunk testChunkA = createTestChunk(9, 9);
        TestChunk testChunkB = createTestChunk(10, 10);

        NamespacedKey key = new NamespacedKey("test", "marker");
        this.persister.persist(testChunkA.chunk(), key, PersistentDataType.STRING, "A");
        this.persister.persist(testChunkB.chunk(), key, PersistentDataType.STRING, "B");

        this.persister.onChunkUnload(new ChunkUnloadEvent(testChunkA.chunk(), true));

        assertEquals("A", testChunkA.pdc().get(key, PersistentDataType.STRING));
        assertNull(testChunkB.pdc().get(key, PersistentDataType.STRING));
        assertEquals("B", this.persister.retrieve(testChunkB.chunk(), key, PersistentDataType.STRING));

        this.persister.onChunkUnload(new ChunkUnloadEvent(testChunkB.chunk(), true));

        assertEquals("B", testChunkB.pdc().get(key, PersistentDataType.STRING));
    }

    @Test
    @DisplayName("flushAll persists dirty chunks")
    void flushAllPersistsDirtyChunks() {
        TestChunk testChunk = createTestChunk(4, -2);

        NamespacedKey key = new NamespacedKey("test", "batch");
        this.persister.persist(testChunk.chunk(), key, PersistentDataType.STRING, "queued");

        this.persister.flushAll();

        assertEquals("queued", testChunk.pdc().get(key, PersistentDataType.STRING));
    }

    @Test
    @DisplayName("retrieve falls back to persisted data when another key is dirty")
    void retrieveFallsBackToPersistedDataWhenOtherKeyIsDirty() {
        TestChunk testChunk = createTestChunk(6, 6);

        NamespacedKey persistedKey = new NamespacedKey("test", "persisted");
        NamespacedKey dirtyKey = new NamespacedKey("test", "dirty");
        testChunk.pdc().set(persistedKey, PersistentDataType.STRING, "stored");

        this.persister.persist(testChunk.chunk(), dirtyKey, PersistentDataType.STRING, "pending");

        assertEquals("stored", this.persister.retrieve(testChunk.chunk(), persistedKey, PersistentDataType.STRING));
    }

    @Test
    @DisplayName("new dirty writes after a flush are persisted on the next flush")
    void dirtyWritesAfterFlushArePersistedOnNextFlush() {
        TestChunk testChunk = createTestChunk(-3, 14);

        NamespacedKey key = new NamespacedKey("test", "version");
        this.persister.persist(testChunk.chunk(), key, PersistentDataType.STRING, "v1");
        this.persister.flushAll();

        this.persister.persist(testChunk.chunk(), key, PersistentDataType.STRING, "v2");
        this.persister.flushAll();

        assertEquals("v2", testChunk.pdc().get(key, PersistentDataType.STRING));
    }

    private TestChunk createTestChunk(int x, int z) {
        Chunk chunk = this.world.getChunkAt(x, z);
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        return new TestChunk(chunk, pdc);
    }

    private static Plugin mockPlugin(MockWorld world) {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask batchTask = mock(BukkitTask.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getWorld(world.getUID())).thenReturn(world);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(batchTask);
        doAnswer(invocation -> {
            assertTrue(invocation.getArgument(0) instanceof Listener);
            return null;
        }).when(pluginManager).registerEvents(any(Listener.class), eq(plugin));

        return plugin;
    }

    private record TestChunk(Chunk chunk, PersistentDataContainer pdc) {}
}
