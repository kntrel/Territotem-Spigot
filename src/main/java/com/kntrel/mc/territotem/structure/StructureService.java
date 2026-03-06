package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.territotem.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.event.StructureUncompletedEvent;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistration;
import com.kntrel.mc.territotem.structure.blueprint.BlueprintRegistrationBuilder;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTileWriter;
import com.kntrel.mc.territotem.util.ChunkCache;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Triplet;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;


public class StructureService {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureService.class);
    private static final String STRUCTURES_KEY = "structures";


    //FIELDS
    private final Plugin plugin_;
    private final ChunkPersister chunkPersister_;
    private final Map<Long, Blueprint> blueprints_;
    private final ChunkCache<Structure> candidatesByChunk_;
    private final Map<Structure, ReentrantLock> candidateLocks_;
    private final Map<Triplet<Vec3i, UUID, Long>, Structure> trackersMap_;
    private final Executor executor_;
    private final NamespacedKey candidatesNSK_;
    private final BlueprintBitsetGenerator bitsetGenerator_;
    private final StructureServiceListener listener_;



    //CONSTRUCTOR
    public StructureService(Plugin plugin, ChunkPersister chunkPersister) {
        this.plugin_ = plugin;
        this.chunkPersister_ = chunkPersister;
        this.blueprints_ = new ConcurrentHashMap<>();
        this.candidatesByChunk_ = new ChunkCache<>(c -> ChunkKey.ofBlock(c.origin(), c.world().getUID()));
        this.candidateLocks_ = new ConcurrentHashMap<>();
        this.trackersMap_ = new  ConcurrentHashMap<>();
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.candidatesNSK_ = new NamespacedKey(this.plugin_, STRUCTURES_KEY);
        this.bitsetGenerator_ = new BlueprintBitsetGenerator();
        this.listener_ = new StructureServiceListener(this);

        this.getServer().getPluginManager().registerEvents(this.listener_, this.plugin_);
        LOGGER.info("Tracking totem candidates");
    }


    //SERVICES
    public Plugin getPlugin() {
        return this.plugin_;
    }
    public Server getServer() {
        return this.plugin_.getServer();
    }
    public Structure track(Blueprint blueprint, Vec3i origin, World world, Entity causer) {

        LOGGER.debug("New structure started at {}", origin);
        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(origin, world.getUID(), blueprint.id());
        Structure structure = this.trackersMap_.get(trackKey);
        if (structure != null) {
            LOGGER.debug("Already tracking a structure of 'blueprint' {} at {}", blueprint.id(), origin);
            return structure;
        }

        structure = this.runInMainThread(() -> this.newTracker(blueprint, world, origin));
        if (structure.isComplete()) {
            LOGGER.debug("Structure at {} was complete right away.", origin);
            this.handleComplete(structure, causer, new StateChange(Structure.State.EMPTY, Structure.State.COMPLETE));
            return structure;
        }

        this.trackersMap_.put(trackKey, structure);
        this.candidatesByChunk_.put(structure);
        this.candidateLocks_.put(structure, new ReentrantLock());
        this.persistStructuresInChunk(ChunkKey.ofBlock(structure.origin(), structure.world().getUID()));

        return structure;
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
        return new Structure(this, blueprint, world, location);
    }
    public void updateAt(Entity who, Vec3i where, World world) {
        // Execute candidate update task asynchronously
        this.executor_.execute(() -> handleStructuresUpdateTask(who, where, world));
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
    public void dropStructure(Structure candidate) {
        LOGGER.debug("Dropping candidate for blueprint {} at origin {}", candidate.blueprint().id(), candidate.origin());
        this.candidatesByChunk_.evict(candidate);
        this.candidateLocks_.remove(candidate);

        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(candidate.origin(), candidate.world().getUID(), candidate.blueprint().id());
        this.trackersMap_.remove(trackKey);
        this.persistStructuresInChunk(ChunkKey.ofBlock(candidate.origin(), candidate.world().getUID()));
    }
    public Collection<Structure> getStructuresInChunk(ChunkKey chunk) {
        Collection<Structure> candidates = List.copyOf(this.candidatesByChunk_.get(chunk));
        LOGGER.trace("Retrieved {} candidates from chunk {}", candidates.size(), chunk);
        return candidates;
    }


    //LISTENERS
    void handleChunkLoad(Chunk chunk) {
        List<StructureChunkData> data = this.chunkPersister_.retrieve(chunk, this.candidatesNSK_, StructureCandidatePersistentDataType.instance());
        if (data == null || data.isEmpty()) {
            LOGGER.trace("No totem data found in chunk [{}, {}]", chunk.getX(), chunk.getZ());
            return;
        }

        LOGGER.info("Found {} totem data in chunk [{}, {}]", data.size(), chunk.getX(), chunk.getZ());
        for (StructureChunkData candidate : data) {
            Blueprint blueprint = blueprints_.get(candidate.blueprintId());
            if (blueprint == null) { 
                LOGGER.warn("Blueprint with ID {} not found in registry", candidate.blueprintId());
                continue;
            }

            Vec3i origin = new Vec3i(chunk.getX(), 0, chunk.getZ())
                    .shiftLeft(Constants.CHUNK_SHIFT)
                    .add(candidate.offset());

            LOGGER.debug("Restoring totem candidate for blueprint {} at origin {}", blueprint.id(), origin);
            this.executor_.execute(() -> this.track(blueprint, origin, chunk.getWorld(), null));
        }
    }


    //TASKS
    private void handleStructuresUpdateTask(Entity who, Vec3i where, World world) {
        ChunkKey chunk = ChunkKey.ofBlock(where, world.getUID());
        Collection<Structure> candidates = this.getStructuresInChunk(chunk);

        for (Structure candidate : candidates) {
            this.executor_.execute(() -> updateStructureTask(who, candidate, where));
        }
    }
    private void updateStructureTask(Entity who, Structure structure, Vec3i where) {
        LOGGER.trace("Updating structure {} at position {}", structure.blueprint().id(), where);
        StateChange change = this.updateStructure(structure, where);
        if (change == null || change.noChanges()) { return; }
        if (change.wasCompleted()) {
            this.handleComplete(structure, who, change);
        }
        if (change.wasUncompleted()) {
            this.handleUncompleted(structure, who, change);
        }
    }
    private void handleComplete(Structure candidate, Entity who, StateChange change) {
        LOGGER.info("Structure completed for blueprint {}", candidate.blueprint().id());
        this.dropStructure(candidate);

        Structure structure = new Structure(this, candidate.blueprint(), candidate.world(), candidate.origin());
        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureCompletedEvent(structure, who, change.previousState()));
            return null;
        });
    }
    private void handleUncompleted(Structure candidate, Entity who, StateChange change) {
        LOGGER.info("Structure destroyed for blueprint {}", candidate.blueprint().id());

        Structure structure = new Structure(this, candidate.blueprint(), candidate.world(), candidate.origin());
        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureUncompletedEvent(structure, who, change.newState()));
            return null;
        });
    }
    private StateChange updateStructure(Structure structure, Vec3i where) {
        if (!structure.contains(where)) {
            LOGGER.trace("Position {} is not within structure bounds", where);
            return null;
        }

        ReentrantLock lock = this.candidateLocks_.get(structure);
        if (lock == null) { 
            LOGGER.warn("No lock found for structure at {}", structure.origin());
            return null;
        }

        Vec3i offset = where.subtract(structure.origin());

        lock.lock();
        Structure.State prev = structure.getState(), curr = null;
        try {
            LOGGER.debug("Updating structure of {} at offset {}", structure.blueprint().name(), offset);
            structure.update(offset);
            curr = structure.getState();
        } finally {
            lock.unlock();
        }

        return new StateChange(prev, curr);
    }


    //HELPERS
    protected Structure newTracker(Blueprint blueprint, World world, Vec3i origin) {
        return new Structure(this, blueprint, world, origin);
    }
    private static Vec3i offsetInChunk(Vec3i src) {
        return new Vec3i(
                Math.floorMod(src.x(), Constants.CHUNK_SIZE),
                src.y(),
                Math.floorMod(src.z(), Constants.CHUNK_SIZE)
        );
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
        Collection<Structure> candidates = this.getStructuresInChunk(chunkKey);
        if (candidates.isEmpty()) {
            this.chunkPersister_.drop(chunk, this.candidatesNSK_);
            return;
        }

        List<StructureChunkData> toSerialize = new ArrayList<>(candidates.size());
        for (Structure candidate : candidates) {
            toSerialize.add(new StructureChunkData(offsetInChunk(candidate.origin()), candidate.blueprint().id()));
        }
        this.chunkPersister_.persist(chunk, this.candidatesNSK_, StructureCandidatePersistentDataType.instance(), toSerialize);
    }


    //SUBTYPES
    private record StateChange(Structure.State previousState, Structure.State newState) {

        boolean somethingChanged() {
            return this.previousState != this.newState;
        }
        boolean noChanges() {
            return this.previousState == this.newState;
        }
        boolean wasCompleted() {
            if (noChanges()) { return false; }
            return     this.previousState != Structure.State.COMPLETE
                    && this.newState == Structure.State.COMPLETE;
        }
        boolean wasUncompleted() {
            if (noChanges()) { return false; }
            return this.previousState == Structure.State.COMPLETE;
        }
    }
}
