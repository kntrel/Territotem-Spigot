package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistration;
import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistrationBuilder;
import com.kntrel.mc.territotem.structure.event.*;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import com.kntrel.mc.territotem.util.ChunkCache;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Triplet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;


public class StructureService {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureService.class);
    private static final String STRUCTURES_KEY = "structures";


    //FIELDS
    private final Plugin plugin_;
    private final ChunkPersister chunkPersister_;
    private final Map<Long, Blueprint> blueprints_;
    private final ChunkCache<Structure> structuresByChunk_;
    private final Map<UUID, Structure> structuresById_;
    private final Map<Structure, ReentrantLock> structureLocks_;
    private final Map<Triplet<Vec3i, UUID, Long>, Structure> trackersMap_;
    private final Executor executor_;
    private final NamespacedKey structuresNSK_;
    private final BlueprintBitsetGenerator bitsetGenerator_;
    private final StructureServiceListener listener_;
    private final Object lock_;


    //CONSTRUCTOR
    public StructureService(Plugin plugin, ChunkPersister chunkPersister) {
        this.plugin_ = plugin;
        this.chunkPersister_ = chunkPersister;
        this.blueprints_ = new ConcurrentHashMap<>();
        this.structuresByChunk_ = new ChunkCache<>(c -> ChunkKey.ofBlock(c.origin(), c.world().getUID()));
        this.structuresById_ = new ConcurrentHashMap<>();
        this.structureLocks_ = new ConcurrentHashMap<>();
        this.trackersMap_ = new ConcurrentHashMap<>();
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.structuresNSK_ = new NamespacedKey(this.plugin_, STRUCTURES_KEY);
        this.bitsetGenerator_ = new BlueprintBitsetGenerator();
        this.listener_ = new StructureServiceListener(this);
        this.lock_ = new Object();

        this.getServer().getPluginManager().registerEvents(this.listener_, this.plugin_);
        LOGGER.info("Tracking totem structures");
    }


    //SERVICES
    public Plugin getPlugin() {
        return this.plugin_;
    }
    public Server getServer() {
        return this.plugin_.getServer();
    }
    public Structure track(Blueprint blueprint, Vec3i origin, World world, Entity causer) {
        return this.track(blueprint, origin, world, causer, UUID.randomUUID(), null);
    }
    public void registerBlueprint(BlueprintRegistration registration) {
        this.registerBlueprintInner(registration.blueprint());
        this.listener_.listen(registration);
    }
    public BlueprintRegistrationBuilder.Completer registerBlueprint(Blueprint blueprint) {
        this.registerBlueprintInner(blueprint);
        return BlueprintRegistrationBuilder.forBlueprint(blueprint, r -> {
            if (r.eventClass() == null) { return; }
            this.listener_.listen(r);
        });
    }
    public void registerUntrackedBlueprint(Blueprint blueprint) {
        this.registerBlueprintInner(blueprint);
    }
    public Structure placeAt(Blueprint blueprint, Vec3i location, World world) {
        for (Tile piece : blueprint.pieces()) {
            Vec3i loc = location.add(piece.offset());
            piece.place(WorldTileWriter.of(loc, world));
        }
        return this.newStructure(blueprint, world, location, UUID.randomUUID());
    }
    public void updateAt(Entity who, Vec3i where, World world) {
        // Execute structure update task asynchronously
        this.executor_.execute(() -> handleStructuresUpdateTask(who, where, world));
    }
    public void dropStructure(Structure structure) {
        this.unloadStructure(structure);
        this.persistStructuresInChunk(ChunkKey.ofBlock(structure.origin(), structure.world().getUID()));
        LOGGER.debug("Dropped structure for blueprint {} at origin {}", structure.blueprint().id(), structure.origin());
    }
    public Collection<Structure> getStructuresInChunk(ChunkKey chunk) {
        Collection<Structure> structures = List.copyOf(this.structuresByChunk_.get(chunk));
        LOGGER.trace("Retrieved {} structures from chunk {}", structures.size(), chunk);
        return structures;
    }
    public Structure get(UUID structureId) {
        return this.structuresById_.get(structureId);
    }


    //PACKAGE-PRIVATE SERVICES
    BlueprintBitsetGenerator getBitsetGenerator() {
        return this.bitsetGenerator_;
    }
    <T> CompletableFuture<T> runInMainThreadAsync(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                T res = task.call();
                return CompletableFuture.completedFuture(res);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        this.plugin_.getServer().getScheduler().runTask(this.plugin_, () -> {
            try {
                future.complete(task.call());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    <T> T runInMainThread(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        FutureTask<T> future = new FutureTask<>(task);
        this.plugin_.getServer().getScheduler().runTask(this.plugin_, future);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    //LISTENERS
    void handleChunkLoad(Chunk chunk) {
        List<StructureChunkData> data = this.chunkPersister_.retrieve(chunk, this.structuresNSK_, StructurePersistentDataType.instance());
        if (data == null || data.isEmpty()) {
            LOGGER.trace("No totem data found in chunk [{}, {}]", chunk.getX(), chunk.getZ());
            return;
        }

        LOGGER.info("Found {} totem data in chunk [{}, {}]", data.size(), chunk.getX(), chunk.getZ());
        for (StructureChunkData structureData : data) {
            Blueprint blueprint = this.blueprints_.get(structureData.blueprintId());
            if (blueprint == null) {
                LOGGER.warn("Blueprint with ID {} not found in registry", structureData.blueprintId());
                continue;
            }

            Vec3i origin = new Vec3i(chunk.getX(), 0, chunk.getZ())
                    .shiftLeft(Constants.CHUNK_SHIFT)
                    .add(structureData.offset());

            UUID structureId = normalizeStructureId(structureData.structureId());
            LOGGER.debug("Restoring totem structure for blueprint {} at origin {}", blueprint.id(), origin);
            this.executor_.execute(() -> {
                Structure s = this.track(blueprint, origin, chunk.getWorld(), null, structureId, structureData.state());
                this.runInMainThread(() -> {
                    this.getServer().getPluginManager().callEvent(new StructureLoadedEvent(s));
                    return null;
                });
            });
        }
    }
    void handleChunkUnload(Chunk chunk) {
        ChunkKey ck = new ChunkKey(chunk.getX(), chunk.getZ(), chunk.getWorld());
        for (Structure structure : this.getStructuresInChunk(ck)) {
            this.unloadStructure(structure);
            this.getServer().getPluginManager().callEvent(new StructureUnloadedEvent(structure));
        }
    }


    //TASKS
    private void handleStructuresUpdateTask(Entity who, Vec3i where, World world) {
        ChunkKey chunk = ChunkKey.ofBlock(where, world.getUID());
        Collection<Structure> structures = this.getStructuresInChunk(chunk);

        for (Structure structure : structures) {
            this.executor_.execute(() -> updateStructureTask(who, structure, where));
        }
    }
    private void updateStructureTask(Entity who, Structure structure, Vec3i where) {
        LOGGER.trace("Updating structure {} at position {}", structure.blueprint().id(), where);
        UpdateResult result = this.updateStructure(structure, where);
        if (result.unchanged()) { return; }

        UpdateResult.Change change = result.getChange().orElse(null);
        if (change == null) { return; }         //Should never return here


        this.persistStructuresInChunk(ChunkKey.ofBlock(structure.origin(), structure.world().getUID()));
        if (change.wasCompleted()) {
            this.handleComplete(structure, who, change);
        } else if (change.wasUncompleted()) {
            this.handleUncompleted(structure, who, change);
        } else {
            this.handleChange(structure, who, change);
        }
    }
    private void handleChange(Structure structure, Entity who, UpdateResult.Change change) {
        LOGGER.info("Structure {} changed", structure.id());

        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureChangedEvent(structure, who, change.oldState(), change.newState(), change.piece(), change.match()));
            return null;
        });
    }
    private void handleComplete(Structure structure, Entity who, UpdateResult.Change change) {
        LOGGER.info("Structure {} completed", structure.id());

        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureCompletedEvent(structure, who, change.oldState(), change.piece()));
            return null;
        });
    }
    private void handleUncompleted(Structure structure, Entity who, UpdateResult.Change change) {
        LOGGER.info("Structure {} uncompleted", structure.id());

        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureUncompletedEvent(structure, who, change.newState(), change.piece()));
            return null;
        });
    }
    private UpdateResult updateStructure(Structure structure, Vec3i where) {
        if (!structure.contains(where)) {
            LOGGER.trace("Position {} is not within structure bounds", where);
            return UpdateResult.noChange();
        }

        ReentrantLock lock = this.structureLocks_.get(structure);
        if (lock == null) {
            LOGGER.warn("No lock found for structure at {}", structure.origin());
            return UpdateResult.noChange();
        }

        Vec3i offset = where.subtract(structure.origin());

        lock.lock();
        try {
            LOGGER.debug("Updating structure of {} at offset {}", structure.blueprint().name(), offset);
            return structure.update(offset);
        } finally {
            lock.unlock();
        }
    }


    //HELPERS
    private Structure track(Blueprint blueprint, Vec3i origin, World world, Entity causer, UUID structureId, Structure.State persistedState) {
        LOGGER.debug("New structure started at {}", origin);
        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(origin, world.getUID(), blueprint.id());

        Structure structure;
        synchronized (this.lock_) {
            structure = this.trackersMap_.get(trackKey);
            if (structure != null) {
                LOGGER.debug("Already tracking a structure of 'blueprint' {} at {}", blueprint.id(), origin);
                return structure;
            }

            UUID assignedStructureId = normalizeStructureId(structureId);
            structure = this.runInMainThread(() -> this.newStructure(blueprint, world, origin, assignedStructureId));
            this.trackersMap_.put(trackKey, structure);
            this.structuresByChunk_.put(structure);
            this.structuresById_.put(structure.id(), structure);
            this.structureLocks_.put(structure, new ReentrantLock());
        }

        Structure.State previousState = (persistedState == null)
                ? Structure.State.EMPTY
                : persistedState;
        UpdateResult.Change change = UpdateResult.change(null, false, previousState, structure.getState());
        if (change.wasCompleted()) {
            this.handleComplete(structure, causer, change);
        }
        if (change.wasUncompleted()) {
            this.handleUncompleted(structure, causer, change);
        }

        this.persistStructuresInChunk(ChunkKey.ofBlock(structure.origin(), structure.world().getUID()));
        return structure;
    }
    private void unloadStructure(Structure structure) {
        LOGGER.trace("Unloading structure for blueprint {} at origin {}", structure.blueprint().id(), structure.origin());
        this.structuresByChunk_.evict(structure);
        this.structureLocks_.remove(structure);

        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(structure.origin(), structure.world().getUID(), structure.blueprint().id());
        this.trackersMap_.remove(trackKey);
    }
    protected Structure newStructure(Blueprint blueprint, World world, Vec3i origin, UUID structureId) {
        return new Structure(structureId, this, blueprint, world, origin);
    }
    private static Vec3i offsetInChunk(Vec3i src) {
        return new Vec3i(
                Math.floorMod(src.x(), Constants.CHUNK_SIZE),
                src.y(),
                Math.floorMod(src.z(), Constants.CHUNK_SIZE)
        );
    }
    private static UUID normalizeStructureId(UUID structureId) {
        if (structureId == null || StructureChunkData.UNASSIGNED_ID.equals(structureId)) {
            return UUID.randomUUID();
        }
        return structureId;
    }
    public void registerBlueprintInner(Blueprint blueprint) {
        long id = blueprint.id();
        if (blueprints_.containsKey(id)) {
            LOGGER.error("Tried to register a blueprint with ID " + id + " one with the same ID was already registered");
            throw new IllegalArgumentException("A blueprint with ID " + id + " is already registered");
        }

        this.blueprints_.put(id, blueprint);
    }

    private void persistStructuresInChunk(ChunkKey chunkKey) {
        World world = this.plugin_.getServer().getWorld(chunkKey.world());
        if (world == null || !world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
            return;
        }

        Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());
        Collection<Structure> structures = this.getStructuresInChunk(chunkKey);
        if (structures.isEmpty()) {
            this.chunkPersister_.drop(chunk, this.structuresNSK_);
            return;
        }

        List<StructureChunkData> toSerialize = new ArrayList<>(structures.size());
        for (Structure structure : structures) {
            toSerialize.add(new StructureChunkData(offsetInChunk(structure.origin()), structure.blueprint().id(), structure.id(), structure.getState()));
        }
        this.chunkPersister_.persist(chunk, this.structuresNSK_, StructurePersistentDataType.instance(), toSerialize);
    }
}
