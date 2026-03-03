package com.kntrel.mc.chunkPersistence;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkPersister implements Listener {

    private final Map<ChunkRef, DirtyChunk> dirtyChunks_;

    public ChunkPersister(Plugin plugin) {
        this.dirtyChunks_ = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public <P, C> void persist(Chunk chunk, NamespacedKey key, PersistentDataType<P, C> type, C value) {
        DirtyChunk dirty = this.dirtyChunks_.computeIfAbsent(ChunkRef.of(chunk), ignored -> new DirtyChunk());
        dirty.removals_.remove(key);
        dirty.upserts_.put(key, new Entry(type, value));
    }

    public <P, C> C retrieve(Chunk chunk, NamespacedKey key, PersistentDataType<P, C> type) {
        DirtyChunk dirty = this.dirtyChunks_.get(ChunkRef.of(chunk));
        if (dirty != null) {
            if (dirty.removals_.contains(key)) {
                return null;
            }
            Entry pending = dirty.upserts_.get(key);
            if (pending != null) {
                return castValue(pending.value_);
            }
        }

        return chunk.getPersistentDataContainer().get(key, type);
    }

    public void drop(Chunk chunk, NamespacedKey key) {
        DirtyChunk dirty = this.dirtyChunks_.computeIfAbsent(ChunkRef.of(chunk), ignored -> new DirtyChunk());
        dirty.upserts_.remove(key);
        dirty.removals_.add(key);
    }

    public <P, C> void persist(NamespacedKey key, PersistentDataType<P, C> type, C value) {
        throw new UnsupportedOperationException("Use chunk-aware persist(chunk, key, type, value)");
    }

    public <P, C> C retrieve(NamespacedKey key, PersistentDataType<P, C> type) {
        throw new UnsupportedOperationException("Use chunk-aware retrieve(chunk, key, type)");
    }

    public void drop(NamespacedKey key) {
        throw new UnsupportedOperationException("Use chunk-aware drop(chunk, key)");
    }

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        DirtyChunk dirty = this.dirtyChunks_.remove(ChunkRef.of(chunk));
        if (dirty == null) {
            return;
        }

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : dirty.removals_) {
            pdc.remove(key);
        }
        for (Map.Entry<NamespacedKey, Entry> entry : dirty.upserts_.entrySet()) {
            setUnchecked(pdc, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static <P, C> void setUnchecked(PersistentDataContainer pdc, NamespacedKey key, Entry entry) {
        pdc.set(key, (PersistentDataType<P, C>) entry.type_, (C) entry.value_);
    }

    @SuppressWarnings("unchecked")
    private static <C> C castValue(Object value) {
        return (C) value;
    }

    private record ChunkRef(UUID world, int x, int z) {
        static ChunkRef of(Chunk chunk) {
            return new ChunkRef(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    private static class DirtyChunk {
        private final Map<NamespacedKey, Entry> upserts_ = new ConcurrentHashMap<>();
        private final Set<NamespacedKey> removals_ = ConcurrentHashMap.newKeySet();
    }

    private record Entry(PersistentDataType<?, ?> type_, Object value_) { }
}
