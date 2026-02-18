package com.kntrel.mc.territotem.util;

import java.util.*;
import java.util.function.Function;

public class ChunkCache<T> {

    //FIELDS
    private final Map<ChunkKey, Set<T>> cache_;
    private final Map<ChunkKey, Set<T>> individualCache_;
    private final Function<T, ChunkKey> chunkMapper_;
    private final int proximity_;


    //CONSTRUCTOR
    public ChunkCache(Function<T, ChunkKey> mapper, int proximity) {
        if (proximity < 1) {
            throw new IllegalArgumentException("Proximity must be at least 1");
        }
        this.chunkMapper_ = mapper;
        this.proximity_ = proximity;
        this.cache_ = new HashMap<>();
        this.individualCache_ = new HashMap<>();
    }
    public ChunkCache(Function<T, ChunkKey> mapper) {
        this(mapper,1);
    }


    //API
    public void put(ChunkKey chunk, T element) {
        Set<T> set = this.individualCache_.computeIfAbsent(chunk, k -> new HashSet<>());
        if (!set.add(element)) { return; }

        getAdjacent(chunk, this.proximity_).forEach(c ->
            this.cache_.computeIfAbsent(c, k -> new HashSet<>()).add(element)
        );
    }
    public void put(T element) {
        this.put(this.chunkMapper_.apply(element), element);
    }
    public List<T> get(ChunkKey chunk) {
        Set<T> out = this.cache_.get(chunk);
        if (out == null) { return Collections.emptyList(); }
        return List.copyOf(out);
    }
    public List<T> evict(ChunkKey chunk) {
        Set<T> evicted = this.individualCache_.get(chunk);
        if (evicted == null) { return Collections.emptyList(); }
        for (T element : evicted) {
            getAdjacent(chunk, this.proximity_).forEach(c -> {
                Set<T> set = this.cache_.get(c);
                if (set == null) { return; }
                set.remove(element);
                if (set.isEmpty()) {
                    this.cache_.remove(c);
                }
            });
        }
        Set<T> set = this.individualCache_.remove(chunk);
        return List.copyOf(set);
    }
    public void evict(T element) {
        ChunkKey chunk = this.chunkMapper_.apply(element);
        Set<T> set = this.individualCache_.get(chunk);
        if (set == null || !set.remove(element)) { return; }
        if (set.isEmpty()) { this.individualCache_.remove(chunk); }

        getAdjacent(chunk, this.proximity_).forEach(c -> {
            Set<T> adjacent = this.cache_.get(c);
            if (adjacent == null) { return; }
            adjacent.remove(element);
            if (adjacent.isEmpty()) { this.cache_.remove(c); }
        });
    }



    //HELPERS
    private static List<ChunkKey> getAdjacent(ChunkKey center, int proximity) {
        int minX = center.x() - proximity,
            maxX = center.x() + proximity,
            minZ = center.z() - proximity,
            maxZ = center.z() + proximity;

        List<ChunkKey> out = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                out.add(new ChunkKey(x, z, center.world()));
            }
        }
        return out;
    }
}
