package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.territotem.util.ChunkCache;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.SetMap;
import com.kntrel.util.Vec3i;
import com.kntrel.util.tuple.Triplet;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
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
    private final Map<Long, Blueprint> blueprints_;
    private final ChunkCache<BlueprintTracker> candidatesByChunk_;
    private final Map<BlueprintTracker, ReentrantLock> candidateLocks_;
    private final SetMap<Material, Blueprint> blueprintsByCore_;
    private final Set<Triplet<Vec3i, UUID, Long>> tracked_;
    private final Executor executor_;
    private final NamespacedKey candidatesNSK_;
    private final BlueprintBitsetGenerator bitsetGenerator_;



    //CONSTRUCTOR
    public StructureService(Plugin plugin) {
        this.plugin_ = plugin;
        this.blueprints_ = new ConcurrentHashMap<>();
        this.candidatesByChunk_ = new ChunkCache<>(c -> ChunkKey.ofBlock(c.origin(), c.world().getUID()));
        this.candidateLocks_ = new ConcurrentHashMap<>();
        this.blueprintsByCore_ = new SetMap<>();
        this.tracked_ = ConcurrentHashMap.newKeySet();
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.candidatesNSK_ = new NamespacedKey(this.plugin_, CANDIDATES_KEY);
        this.bitsetGenerator_ = new BlueprintBitsetGenerator();

        LOGGER.info("Tracking totem candidates");
    }


    //SERVICES
    public void registerBlueprint(Blueprint blueprint) {
        long id = blueprint.id();
        if (blueprints_.containsKey(id)) {
            LOGGER.error("Tried to register a blueprint with ID " + id + " one with the same ID was already registered");
            throw new IllegalArgumentException("A blueprint with ID " + id + " is already registered");
        }

        this.blueprints_.put(id, blueprint);
        this.blueprintsByCore_.putInto(blueprint.core().element().type(), blueprint);
    }
    public BlueprintTracker trackerAt(Blueprint blueprint, World world, Vec3i origin) {
        return new BlueprintTracker(this, blueprint, world, origin);
    }


    //PACKAGE-PRIVATE SERVICES
    BlueprintBitsetGenerator getBitsetGenerator() {
        return this.bitsetGenerator_;
    }
    protected <T> CompletableFuture<T> runInMainThreadAsync(Callable<T> task) {
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


    //LISTENERS
    void handleChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (!pdc.has(this.candidatesNSK_)) { 
            LOGGER.trace("No totem candidates found in chunk [{}, {}]", chunk.getX(), chunk.getZ());
            return; 
        }

        List<ChunkTotemCandidate> candidates = pdc.get(this.candidatesNSK_, TotemCandidatePersistentDataType.instance());
        if (candidates == null || candidates.isEmpty()) { 
            LOGGER.warn("Totem candidates list is null or empty for chunk [{}, {}]", chunk.getX(), chunk.getZ());
            pdc.remove(this.candidatesNSK_);
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

            Vec3i coreLoc = origin.add(blueprint.core().offset());
            if (!blueprint.core().element().matchesAt(coreLoc, chunk.getWorld())) {
                LOGGER.warn("Expected a blueprint core at {} but bot found.", coreLoc);
                continue;
            }

            LOGGER.debug("Restoring totem candidate for blueprint {} at origin {}", blueprint.id(), origin);
            this.executor_.execute(() -> this.trackCandidate(null, blueprint, chunk.getWorld(), origin));
        }
    }
    void handleChunkUnload(Chunk chunk) {
        LOGGER.trace("Chunk unloading at [{}, {}] in world {}. Checking for totem candidates to serialize", chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
        ChunkKey chunkKey = ChunkKey.ofBlock(new Vec3i(chunk.getX() * 16, 0, chunk.getZ() * 16), chunk.getWorld().getUID());
        Collection<BlueprintTracker> candidatesInChunk = this.candidatesByChunk_.evict(chunkKey);

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (candidatesInChunk.isEmpty()) {
            LOGGER.trace("No candidates to persist in chunk [{}, {}]", chunk.getX(), chunk.getZ());
            pdc.remove(this.candidatesNSK_);
            return;
        }

        LOGGER.debug("Persisting {} totem candidates in chunk [{}, {}]", candidatesInChunk.size(), chunk.getX(), chunk.getZ());
        List<ChunkTotemCandidate> toSerialize = new ArrayList<>();
        for (BlueprintTracker candidate : candidatesInChunk) {
            LOGGER.debug("Persisting candidate for blueprint {} at origin {}", candidate.blueprint().id(), candidate.origin());
            toSerialize.add(new ChunkTotemCandidate(offsetInChunk(candidate.origin()), candidate.blueprint().id()));

            Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(candidate.origin(), candidate.world().getUID(), candidate.blueprint().id());
            this.tracked_.remove(trackKey);
            this.candidateLocks_.remove(candidate);
        }

        pdc.set(this.candidatesNSK_, TotemCandidatePersistentDataType.instance(), toSerialize);
    }
    void handleBlockUpdate(Entity who, Vec3i where, World world, Material material, BlockState block) {
        // Execute candidate update task asynchronously
        this.executor_.execute(() -> handleCandidatesUpdateTask(who, where, world, material, block));

        // Track new candidates if block matches a blueprint core
        Set<Blueprint> candidateBlueprints = this.blueprintsByCore_.get(material);
        if (candidateBlueprints == null || candidateBlueprints.isEmpty()) {
            LOGGER.trace("No blueprints found with core material: {}", material);
            return;
        }

        LOGGER.debug("Block matches core material - tracking {} candidate totem(s) at {}", candidateBlueprints.size(), where);
        for (Blueprint b : candidateBlueprints) {
            LOGGER.debug("Tracking new candidate for blueprint {} at origin {}", b.id(), where);
            Vec3i origin = where.subtract(b.core().offset());
            this.executor_.execute(() -> this.trackCandidate(who, b, world, origin));
        }
    }


    //TASKS
    private void handleCandidatesUpdateTask(Entity who, Vec3i where, World world, Material material, BlockState block) {
        ChunkKey chunk = ChunkKey.ofBlock(where, world.getUID());
        Collection<BlueprintTracker> candidates = this.getCandidatesInChunk(chunk);

        for (BlueprintTracker candidate : candidates) {
            this.executor_.execute(() -> updateCandidateTask(who, candidate, where, material, block));
        }
    }
    private void updateCandidateTask(Entity who, BlueprintTracker candidate, Vec3i where, Material material, BlockState block) {
        LOGGER.trace("Updating candidate {} at position {}", candidate.blueprint().id(), where);
        Piece element = new Piece.Block(material);
        boolean completed = this.updateCandidate(candidate, where, element);

        if (completed) {
            handleCompleteCandidate(candidate);
        }
    }
    private void handleCompleteCandidate(BlueprintTracker candidate) {
        LOGGER.info("Totem candidate completed for blueprint {} by player {}", context.candidate.blueprint().id(), context.who.getName());
        this.dropCandidate(candidate);
    }
    public void trackCandidate(Entity who, Blueprint blueprint, World world, Vec3i origin) {
        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(origin, world.getUID(), blueprint.id());
        if (this.tracked_.contains(trackKey)) {
            LOGGER.debug("Already tracking a potential totem of blueprint {} at {}", blueprint.id(), origin);
            return;
        }

        BlueprintTracker candidate = this.trackerAt(blueprint, world, origin);
        if (candidate.isComplete()) {
            this.handleCompleteCandidate(candidate);
            LOGGER.debug("Candidate at {} was complete right away. No tracking needed", origin);
            return;
        }

        this.tracked_.add(trackKey);
        this.candidatesByChunk_.put(candidate);
        this.candidateLocks_.put(candidate, new ReentrantLock());
    }
    public void dropCandidate(BlueprintTracker candidate) {
        LOGGER.debug("Dropping candidate for blueprint {} at origin {}", candidate.blueprint().id(), candidate.origin());
        this.candidatesByChunk_.evict(candidate);
        this.candidateLocks_.remove(candidate);

        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(candidate.origin(), candidate.world().getUID(), candidate.blueprint().id());
        this.tracked_.remove(trackKey);
    }
    public Collection<BlueprintTracker> getCandidatesInChunk(ChunkKey chunk) {
        Collection<BlueprintTracker> candidates = List.copyOf(this.candidatesByChunk_.get(chunk));
        LOGGER.trace("Retrieved {} candidates from chunk {}", candidates.size(), chunk);
        return candidates;
    }
    public boolean updateCandidate(BlueprintTracker candidate, Vec3i where, Piece element) {
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

            LOGGER.debug("Updating candidate with element {} at offset {}", element, offset);
            candidate.update(offset, element);
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
    private static Vec3i offsetInChunk(Vec3i src) {
        return new Vec3i(
                Math.floorMod(src.x(), Constants.CHUNK_SIZE),
                src.y(),
                Math.floorMod(src.z(), Constants.CHUNK_SIZE)
        );
    }
}