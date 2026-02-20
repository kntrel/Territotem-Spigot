package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.mc.territotem.blueprint.*;
import com.kntrel.mc.territotem.util.ChunkCache;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    //CONSTANTS
    private static final String TOTEM_LOCATION_KEY = "totem_location",
                                TOTEM_BLUEPRINT_KEY = "totem_blueprint";


    //UTIL
    public static boolean isTotemOwned(Region region) {
        RegionDataContainer dataContainer = region.getDataContainer();
        return dataContainer.has(TOTEM_LOCATION_KEY) && dataContainer.has(TOTEM_BLUEPRINT_KEY);
    }


    //FIELDS
    private final Plugin plugin_;
    private final BlueprintRegistry registry_;
    private final RegionContext regionContext_;
    private final Executor executor_;
    private final TotemServiceListener listener_;


    //SUPPORT DATA STRUCTURES
    private final TotemCandidateTracker candidateTracker_;
    private final ChunkCache<TotemTracker> totemsByChunk_;
    private final Map<Totem, ReentrantLock> totemLocks_;


    //CONSTRUCTORS
    public TotemService(RegionContext regionContext, BlueprintRegistry blueprintRegistry) {
        this.regionContext_ = regionContext;
        this.plugin_ = this.regionContext_.getPlugin();
        this.registry_ = blueprintRegistry;
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.listener_ = new TotemServiceListener(this);

        NamespacedKey candidatesNSK = new NamespacedKey(this.plugin_, "totem_candidates");
        this.candidateTracker_ = new TotemCandidateTracker(
            blueprintRegistry,
            candidatesNSK,
            this::onCandidateCompleted
        );
        this.totemsByChunk_ = new ChunkCache<>(t -> ChunkKey.ofBlock(t.totem().origin(), t.totem().world().getUID()));
        this.totemLocks_ = new ConcurrentHashMap<>();

        this.plugin_.getServer().getPluginManager().registerEvents(this.listener_, this.plugin_);
        this.registry_.all().forEach(this::register);
        this.registry_.onNewRegistration(this::register);
    }


    //SERVICES
    public Totem newTotem(Entity who, String name, Blueprint blueprint, Vec3i origin, World world) {

        BoundingBox bb = blueprint.initialRegionBounds().shift(origin.x(), origin.y(), origin.z());
        Region region = this.regionContext_.create(who, bb, world, name, blueprint.hierarchy());

        RegionDataContainer dc = region.getDataContainer();

        dc.add(new RegionData(TOTEM_LOCATION_KEY, origin));
        dc.add(new RegionData(TOTEM_BLUEPRINT_KEY, blueprint.id()));
        if (who instanceof Player owner) {
            region.addPermission(owner, region.getHierarchy().getLowestLever());
        }
        region.save();

        Totem totem = new Totem(blueprint, origin, region);
        this.trackTotem(totem);

        return totem;
    }


    //IGNITERS
    void handleBlockUpdate(Entity who, Vec3i where, World world, Material material, BlockState block) {
        Area area = new Area(
                where.x(), where.y(), where.z(),
                where.x() + 1, where.y() + 1, where.z() + 1,
                world
        );
        List<Region> presentRegions = this.regionContext_.getHotRegionRepository().where()
                .in(area)
                .hasDataKey(TOTEM_LOCATION_KEY)
                .hasDataKey(TOTEM_BLUEPRINT_KEY)
                .get();
        if (!presentRegions.isEmpty()) { return; }

        this.candidateTracker_.handleBlockUpdate(who, where, world, material, block, ctx -> handleTotemUpdate(who, ctx));
    }
    void handleChunkLoad(Chunk chunk) {
        this.candidateTracker_.handleChunkLoad(chunk);
    }
    void handleChunkUnload(Chunk chunk) {
        this.candidateTracker_.handleChunkUnload(chunk);
    }


    //TASKS
    private void handleTotemUpdate(Entity who, TotemCandidateTracker.UpdateContext context) {
        ChunkKey chunk = ChunkKey.ofBlock(context.where(), who.getWorld().getUID());
        Collection<TotemTracker> totems = List.copyOf(this.totemsByChunk_.get(chunk));

        for (TotemTracker totem : totems) {
            this.executor_.execute(() -> updateTotemTask(totem, who, context.where(), context.material(), context.block()));
        }
    }
    private void updateTotemTask(TotemTracker totem, Entity who, Vec3i where, Material material, BlockState block) {
        BlueprintTracker tracker = totem.tracker();
        if (!tracker.contains(where)) { return; }

        ReentrantLock lock = this.totemLocks_.get(totem.totem());
        if (lock == null) { return; }

        BlueprintElement element = new BlueprintElement.Block(material);
        Vec3i offset = where.subtract(tracker.origin());
        BlueprintCoreTile core = totem.totem().blueprint().core();

        lock.lock();
        try {
            if (tracker.isLocked()) { return; }

            if (core.offset().equals(offset) && !core.element().matches(element)) {
                tracker.lock();
                totem.totem().destroy();
                this.dropTotem(totem);
                return;
            }

            tracker.update(offset, element);
            totem.totem().setEnabled(tracker.isComplete());
        } finally {
            lock.unlock();
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
    private void onCandidateCompleted(TotemCandidateTracker.CandidateUpdateContext context) {
        if (context.who() instanceof Player player) {
            player.sendMessage("Completed blueprint " + context.candidate().blueprint().name());
        }
    }
    private void trackTotem(Totem totem) {
        BlueprintTracker blueprintTracker = this.registry_.trackerAt(totem.blueprint(), totem.world(), totem.origin());
        TotemTracker totemTracker = new TotemTracker(totem, blueprintTracker);
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            this.totemsByChunk_.put(totemTracker);
            this.totemLocks_.put(totem, lock);
            totem.setEnabled(blueprintTracker.isComplete());
        } finally {
            lock.unlock();
        }
    }
    private void dropTotem(TotemTracker totem) {
        this.totemLocks_.remove(totem.totem());
    }
    private void register(Blueprint blueprint) {
        this.candidateTracker_.registerBlueprint(blueprint);
    }


    //SUBTYPES
    private record TotemTracker(Totem totem, BlueprintTracker tracker) {}
}
