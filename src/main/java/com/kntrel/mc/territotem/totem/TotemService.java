package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.territotem.structure.*;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import java.util.concurrent.*;

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
    private final StructureService structureService_;
    private final RegionContext regionContext_;
    private final Executor executor_;
    private final TotemServiceListener listener_;



    //CONSTRUCTORS
    public TotemService(RegionContext regionContext, StructureService blueprintRegistry) {
        this.regionContext_ = regionContext;
        this.plugin_ = this.regionContext_.getPlugin();
        this.structureService_ = blueprintRegistry;
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();
        this.listener_ = new TotemServiceListener(this);
        this.plugin_.getServer().getPluginManager().registerEvents(this.listener_, this.plugin_);
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

        return new Totem(blueprint, origin, region);
    }


    //IGNITERS


    //TASKS


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



    //SUBTYPES
    private record TotemTracker(Totem totem, BlueprintTracker tracker) {}
}
