package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.SetMap;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import java.util.*;

class TotemStore {

    //FIELDS
    private final Map<UUID, Totem> byId_;
    private final Map<Long, Totem> byRegion_;
    private final SetMap<ChunkKey, Totem> byChunk_;
    private final SetMap<Long, Totem> byBlueprint_;
    private final Object mutex_;


    //CONSTRUCTOR
    TotemStore() {
        this.byId_ = new HashMap<>();
        this.byRegion_ = new HashMap<>();
        this.byChunk_ = new SetMap<>();
        this.byBlueprint_ = new SetMap<>();
        this.mutex_ = new Object();
    }


    //API - BY CHUNK
    public List<Totem> getAtChunk(ChunkKey chunk) {
        Set<Totem> l;
        synchronized (this.mutex_) {
            l = this.byChunk_.get(chunk);
        }
        if (l == null) { return Collections.emptyList(); }
        return List.copyOf(l);
    }
    public List<Totem> getAtChunk(int x, int z, World world) {
        return this.getAtChunk(new ChunkKey(x, z, world.getUID()));
    }
    public boolean isTotemAt(Vec3i coordinates, World world) {
        ChunkKey chunkKey = ChunkKey.ofBlock(coordinates, world.getUID());
        Set<Totem> totems;

        synchronized (this.mutex_) {
            totems = this.byChunk_.get(chunkKey);
        }

        if (totems == null) { return false; }
        for (Totem t : totems) {
            if (t.origin().equals(coordinates) && t.world().getUID().equals(world.getUID())) {
                return true;
            }
        }
        return false;
    }
    public boolean isTotemAt(int x, int y, int z, World world) {
        return isTotemAt(new Vec3i(x, y, z), world);
    }

    //API - BY ID
    public Optional<Totem> get(UUID id) {
        synchronized (this.mutex_) {
            return Optional.ofNullable(this.byId_.get(id));
        }
    }
    public boolean has(UUID id) {
        return this.byId_.containsKey(id);
    }
    public boolean has(Totem totem) {
        return this.has(totem.id());
    }

    //API - BY REGION
    public Optional<Totem> getFromRegion(long regionId) {
        synchronized (this.mutex_) {
            return Optional.ofNullable(this.byRegion_.get(regionId));
        }
    }
    public Optional<Totem> getFromRegion(Region region) {
        return this.getFromRegion(region.getId());
    }

    //API - BY STRUCTURE
    public Optional<Totem> getFromStructure(Structure structure) {
        return this.get(structure.id());
    }

    //API - BY BLUEPRINT
    public List<Totem> getByBlueprint(long id) {
        Set<Totem> l;
        synchronized (this.mutex_) {
            l = this.byBlueprint_.get(id);
        }
        if (l == null) { return Collections.emptyList(); }
        return List.copyOf(l);
    }
    public List<Totem> getByBlueprint(Blueprint blueprint) {
        return this.getByBlueprint(blueprint.id());
    }

    //API - ADD
    public void add(Totem totem) {
        synchronized (this.mutex_) {
            if (this.byId_.containsKey(totem.id())) { return; }

            this.byId_.put(totem.id(), totem);

            this.byRegion_.put(totem.region().getId(), totem);

            ChunkKey chunkKey = ChunkKey.ofBlock(totem.origin(), totem.world().getUID());
            this.byChunk_.putInto(chunkKey, totem);

            long blueprintId = totem.blueprint().id();
            this.byBlueprint_.putInto(blueprintId, totem);
        }
    }

    //API - REMOVE
    public Totem remove(UUID id) {
        synchronized (this.mutex_) {
            Totem totem = this.byId_.remove(id);
            if (totem == null) { return null; }

            this.byRegion_.remove(totem.region().getId());

            ChunkKey chunkKey = ChunkKey.ofBlock(totem.origin(), totem.world().getUID());
            Set<Totem> set = this.byChunk_.get(chunkKey);
            if (set != null) {
                set.remove(totem);
                if (set.isEmpty()) {
                    this.byChunk_.remove(chunkKey);
                }
            }

            long blueprintId = totem.blueprint().id();
            set = this.byBlueprint_.get(blueprintId);
            if (set != null) {
                set.remove(totem);
                if (set.isEmpty()) {
                    this.byBlueprint_.remove(blueprintId);
                }
            }

            return totem;
        }
    }
    public boolean remove(Totem totem) {
        return this.remove(totem.id()) != null;
    }
    public Totem removeByRegion(long id) {
        synchronized (this.mutex_) {
            Totem toRemove = this.byRegion_.get(id);
            if (toRemove == null) { return null; }
            return this.remove(toRemove.id());
        }
    }
    public Totem removeByRegion(Region region) {
        return this.removeByRegion(region.getId());
    }
    public List<Totem> removeAllFromChunk(ChunkKey chunk) {
        synchronized (this.mutex_) {
            Set<Totem> toRemove = this.byChunk_.remove(chunk);
            if (toRemove == null || toRemove.isEmpty()) { return Collections.emptyList(); }

            for (Totem t : toRemove) { this.remove(t.id()); }
            return List.copyOf(toRemove);
        }
    }
    public List<Totem> removeAllFromChunk(int x, int z, World world) {
        return this.removeAllFromChunk(new ChunkKey(x, z, world.getUID()));
    }
}