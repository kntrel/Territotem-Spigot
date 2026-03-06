package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.worldTile.MockWorldTile;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class MockStructureService extends StructureService {

    public MockStructureService(Plugin plugin) {
        super(plugin, new ChunkPersister(plugin));
    }



    @Override
    <T> CompletableFuture<T> runInMainThreadAsync(Callable<T> task) {
        try {
            return CompletableFuture.completedFuture(task.call());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    <T> T runInMainThread(Callable<T> task) {
        try { return task.call(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    protected Structure newStructure(Blueprint blueprint, World world, Vec3i origin) {
        return new Structure(this, blueprint, world, origin, MockWorldTile::new);
    }
}
