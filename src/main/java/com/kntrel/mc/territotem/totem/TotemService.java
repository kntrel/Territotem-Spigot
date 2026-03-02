package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TotemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotemService.class);
    private static final String CORES_KEY = "totem_cores";

    private final Plugin plugin_;
    private final NamespacedKey coresNSK_;
    private final TotemServiceListener listener_;
    private final Map<UUID, Map<Vec3i, TotemCore>> coresByWorld_;

    public TotemService(Plugin plugin) {
        this.plugin_ = plugin;
        this.coresNSK_ = new NamespacedKey(plugin, CORES_KEY);
        this.coresByWorld_ = new ConcurrentHashMap<>();
        this.listener_ = new TotemServiceListener(this);

        this.plugin_.getServer().getPluginManager().registerEvents(this.listener_, this.plugin_);
    }

    public TotemCore createCore(Vec3i coordinates, World world, TotemCore.State state, TotemCore.Direction direction) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.computeIfAbsent(world.getUID(), ignored -> new ConcurrentHashMap<>());
        TotemCore existing = worldCores.get(coordinates);
        if (existing != null) {
            existing.setState(state);
            existing.setDirection(direction);
            return existing;
        }

        TotemCore core = new TotemCore(coordinates, world, state, direction);
        worldCores.put(coordinates, core);
        return core;
    }

    public void breakCore(Block block) {
        TotemCore core = this.getCore(block);
        if (core == null) {
            return;
        }

        core.breakDown();
        this.dropCore(core);
    }

    public Collection<TotemCore> getExistingCores() {
        List<TotemCore> out = new ArrayList<>();
        for (Map<Vec3i, TotemCore> worldCores : this.coresByWorld_.values()) {
            out.addAll(worldCores.values());
        }
        return Collections.unmodifiableList(out);
    }

    public boolean isCore(Block block) {
        return this.getCore(block) != null;
    }

    TotemCore getCore(Block block) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(block.getWorld().getUID());
        if (worldCores == null) {
            return null;
        }

        return worldCores.get(Vec3i.ofBlock(block));
    }

    void handleChunkUnload(Chunk chunk) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(chunk.getWorld().getUID());
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (worldCores == null || worldCores.isEmpty()) {
            pdc.remove(this.coresNSK_);
            return;
        }

        int baseX = chunk.getX() * Constants.CHUNK_SIZE;
        int baseZ = chunk.getZ() * Constants.CHUNK_SIZE;

        List<TotemCoreChunkData> serialized = new ArrayList<>();
        for (Map.Entry<Vec3i, TotemCore> entry : worldCores.entrySet()) {
            Vec3i pos = entry.getKey();
            if (!isInChunk(pos, chunk)) {
                continue;
            }

            TotemCore core = entry.getValue();
            Vec3i rel = new Vec3i(pos.x() - baseX, pos.y(), pos.z() - baseZ);
            serialized.add(new TotemCoreChunkData(rel, core.getState(), core.getDirection()));
        }

        if (serialized.isEmpty()) {
            pdc.remove(this.coresNSK_);
            return;
        }

        for (TotemCoreChunkData data : serialized) {
            Vec3i absolute = data.offset().add(new Vec3i(baseX, 0, baseZ));
            worldCores.remove(absolute);
        }

        pdc.set(this.coresNSK_, TotemCorePersistentDataType.instance(), serialized);
        LOGGER.debug("Serialized {} totem cores in chunk [{}, {}]", serialized.size(), chunk.getX(), chunk.getZ());
    }

    void handleChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        List<TotemCoreChunkData> cores = pdc.get(this.coresNSK_, TotemCorePersistentDataType.instance());
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

    private static boolean isInChunk(Vec3i position, Chunk chunk) {
        return (position.x() >> 4) == chunk.getX() && (position.z() >> 4) == chunk.getZ();
    }

    private void dropCore(TotemCore core) {
        Map<Vec3i, TotemCore> worldCores = this.coresByWorld_.get(core.getWorld().getUID());
        if (worldCores == null) {
            return;
        }

        worldCores.remove(core.getCoordinates());
        if (worldCores.isEmpty()) {
            this.coresByWorld_.remove(core.getWorld().getUID());
        }
    }
}
