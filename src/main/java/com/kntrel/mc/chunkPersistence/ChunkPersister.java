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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class ChunkPersister implements Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkPersister.class);


    //FIELDS
    private final Map<ChunkRef, DirtyChunk> dirtyChunks_;
    private final Plugin plugin_;


    //CONSTRUCTOR
    public ChunkPersister(Plugin plugin) {
        this.plugin_ = plugin;
        this.dirtyChunks_ = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    //API
    public <P, C> void persist(Chunk chunk, NamespacedKey key, PersistentDataType<P, C> type, C value) {
        DirtyChunk dirty = this.dirtyChunks_.computeIfAbsent(ChunkRef.of(chunk), ignored -> new DirtyChunk());
        dirty.removals_.remove(key);
        dirty.inserts_.put(key, new Entry(type, value));
    }
    public <P, C> @Nullable C retrieve(Chunk chunk, NamespacedKey key, PersistentDataType<P, C> type) {
        DirtyChunk dirty = this.dirtyChunks_.get(ChunkRef.of(chunk));
        if (dirty == null) {
            return chunk.getPersistentDataContainer().get(key, type);
        }

        if (dirty.removals_.contains(key)) { return null; }
        Entry pending = dirty.inserts_.get(key);
        if (pending == null) { return null; }
        return castValue(pending.value_);
    }
    public void drop(Chunk chunk, NamespacedKey key) {
        DirtyChunk dirty = this.dirtyChunks_.computeIfAbsent(ChunkRef.of(chunk), ignored -> new DirtyChunk());
        dirty.inserts_.remove(key);
        dirty.removals_.add(key);
    }
    public void flushChunk(Chunk chunk) {
        DirtyChunk dirty = this.dirtyChunks_.remove(ChunkRef.of(chunk));
        if (dirty == null) { return; }

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : dirty.removals_) {
            LOGGER.debug("Removing data entry from chunk ({}, {}). Key: {}", chunk.getX(), chunk.getZ(), key);
            pdc.remove(key);
        }
        for (Map.Entry<NamespacedKey, Entry> entry : dirty.inserts_.entrySet()) {
            NamespacedKey key = entry.getKey();
            Entry val = entry.getValue();
            LOGGER.debug("Persisting data to chunk ({}, {}). Key: {}, Type: {}", chunk.getX(), chunk.getZ(), key, val.value_);
            setUnchecked(pdc, key, val);
        }
    }
    public void flushAll() {
        for (ChunkRef ref : this.dirtyChunks_.keySet()) {
            World world = this.plugin_.getServer().getWorld(ref.world());
            if (world == null) {
                LOGGER.error("Trying to persist data to a chunk in word {}, but such a world wasn't found", ref.world());
                continue;
            }
            Chunk chunk = world.getChunkAt(ref.x(), ref.z());
            this.flushChunk(chunk);
        }
    }


    //LISTERNER
    @EventHandler void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        this.flushChunk(chunk);
    }


    //HELPERS
    @SuppressWarnings("unchecked")
    private static <P, C> void setUnchecked(PersistentDataContainer pdc, NamespacedKey key, Entry entry) {
        pdc.set(key, (PersistentDataType<P, C>) entry.type_, (C) entry.value_);
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
    private static class DirtyChunk {
        private final Map<NamespacedKey, Entry> inserts_ = new ConcurrentHashMap<>();
        private final Set<NamespacedKey> removals_ = ConcurrentHashMap.newKeySet();
    }
    private record Entry(PersistentDataType<?, ?> type_, Object value_) { }
}
