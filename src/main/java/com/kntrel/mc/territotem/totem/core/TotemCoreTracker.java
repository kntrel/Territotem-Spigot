package com.kntrel.mc.territotem.totem.core;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.structure.StructureService;
import com.kntrel.mc.territotem.structure.worldTile.WorldView;
import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TotemCoreTracker {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemCoreTracker.class);
    private static final String CORES_KEY = "totem_cores";


    //FIELDS
    private final RegionContext regionContext_;
    private final StructureService structureService_;
    private final ChunkPersister chunkPersister_;
    private final NamespacedKey coresNSK_;
    private final TotemCoreListener listener_;
    private final Map<UUID, Map<Vec3i, TotemCore>> coresByWorld_;
    private final Executor executor_;


    //CONSTRUCTORS
    public TotemCoreTracker(RegionContext regionContext, StructureService structureService, ChunkPersister chunkPersister) {
        this.regionContext_ = regionContext;
        this.structureService_ = structureService;
        this.chunkPersister_ = chunkPersister;
        this.coresNSK_ = new NamespacedKey(regionContext.getPlugin(), CORES_KEY);
        this.coresByWorld_ = new ConcurrentHashMap<>();
        this.listener_ = new TotemCoreListener(this);
        this.executor_ = Executors.newVirtualThreadPerTaskExecutor();

        this.regionContext_.getServer().getPluginManager().registerEvents(this.listener_, this.getPlugin());
    }


    //GETTERS
    public RegionContext getRegionContext() {
        return this.regionContext_;
    }
    public Plugin getPlugin() {
        return this.regionContext_.getPlugin();
    }
    public Server getServer() {
        return this.regionContext_.getServer();
    }
    public StructureService getStructureService() {
        return this.structureService_;
    }


    //SERVICES
    public TotemCore createCore(Vec3i coordinates, World world, TotemCore.State state, TotemCore.Direction direction) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.computeIfAbsent(world.getUID(), ignored -> new ConcurrentHashMap<>());
        TotemCore existing = worldCores.get(coordinates);
        if (existing != null) {
            existing.kill();
            existing.setState(state);
            existing.setDirection(direction);
            this.persistCore(existing);
            return existing;
        }

        TotemCore core = new TotemCore(coordinates, world, state, direction);
        worldCores.put(coordinates, core);
        this.persistCore(core);
        return core;
    }

    public void breakCore(TotemCore core) {
        core.breakDown();
        this.destroyCore(core);
    }

    public boolean breakCore(Block block) {
        TotemCore core = this.getCore(block);
        if (core == null) {
            return false;
        }

        core.breakDown();
        this.destroyCore(core);
        return true;
    }

    public Collection<TotemCore> getLoadedCores() {
        List<TotemCore> out = new ArrayList<>();
        for (Map<Vec3i, TotemCore> worldCores : this.coresByWorld_.values()) {
            out.addAll(worldCores.values());
        }
        return Collections.unmodifiableList(out);
    }

    public boolean isCoreAt(World world, Vec3i coordinates) {
        return this.getCoreAt(world.getUID(), coordinates) != null;
    }

    public boolean isCore(Block block) {
        return this.isCoreAt(block.getWorld(), Vec3i.ofBlock(block));
    }

    public CompletableFuture<List<TotemCore>> getNearByCores(World world, Vec3i coordinates) {

        Map<Vec3i, TotemCore> worldTotems = this.coresByWorld_.get(world.getUID());
        if (worldTotems == null) { return CompletableFuture.completedFuture(Collections.emptyList()); }

        int     chunkX = coordinates.x() >> Constants.CHUNK_SHIFT,
                chunkZ = coordinates.z() >> Constants.CHUNK_SHIFT,
                minX = (chunkX - 1) << Constants.CHUNK_SHIFT,
                minZ = (chunkZ - 1) << Constants.CHUNK_SHIFT,
                maxX = ((chunkX + 2) << Constants.CHUNK_SHIFT) - 1,
                maxZ = ((chunkZ + 2) << Constants.CHUNK_SHIFT) - 1;

        return this.coresIn(minX, minZ, maxX, maxZ, world.getUID());

    }

    public CompletableFuture<List<TotemCore>> getNearByCores(Block block) {
        return this.getNearByCores(block.getWorld(), Vec3i.ofBlock(block));
    }

    public void destroyCore(TotemCore core) {
        this.unloadCore(core);
        this.persistChunk(core.getWorld(), core.getCoordinates().x() >> Constants.CHUNK_SHIFT, core.getCoordinates().z() >> Constants.CHUNK_SHIFT);
    }

    public void persistCore(TotemCore core) {
        this.persistChunk(core.getWorld(), core.getCoordinates().x() >> Constants.CHUNK_SHIFT, core.getCoordinates().z() >> Constants.CHUNK_SHIFT);
    }

    public TotemCore getCoreAt(UUID worldUUID, Vec3i coordinates) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(worldUUID);
        if (worldCores == null) {
            return null;
        }
        return worldCores.get(coordinates);
    }

    public TotemCore getCoreAt(WorldView world, Vec3i coordinates) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(world.id());
        if (worldCores == null) {
            return null;
        }
        return worldCores.get(coordinates);
    }

    public TotemCore getCore(Block block) {
        return this.getCoreAt(block.getWorld().getUID(), Vec3i.ofBlock(block));
    }

    void handleChunkLoad(Chunk chunk) {
        List<TotemCoreChunkData> cores = this.chunkPersister_.retrieve(chunk, this.coresNSK_, TotemCorePersistentDataType.instance());
        if (cores == null || cores.isEmpty()) {
            return;
        }

        int baseX = chunk.getX() * Constants.CHUNK_SIZE;
        int baseZ = chunk.getZ() * Constants.CHUNK_SIZE;

        for (TotemCoreChunkData data : cores) {
            Vec3i absolute = data.offset().add(new Vec3i(baseX, 0, baseZ));
            this.createCore(absolute, chunk.getWorld(), data.state(), data.direction());
        }

        LOGGER.debug("Deserialized {} totem cores in chunk [{}, {}]", cores.size(), chunk.getX(), chunk.getZ());
    }


    //HELPERS
    void unloadCore(TotemCore core) {
        core.kill();;
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(core.getWorld().getUID());
        if (worldCores == null) { return; }

        worldCores.remove(core.getCoordinates());
        if (worldCores.isEmpty()) {
            this.coresByWorld_.remove(core.getWorld().getUID());
        }
    }

    void unloadChunk(World world, int chunkX, int chunkZ) {
        int baseX = chunkX << Constants.CHUNK_SHIFT;
        int baseZ = chunkZ << Constants.CHUNK_SHIFT;

        this.coresIn(baseX, baseZ, baseX + Constants.CHUNK_SIZE, baseZ + Constants.CHUNK_SIZE, world.getUID())
                .thenAccept(l -> this.getServer().getScheduler().runTask(this.getPlugin(), () -> l.forEach(this::unloadCore)));
    }

    private void persistChunk(World world, int chunkX, int chunkZ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) { return; }

        int baseX = chunkX << Constants.CHUNK_SHIFT;
        int baseZ = chunkZ << Constants.CHUNK_SHIFT;
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);

        this.coresIn(baseX, baseZ, baseX + Constants.CHUNK_SIZE, baseZ + Constants.CHUNK_SIZE, world.getUID())
            .thenAccept(l -> {
                List<TotemCoreChunkData> serialized = new ArrayList<>();
                for (TotemCore core : l) {
                    Vec3i pos = core.getCoordinates();
                    Vec3i rel = new Vec3i(pos.x() - baseX, pos.y(), pos.z() - baseZ);
                    serialized.add(new TotemCoreChunkData(rel, core.getState(), core.getDirection()));
                }
                this.chunkPersister_.persist(chunk, this.coresNSK_, TotemCorePersistentDataType.instance(), serialized);
            });
    }

    private CompletableFuture<List<TotemCore>> coresIn(int minX, int minZ, int maxX, int maxZ, UUID world) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(world);
            if (worldCores == null) { return Collections.emptyList(); }
            return worldCores.values().stream()
                .filter(c -> {
                    Vec3i v = c.getCoordinates();
                    return v.x() >= minX && v.x() <= maxX && v.z() >= minZ && v.z() <= maxZ;
                })
                .toList();
        }, this.executor_);
    }
}

