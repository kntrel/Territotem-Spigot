package com.kntrel.mc.chunkPersistence;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


public class ChunkPersister implements Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkPersister.class);


    //FIELDS
    private final Map<ChunkRef, DirtyChunk> dirtyChunks_;
    private final Plugin plugin_;
    private final @Nullable BukkitTask batchTask_;


    //CONSTRUCTOR
    public ChunkPersister(Plugin plugin) {
        this(plugin, 0L);
    }
    public ChunkPersister(Plugin plugin, long batchPeriodTicks) {
        this.plugin_ = plugin;
        this.dirtyChunks_ = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.batchTask_ = batchPeriodTicks > 0
                ? plugin.getServer().getScheduler().runTaskTimer(plugin, this::flushAll, batchPeriodTicks, batchPeriodTicks)
                : null;
    }


    //API
    public <P, C> void persist(Chunk chunk, NamespacedKey key, PersistentDataType<P, C> type, C value) {
        ChunkRef ref = ChunkRef.of(chunk);
        this.dirtyChunks_.compute(ref, (_, existing) -> {
            DirtyChunk dirty = existing == null ? new DirtyChunk() : existing;
            dirty.persist(key, new Entry(type, value));
            return dirty;
        });
    }
    public <P, C> @Nullable C retrieve(Chunk chunk, NamespacedKey key, PersistentDataType<P, C> type) {
        DirtyChunk dirty = this.dirtyChunks_.get(ChunkRef.of(chunk));
        if (dirty == null) {
            return chunk.getPersistentDataContainer().get(key, type);
        }

        DirtyChunk.Lookup lookup = dirty.lookup(key);
        if (lookup.removed()) {
            return null;
        }
        if (lookup.entry() != null) {
            return castValue(lookup.entry().value);
        }
        return chunk.getPersistentDataContainer().get(key, type);
    }
    public void drop(Chunk chunk, NamespacedKey key) {
        ChunkRef ref = ChunkRef.of(chunk);
        this.dirtyChunks_.compute(ref, (ignored, existing) -> {
            DirtyChunk dirty = existing == null ? new DirtyChunk() : existing;
            dirty.drop(key);
            return dirty;
        });
    }
    public void flushChunk(Chunk chunk) {
        this.flushChunk(ChunkRef.of(chunk), chunk);
    }
    public void flushAll() {
        for (ChunkRef ref : Set.copyOf(this.dirtyChunks_.keySet())) {
            World world = this.plugin_.getServer().getWorld(ref.world());
            if (world == null) {
                LOGGER.error("Trying to persist data to a chunk in word {}, but such a world wasn't found", ref.world());
                continue;
            }
            Chunk chunk = world.getChunkAt(ref.x(), ref.z());
            this.flushChunk(ref, chunk);
        }
    }
    public void shutdown() {
        if (this.batchTask_ != null) {
            this.batchTask_.cancel();
        }
        this.flushAll();
    }


    //LISTERNER
    @EventHandler void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        this.flushChunk(ChunkRef.of(chunk), chunk);
    }


    //HELPERS
    private void flushChunk(ChunkRef ref, Chunk chunk) {
        DirtyChunk dirty = this.takeSnapshot(ref);
        if (dirty == null) { return; }

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : dirty.removals) {
            LOGGER.debug("Removing data entry from chunk ({}, {}). Key: {}", chunk.getX(), chunk.getZ(), key);
            pdc.remove(key);
        }
        for (Map.Entry<NamespacedKey, Entry> entry : dirty.inserts.entrySet()) {
            NamespacedKey key = entry.getKey();
            Entry val = entry.getValue();
            LOGGER.debug("Persisting data to chunk ({}, {}). Key: {}, Type: {}", chunk.getX(), chunk.getZ(), key, val.value);
            setUnchecked(pdc, key, val);
        }
    }
    private @Nullable DirtyChunk takeSnapshot(ChunkRef ref) {
        AtomicReference<DirtyChunk> snapshot = new AtomicReference<>();
        this.dirtyChunks_.computeIfPresent(ref, (ignored, dirty) -> {
            snapshot.set(dirty.snapshot());
            return null;
        });
        return snapshot.get();
    }
    @SuppressWarnings("unchecked")
    private static <P, C> void setUnchecked(PersistentDataContainer pdc, NamespacedKey key, Entry entry) {
        pdc.set(key, (PersistentDataType<P, C>) entry.type, (C) entry.value);
    }
    @SuppressWarnings("unchecked")
    private static <C> C castValue(Object value) {
        return (C) value;
    }


    //SUBTYPES
    private record ChunkRef(UUID world, int x, int z) {
        static ChunkRef of(Chunk chunk) {
            return new ChunkRef(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    private record DirtyChunk(Map<NamespacedKey, Entry> inserts, Set<NamespacedKey> removals) {
        DirtyChunk() {
            this(new HashMap<>(), new HashSet<>());
        }

        private synchronized void persist(NamespacedKey key, Entry entry) {
            this.removals.remove(key);
            this.inserts.put(key, entry);
        }

        private synchronized void drop(NamespacedKey key) {
            this.inserts.remove(key);
            this.removals.add(key);
        }

        private synchronized Lookup lookup(NamespacedKey key) {
            return new Lookup(this.removals.contains(key), this.inserts.get(key));
        }

        private synchronized DirtyChunk snapshot() {
            return new DirtyChunk(new HashMap<>(this.inserts), new HashSet<>(this.removals));
        }

        private record Lookup(boolean removed, @Nullable Entry entry) {}
    }
    private record Entry(PersistentDataType<?, ?> type, Object value) { }
}
