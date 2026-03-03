package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.territotem.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.event.StructureDestroyedEvent;
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
import org.bukkit.block.BlockState;
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
    private static final String CANDIDATES_KEY = "structure_candidates";


    //FIELDS
    private final Plugin plugin_;
    private final ChunkPersister chunkPersister_;
    private final Map<Long, Blueprint> blueprints_;
    private final ChunkCache<BlueprintTracker> candidatesByChunk_;
    private final Map<BlueprintTracker, ReentrantLock> candidateLocks_;
    private final Map<Triplet<Vec3i, UUID, Long>, BlueprintTracker> trackersMap_;
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
        this.candidatesNSK_ = new NamespacedKey(this.plugin_, CANDIDATES_KEY);
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
    public BlueprintTracker track(Blueprint blueprint, Vec3i origin, World world, Entity causer) {

        LOGGER.debug("New blueprint tracker started at {}", origin);
        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(origin, world.getUID(), blueprint.id());
        BlueprintTracker candidate = this.trackersMap_.get(trackKey);
        if (candidate != null) {
            LOGGER.debug("Already tracking a potential totem of blueprint {} at {}", blueprint.id(), origin);
            return candidate;
        }

        candidate = this.runInMainThread(() -> this.newTracker(blueprint, world, origin));
        if (candidate.isEmpty()) {
            LOGGER.debug("Candidate at {} had not a single match. Dropped", origin);
            return candidate;
        }
        if (candidate.isComplete()) {
            LOGGER.debug("Candidate at {} was complete right away. No tracking needed", origin);
            this.handleCompleteCandidate(candidate, causer);
            return candidate;
        }

        this.trackersMap_.put(trackKey, candidate);
        this.candidatesByChunk_.put(candidate);
        this.candidateLocks_.put(candidate, new ReentrantLock());
        this.persistCandidatesInChunk(ChunkKey.ofBlock(candidate.origin(), candidate.world().getUID()));

        return candidate;
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
        this.executor_.execute(() -> handleCandidatesUpdateTask(who, where, world));
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
        List<ChunkTotemCandidate> candidates = this.chunkPersister_.retrieve(chunk, this.candidatesNSK_, StructureCandidatePersistentDataType.instance());
        if (candidates == null || candidates.isEmpty()) {
            LOGGER.trace("No totem candidates found in chunk [{}, {}]", chunk.getX(), chunk.getZ());
            return;
        }

        LOGGER.info("Found {} totem candidates in chunk [{}, {}]", candidates.size(), chunk.getX(), chunk.getZ());
        for (ChunkTotemCandidate candidate : candidates) {
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
    private void handleCandidatesUpdateTask(Entity who, Vec3i where, World world) {
        ChunkKey chunk = ChunkKey.ofBlock(where, world.getUID());
        Collection<BlueprintTracker> candidates = this.getCandidatesInChunk(chunk);

        for (BlueprintTracker candidate : candidates) {
            this.executor_.execute(() -> updateCandidateTask(who, candidate, where));
        }
    }
    private void updateCandidateTask(Entity who, BlueprintTracker candidate, Vec3i where) {
        LOGGER.trace("Updating candidate {} at position {}", candidate.blueprint().id(), where);
        boolean completed = this.updateCandidate(candidate, where);
        if (completed) {
            handleCompleteCandidate(candidate, who);
        }
    }
    private void handleCompleteCandidate(BlueprintTracker candidate, Entity who) {
        LOGGER.info("Totem candidate completed for blueprint {}", candidate.blueprint().id());
        this.dropCandidate(candidate);

        Structure structure = new Structure(this, candidate.blueprint(), candidate.world(), candidate.origin());
        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureCompletedEvent(structure, who));
            return null;
        });
    }
    private void handleDestroyedCandidate(BlueprintTracker candidate, Entity who) {
        LOGGER.info("Totem candidate destroyed for blueprint {}", candidate.blueprint().id());
        this.dropCandidate(candidate);

        Structure structure = new Structure(this, candidate.blueprint(), candidate.world(), candidate.origin());
        this.runInMainThreadAsync(() -> {
            this.getServer().getPluginManager().callEvent(new StructureDestroyedEvent(structure, who));
            return null;
        });
    }
    private void dropCandidate(BlueprintTracker candidate) {
        LOGGER.debug("Dropping candidate for blueprint {} at origin {}", candidate.blueprint().id(), candidate.origin());
        this.candidatesByChunk_.evict(candidate);
        this.candidateLocks_.remove(candidate);

        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(candidate.origin(), candidate.world().getUID(), candidate.blueprint().id());
        this.trackersMap_.remove(trackKey);
        this.persistCandidatesInChunk(ChunkKey.ofBlock(candidate.origin(), candidate.world().getUID()));
    }
    public Collection<BlueprintTracker> getCandidatesInChunk(ChunkKey chunk) {
        Collection<BlueprintTracker> candidates = List.copyOf(this.candidatesByChunk_.get(chunk));
        LOGGER.trace("Retrieved {} candidates from chunk {}", candidates.size(), chunk);
        return candidates;
    }
    private boolean updateCandidate(BlueprintTracker candidate, Vec3i where) {
        if (!candidate.contains(where)) { 
            LOGGER.trace("Position {} is not within candidate bounds", where);
            return false; 
        }

        ReentrantLock lock = this.candidateLocks_.get(candidate);
        if (lock == null) { 
            LOGGER.warn("No lock found for candidate at {}", candidate.origin());
            return false; 
        }

        Vec3i offset = where.subtract(candidate.origin());
        boolean completed = false;

        lock.lock();
        try {
            if (candidate.isLocked()) { 
                LOGGER.trace("Candidate is already locked");
                return false; 
            }

            LOGGER.debug("Updating candidate of {} at offset {}", candidate.blueprint().name(), offset);
            candidate.update(offset);
            if (candidate.isComplete()) {
                LOGGER.debug("Candidate is now complete - locking");
                candidate.lock();
                completed = true;
            }
        } finally {
            lock.unlock();
        }

        return completed;
    }


    //HELPERS
    protected BlueprintTracker newTracker(Blueprint blueprint, World world, Vec3i origin) {
        return new BlueprintTracker(this, blueprint, world, origin);
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

    private void persistCandidatesInChunk(ChunkKey chunkKey) {
        World world = this.plugin_.getServer().getWorld(chunkKey.world());
        if (world == null || !world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
            return;
        }

        Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());
        Collection<BlueprintTracker> candidates = this.getCandidatesInChunk(chunkKey);
        if (candidates.isEmpty()) {
            this.chunkPersister_.drop(chunk, this.candidatesNSK_);
            return;
        }

        List<ChunkTotemCandidate> toSerialize = new ArrayList<>(candidates.size());
        for (BlueprintTracker candidate : candidates) {
            toSerialize.add(new ChunkTotemCandidate(offsetInChunk(candidate.origin()), candidate.blueprint().id()));
        }
        this.chunkPersister_.persist(chunk, this.candidatesNSK_, StructureCandidatePersistentDataType.instance(), toSerialize);
    }
}
