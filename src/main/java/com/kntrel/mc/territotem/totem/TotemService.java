package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.blueprint.*;
import com.kntrel.mc.territotem.util.ChunkCache;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.SetMap;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class TotemService {

    //FIELDS
    private final Plugin plugin_;
    private final BlueprintRegistry registry_;
    private final RegionContext regionContext_;
    private final Executor executor_;
    private final TotemServiceListener listener_;


    //SUPPORT DATA STRUCTURES
    private final SetMap<Material, Blueprint> blueprintsByCore_;
    private final ChunkCache<BlueprintTracker> candidatesByChunk_;
    private final ChunkCache<TotemTracker> totemsByChunk_;
    private final Map<BlueprintTracker, ReentrantLock> candidateLocks_;
    private final Map<Totem, ReentrantLock> totemLocks_;


    //CONSTRUCTORS
    public TotemService(RegionContext regionContext, BlueprintRegistry blueprintRegistry) {
        this.regionContext_ = regionContext;
        this.plugin_ = this.regionContext_.getPlugin();
        this.registry_ = blueprintRegistry;
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.listener_ = new TotemServiceListener(this);

        this.blueprintsByCore_ = new SetMap<>();
        this.candidatesByChunk_ = new ChunkCache<>(c -> ChunkKey.ofBlock(c.origin(), c.world().getUID()));
        this.totemsByChunk_ = new ChunkCache<>(t -> ChunkKey.ofBlock(t.totem().origin(), t.totem().world().getUID()));
        this.candidateLocks_ = new ConcurrentHashMap<>();
        this.totemLocks_ = new ConcurrentHashMap<>();

        this.plugin_.getServer().getPluginManager().registerEvents(this.listener_, this.plugin_);
        this.registry_.all().forEach(this::register);
        this.registry_.onNewRegistration(this::register);
    }


    //SERVICES
    public Totem newTotem(Entity who, String name, Blueprint blueprint, Vec3i origin, World world) {

        BoundingBox bb = blueprint.initialRegionBounds().shift(origin.x(), origin.y(), origin.z());
        Region region = this.regionContext_.create(who, bb, world, name, blueprint.hierarchy());

        if (who instanceof Player owner) {
            region.addPermission(owner, region.getHierarchy().getLowestLever());
            region.save();
        }

        Totem totem = new Totem(blueprint, origin, region);
        this.trackTotem(totem);

        return totem;
    }


    //IGNITERS
    void handleBlockUpdate(Entity who, Vec3i where, UUID worldUUID, Material material, BlockState block) {
        this.executor_.execute(() -> handleBlockUpdateTask(who, where, worldUUID, material, block));
    }


    //TASKS
    private void handleBlockUpdateTask(Entity who, Vec3i where, UUID worldUUID, Material material, BlockState block) {
        ChunkKey chunk = ChunkKey.ofBlock(where, worldUUID);

        Collection<BlueprintTracker> candidates;
        synchronized (this.candidatesByChunk_) {
            candidates = List.copyOf(this.candidatesByChunk_.get(chunk));
        }

        for (BlueprintTracker candidate : candidates) {
            this.executor_.execute(() -> handleCandidateTask(candidate, who, where, material, block));
        }
    }
    private void handleCandidateTask(BlueprintTracker candidate, Entity who, Vec3i where, Material material, BlockState block) {
        if (!candidate.contains(where)) { return; }

        BlueprintElement element = new BlueprintElement.Block(material);
        Vec3i offset = where.subtract(candidate.origin());
        ReentrantLock lock = this.candidateLocks_.computeIfAbsent(candidate, c -> new ReentrantLock());
        boolean completed = false;
        lock.lock();
        try {
            if (candidate.isLocked()) { return; }

            candidate.update(offset, element);
            if (candidate.isComplete()) {
                candidate.lock();
                completed = true;
            }
        } finally {
            lock.unlock();
        }

        if (completed) {
            this.promoteCandidate(who, candidate);
            this.dropCandidate(candidate);
        }
    }


    //PACKAGE PRIVATE SERVICES
    <T> T runInMainThread(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        this.plugin_.getServer().getScheduler().runTask(this.plugin_, future);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    <T> CompletableFuture<T> runInMainThreadAsync(Callable<T> task) {
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
    void promoteCandidate(Entity who, BlueprintTracker candidate) {
        this.runInMainThread(() ->
                this.newTotem(who, who.getName() + "'s Region", candidate.blueprint(), candidate.origin(), candidate.world())
        );
    }


    //HELPERS
    private void trackCandidate(Blueprint blueprint, World world, Vec3i offset) {
        BlueprintTracker candidate = this.registry_.trackerAt(blueprint, world, offset);
        synchronized (this.candidatesByChunk_) {
            this.candidatesByChunk_.put(candidate);
        }
        this.candidateLocks_.put(candidate, new ReentrantLock());
    }
    private void dropCandidate(BlueprintTracker candidate) {
        this.candidateLocks_.remove(candidate);
    }
    private void trackTotem(Totem totem) {
        BlueprintTracker blueprintTracker = this.registry_.trackerAt(totem.blueprint(), totem.world(), totem.origin());
        TotemTracker totemTracker = new TotemTracker(totem, blueprintTracker);
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            synchronized (this.totemsByChunk_) {
                this.totemsByChunk_.put(totemTracker);
            }
            this.totemLocks_.put(totem, lock);
            totem.setEnabled(blueprintTracker.isComplete());
        } finally {
            lock.unlock();
        }
    }
    private void register(Blueprint blueprint) {
        this.blueprintsByCore_.putInto(blueprint.core().element().type(), blueprint);
    }


    //SUBTYPES
    private record TotemTracker(Totem totem, BlueprintTracker tracker) {}
}
