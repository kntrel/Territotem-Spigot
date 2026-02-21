package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.territotem.blueprint.Blueprint;
import com.kntrel.mc.territotem.blueprint.BlueprintElement;
import com.kntrel.mc.territotem.blueprint.BlueprintRegistry;
import com.kntrel.mc.territotem.blueprint.BlueprintTracker;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;


class TotemCandidateTracker {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemCandidateTracker.class);


    //FIELDS
    private final BlueprintRegistry registry_;
    private final ChunkCache<BlueprintTracker> candidatesByChunk_;
    private final Map<BlueprintTracker, ReentrantLock> candidateLocks_;
    private final SetMap<Material, Blueprint> blueprintsByCore_;
    private final Set<Triplet<Vec3i, UUID, Long>> tracked_;
    private final Executor executor_;
    private final NamespacedKey candidatesNSK_;
    private final Consumer<CandidateUpdateContext> onCandidateCompleted_;



    //CONSTRUCTOR
    public TotemCandidateTracker(BlueprintRegistry registry, NamespacedKey candidatesNSK, Consumer<CandidateUpdateContext> onCandidateCompleted) {
        this.registry_ = registry;
        this.candidatesByChunk_ = new ChunkCache<>(c -> ChunkKey.ofBlock(c.origin(), c.world().getUID()));
        this.candidateLocks_ = new ConcurrentHashMap<>();
        this.blueprintsByCore_ = new SetMap<>();
        this.tracked_ = ConcurrentHashMap.newKeySet();
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.candidatesNSK_ = candidatesNSK;
        this.onCandidateCompleted_ = onCandidateCompleted;

        LOGGER.info("Tracking totem candidates");
    }


    //SERVICES
    public void registerBlueprint(Blueprint blueprint) {
        this.blueprintsByCore_.putInto(blueprint.core().element().type(), blueprint);
        LOGGER.trace("Blueprint {} registered successfully", blueprint.id());
    }
    public void handleChunkLoad(Chunk chunk) {
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
            Blueprint blueprint = this.registry_.get(candidate.blueprintId()).orElse(null);
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
    public void handleChunkUnload(Chunk chunk) {
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
    public void handleBlockUpdate(Entity who, Vec3i where, World world, Material material, BlockState block, Consumer<UpdateContext> totemUpdateHandler) {
        // Execute candidate update task asynchronously
        this.executor_.execute(() -> handleCandidateUpdateTask(who, where, world, material, block, totemUpdateHandler));

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
    private void handleCandidateUpdateTask(Entity who, Vec3i where, World world, Material material, BlockState block, Consumer<UpdateContext> totemUpdateHandler) {
        ChunkKey chunk = ChunkKey.ofBlock(where, world.getUID());
        Collection<BlueprintTracker> candidates = this.getCandidatesInChunk(chunk);

        for (BlueprintTracker candidate : candidates) {
            this.executor_.execute(() -> updateCandidateTask(who, candidate, where, material, block, totemUpdateHandler));
        }
    }
    private void updateCandidateTask(Entity who, BlueprintTracker candidate, Vec3i where, Material material, BlockState block, Consumer<UpdateContext> totemUpdateHandler) {
        LOGGER.trace("Updating candidate {} at position {}", candidate.blueprint().id(), where);
        BlueprintElement element = new BlueprintElement.Block(material);
        boolean completed = this.updateCandidate(candidate, where, element);

        if (completed) {
            CandidateUpdateContext context = new CandidateUpdateContext(candidate, who, where, element);
            handleCompleteCandidate(context);
        }
    }
    private void handleCompleteCandidate(CandidateUpdateContext context) {
        LOGGER.info("Totem candidate completed for blueprint {} by player {}", context.candidate.blueprint().id(), context.who.getName());
        this.onCandidateCompleted_.accept(context);
        this.dropCandidate(context.candidate());
    }
    public void trackCandidate(Entity who, Blueprint blueprint, World world, Vec3i origin) {
        Triplet<Vec3i, UUID, Long> trackKey = Triplet.of(origin, world.getUID(), blueprint.id());
        if (this.tracked_.contains(trackKey)) {
            LOGGER.debug("Already tracking a potential totem of blueprint {} at {}", blueprint.id(), origin);
            return;
        }

        BlueprintTracker candidate = this.registry_.trackerAt(blueprint, world, origin);
        if (candidate.isComplete()) {
            CandidateUpdateContext context = new CandidateUpdateContext(candidate, who, origin, null);
            this.handleCompleteCandidate(context);
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
    public boolean updateCandidate(BlueprintTracker candidate, Vec3i where, BlueprintElement element) {
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


    //SUBTYPES
    public record CandidateUpdateContext(BlueprintTracker candidate, Entity who, Vec3i where, BlueprintElement element) {}
    public record UpdateContext(Vec3i where, Material material, BlockState block) {}


    //HELPERS
    private static Vec3i offsetInChunk(Vec3i src) {
        return new Vec3i(
                src.x() % Constants.CHUNK_SIZE,
                src.y(),
                src.z() % Constants.CHUNK_SIZE
        );
    }
}