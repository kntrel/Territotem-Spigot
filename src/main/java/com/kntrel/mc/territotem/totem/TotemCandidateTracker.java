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
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;


class TotemCandidateTracker {


    //FIELDS
    private final BlueprintRegistry registry_;
    private final ChunkCache<BlueprintTracker> candidatesByChunk_;
    private final Map<BlueprintTracker, ReentrantLock> candidateLocks_;
    private final SetMap<Material, Blueprint> blueprintsByCore_;
    private final Executor executor_;
    private final NamespacedKey candidatesNSK_;
    private final Consumer<CandidateUpdateContext> onCandidateCompleted_;



    //CONSTRUCTOR
    public TotemCandidateTracker(BlueprintRegistry registry, NamespacedKey candidatesNSK, Consumer<CandidateUpdateContext> onCandidateCompleted) {
        this.registry_ = registry;
        this.candidatesByChunk_ = new ChunkCache<>(c -> ChunkKey.ofBlock(c.origin(), c.world().getUID()));
        this.candidateLocks_ = new ConcurrentHashMap<>();
        this.blueprintsByCore_ = new SetMap<>();
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.candidatesNSK_ = candidatesNSK;
        this.onCandidateCompleted_ = onCandidateCompleted;
    }


    //SERVICES
    public void registerBlueprint(Blueprint blueprint) {
        this.blueprintsByCore_.putInto(blueprint.core().element().type(), blueprint);
    }
    public void handleChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (!pdc.has(this.candidatesNSK_)) { return; }

        List<ChunkTotemCandidate> candidates = pdc.get(this.candidatesNSK_, TotemCandidatePersistentDataType.instance());
        if (candidates == null || candidates.isEmpty()) { return; }

        for (ChunkTotemCandidate candidate : candidates) {
            Blueprint blueprint = this.registry_.get(candidate.blueprintId()).orElse(null);
            if (blueprint == null) { continue; }

            Vec3i origin = new Vec3i(chunk.getX(), 0, chunk.getZ())
                    .shiftLeft(Constants.CHUNK_SHIFT)
                    .add(candidate.offset());

            this.trackCandidate(blueprint, chunk.getWorld(), origin);
        }
    }
    public void handleChunkUnload(Chunk chunk) {
        ChunkKey chunkKey = ChunkKey.ofBlock(new Vec3i(chunk.getX() * 16, 0, chunk.getZ() * 16), chunk.getWorld().getUID());
        Collection<BlueprintTracker> candidatesInChunk = this.candidatesByChunk_.evict(chunkKey);

        if (candidatesInChunk.isEmpty()) { return; }

        List<ChunkTotemCandidate> toSerialize = new ArrayList<>();
        for (BlueprintTracker tracker : candidatesInChunk) {
            toSerialize.add(new ChunkTotemCandidate(offsetInChunk(tracker.origin()), tracker.blueprint().id()));
            this.candidateLocks_.remove(tracker);
        }

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        pdc.set(this.candidatesNSK_, TotemCandidatePersistentDataType.instance(), toSerialize);
    }
    public void handleBlockUpdate(Entity who, Vec3i where, World world, Material material, BlockState block, Consumer<UpdateContext> totemUpdateHandler) {
        // Execute candidate update task asynchronously
        this.executor_.execute(() -> handleCandidateUpdateTask(who, where, world, material, block, totemUpdateHandler));

        // Track new candidates if block matches a blueprint core
        Set<Blueprint> candidateBlueprints = this.blueprintsByCore_.get(material);
        if (candidateBlueprints.isEmpty()) {
            return;
        }

        for (Blueprint b : candidateBlueprints) {
            this.trackCandidate(b, world, where);
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
        BlueprintElement element = new BlueprintElement.Block(material);
        CandidateUpdateContext context = new CandidateUpdateContext(candidate, who, where, element);
        boolean completed = this.updateCandidate(candidate, where, element);

        if (completed) {
            this.onCandidateCompleted_.accept(context);
            this.dropCandidate(candidate);
        }
    }
    public void trackCandidate(Blueprint blueprint, World world, Vec3i origin) {
        BlueprintTracker candidate = this.registry_.trackerAt(blueprint, world, origin);
        this.candidatesByChunk_.put(candidate);
        this.candidateLocks_.put(candidate, new ReentrantLock());
    }
    public void dropCandidate(BlueprintTracker candidate) {
        this.candidatesByChunk_.evict(candidate);
        this.candidateLocks_.remove(candidate);
    }
    public Collection<BlueprintTracker> getCandidatesInChunk(ChunkKey chunk) {
        return List.copyOf(this.candidatesByChunk_.get(chunk));
    }
    public boolean updateCandidate(BlueprintTracker candidate, Vec3i where, BlueprintElement element) {
        if (!candidate.contains(where)) { return false; }

        ReentrantLock lock = this.candidateLocks_.get(candidate);
        if (lock == null) { return false; }

        Vec3i offset = where.subtract(candidate.origin());
        boolean completed = false;

        lock.lock();
        try {
            if (candidate.isLocked()) { return false; }

            candidate.update(offset, element);
            if (candidate.isComplete()) {
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