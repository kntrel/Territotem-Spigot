package com.kntrel.mc.territotem.blueprint;

import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

public class BlueprintRegistry implements Listener {

    //FIELDS
    private final Plugin plugin_;
    private final Map<Long, Blueprint> blueprints_;
    private final List<Consumer<Blueprint>> registerCallBacks_;
    private final BlueprintBitsetGenerator bitsetGenerator_;


    //CONSTRUCTORS
    public BlueprintRegistry(Plugin plugin) {
        this.plugin_ = plugin;
        this.blueprints_ = new HashMap<>();
        this.registerCallBacks_ = new ArrayList<>();
        this.bitsetGenerator_ = new BlueprintBitsetGenerator();
    }


    //API
    public void register(Blueprint blueprint) {
        long id = blueprint.id();
        if (this.blueprints_.containsKey(id)) {
            throw new IllegalArgumentException("A blueprint with id " + id + " is already registered.");
        }
        this.blueprints_.put(id, blueprint);
        this.registerCallBacks_.forEach(c -> c.accept(blueprint));
    }
    public Optional<Blueprint> get(long id) {
        return Optional.ofNullable(this.blueprints_.get(id));
    }
    public List<Blueprint> all() {
        return List.copyOf(this.blueprints_.values());
    }
    public void onNewRegistration(Consumer<Blueprint> action) {
        this.registerCallBacks_.add(action);
    }
    public BlueprintTracker trackerAt(Blueprint blueprint, World world, Vec3i origin) {
        return new BlueprintTracker(this, blueprint, world, origin);
    }


    //PACKAGE-PRIVATE SERVICES
    BlueprintBitsetGenerator getBitsetGenerator() {
        return this.bitsetGenerator_;
    }
    <T> T runInMainThread(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        this.plugin_.getServer().getScheduler().runTask(this.plugin_, future);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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
}
