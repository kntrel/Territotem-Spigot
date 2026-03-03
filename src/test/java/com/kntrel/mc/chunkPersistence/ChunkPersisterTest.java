package com.kntrel.mc.chunkPersistence;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChunkPersisterTest {

    private Plugin plugin;
    private ChunkPersister persister;
    private UUID worldId;

    @BeforeEach
    void setUp() {
        this.plugin = mockPlugin();
        this.persister = new ChunkPersister(this.plugin);
        this.worldId = UUID.randomUUID();
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

    private TestChunk createTestChunk(int x, int z) {
        PersistentDataContainer pdc = mockPersistentDataContainer();
        Chunk chunk = mockChunk(this.worldId, x, z, pdc);
        return new TestChunk(chunk, pdc);
    }

    private static Plugin mockPlugin() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        doAnswer(invocation -> {
            assertTrue(invocation.getArgument(0) instanceof Listener);
            return null;
        }).when(pluginManager).registerEvents(any(Listener.class), eq(plugin));

        return plugin;
    }

    private static Chunk mockChunk(UUID worldId, int x, int z, PersistentDataContainer pdc) {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldId);

        Chunk chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(x);
        when(chunk.getZ()).thenReturn(z);
        when(chunk.getPersistentDataContainer()).thenReturn(pdc);
        return chunk;
    }

    @SuppressWarnings("unchecked")
    private static PersistentDataContainer mockPersistentDataContainer() {
        record StoredEntry(PersistentDataType<?, ?> type, Object value) {}
        Map<NamespacedKey, StoredEntry> values = new HashMap<>();

        PersistentDataContainer pdc = mock(PersistentDataContainer.class);

        doAnswer(invocation -> {
            NamespacedKey key = invocation.getArgument(0);
            PersistentDataType<?, ?> type = invocation.getArgument(1);
            Object value = invocation.getArgument(2);
            values.put(key, new StoredEntry(type, value));
            return null;
        }).when(pdc).set(any(NamespacedKey.class), any(PersistentDataType.class), any());

        when(pdc.get(any(NamespacedKey.class), any(PersistentDataType.class))).thenAnswer(invocation -> {
            NamespacedKey key = invocation.getArgument(0);
            PersistentDataType<?, ?> expectedType = invocation.getArgument(1);
            StoredEntry entry = values.get(key);
            if (entry == null || !entry.type.equals(expectedType)) {
                return null;
            }
            return entry.value;
        });

        doAnswer(invocation -> {
            NamespacedKey key = invocation.getArgument(0);
            values.remove(key);
            return null;
        }).when(pdc).remove(any(NamespacedKey.class));

        when(pdc.has(any(NamespacedKey.class))).thenAnswer(invocation -> values.containsKey(invocation.getArgument(0)));

        when(pdc.has(any(NamespacedKey.class), any(PersistentDataType.class))).thenAnswer(invocation -> {
            NamespacedKey key = invocation.getArgument(0);
            PersistentDataType<?, ?> type = invocation.getArgument(1);
            StoredEntry entry = values.get(key);
            return entry != null && entry.type.equals(type);
        });

        when(pdc.isEmpty()).thenAnswer(invocation -> values.isEmpty());
        when(pdc.getKeys()).thenAnswer(invocation -> new HashSet<>(values.keySet()));

        return pdc;
    }

    private record TestChunk(Chunk chunk, PersistentDataContainer pdc) {}
}
