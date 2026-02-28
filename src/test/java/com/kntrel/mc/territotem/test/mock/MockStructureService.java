package com.kntrel.mc.territotem.test.mock;

import com.kntrel.mc.territotem.structure.BlueprintTracker;
import com.kntrel.mc.territotem.structure.StructureService;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.worldTile.MockWorldTile;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class MockStructureService extends StructureService {

    public MockStructureService(Plugin plugin) {
        super(plugin);
    }


    @Override
    protected <T> CompletableFuture<T> runInMainThreadAsync(Callable<T> task) {
        try {
            return CompletableFuture.completedFuture(task.call());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }


    @Override
    protected BlueprintTracker newTracker(Blueprint blueprint, World world, Vec3i origin) {
        return new BlueprintTracker(this, blueprint, world, origin, MockWorldTile::new);
    }
}
