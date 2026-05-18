package com.kntrel.mc.territotem.geyser.render;

import com.kntrel.util.Vec3i;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BedrockBlockRenderer {

    //FIELDS
    private final Plugin plugin_;
    private final ExtensionLogger logger_;
    private final Map<BlockKey, CustomBlockState> renderedBlocks_;
    private final Set<String> missingDefinitions_;
    private final BedrockBlockRenderListener listener_;


    //CONSTRUCTORS
    public BedrockBlockRenderer(Plugin plugin, Extension extension) {
        this.plugin_ = plugin;
        this.logger_ = extension.logger();
        this.renderedBlocks_ = new ConcurrentHashMap<>();
        this.missingDefinitions_ = ConcurrentHashMap.newKeySet();
        this.listener_ = new BedrockBlockRenderListener(plugin, this.logger_, this);

        plugin.getServer().getPluginManager().registerEvents(this.listener_, plugin);
        extension.eventBus().register(this.listener_);
        Bukkit.getOnlinePlayers().forEach(this.listener_::markDirty);
    }


    //SERVICES
    public void render(Block block, CustomBlockData blockData) {
        this.render(block, blockData.defaultBlockState());
    }

    public void render(Block block, CustomBlockState blockState) {
        this.render(block.getWorld(), Vec3i.ofBlock(block), blockState);
    }

    public void render(Location location, CustomBlockData blockData) {
        this.render(location, blockData.defaultBlockState());
    }

    public void render(Location location, CustomBlockState blockState) {
        World world = Objects.requireNonNull(location.getWorld(), "location must have a world");
        this.render(world, location.getBlockX(), location.getBlockY(), location.getBlockZ(), blockState);
    }

    public void render(World world, Vec3i coordinates, CustomBlockData blockData) {
        this.render(world, coordinates, blockData.defaultBlockState());
    }

    public void render(World world, Vec3i coordinates, CustomBlockState blockState) {
        this.render(world, coordinates.x(), coordinates.y(), coordinates.z(), blockState);
    }

    public void render(World world, int x, int y, int z, CustomBlockData blockData) {
        this.render(world, x, y, z, blockData.defaultBlockState());
    }

    public void render(World world, int x, int y, int z, CustomBlockState blockState) {
        BlockKey key = new BlockKey(world.getUID(), x, y, z);
        this.renderedBlocks_.put(key, blockState);
        this.listener_.blockRendered(key, blockState);
    }

    public void clear(Block block) {
        this.clear(block.getWorld(), Vec3i.ofBlock(block));
    }

    public void clear(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "location must have a world");
        this.clear(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void clear(World world, Vec3i coordinates) {
        this.clear(world, coordinates.x(), coordinates.y(), coordinates.z());
    }

    public void clear(World world, int x, int y, int z) {
        BlockKey key = new BlockKey(world.getUID(), x, y, z);
        if (this.renderedBlocks_.remove(key) != null) {
            this.listener_.blockCleared(key);
        }
    }

    public void clearAll() {
        Map<BlockKey, CustomBlockState> previous = Map.copyOf(this.renderedBlocks_);
        this.renderedBlocks_.clear();
        previous.keySet().forEach(this::restoreToAll);
    }

    public void stop() {
        this.listener_.stop();
        HandlerList.unregisterAll(this.listener_);
    }

    public Map<Vec3i, CustomBlockState> renderedBlocks(World world) {
        Map<Vec3i, CustomBlockState> out = new LinkedHashMap<>();
        for (Map.Entry<BlockKey, CustomBlockState> entry : this.renderedBlocks_.entrySet()) {
            BlockKey key = entry.getKey();
            if (key.world().equals(world.getUID())) {
                out.put(new Vec3i(key.x(), key.y(), key.z()), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(out);
    }


    //PACKAGE SERVICES
    void renderWindow(UUID player, UUID world, int centerChunkX, int centerChunkZ, int radius) {
        for (Map.Entry<BlockKey, CustomBlockState> entry : this.renderedBlocks_.entrySet()) {
            BlockKey key = entry.getKey();
            if (key.isInside(world, centerChunkX, centerChunkZ, radius)) {
                this.renderTo(player, key, entry.getValue());
            }
        }
    }

    void renderWindowChange(
            UUID player,
            UUID world,
            int previousChunkX,
            int previousChunkZ,
            int previousRadius,
            int currentChunkX,
            int currentChunkZ,
            int currentRadius,
            int criticalRadius
    ) {
        for (Map.Entry<BlockKey, CustomBlockState> entry : this.renderedBlocks_.entrySet()) {
            BlockKey key = entry.getKey();
            boolean inCriticalWindow = key.isInside(world, currentChunkX, currentChunkZ, criticalRadius);
            boolean justEnteredWindow =
                       key.isInside(world, currentChunkX, currentChunkZ, currentRadius)
                    && !key.isInside(world, previousChunkX, previousChunkZ, previousRadius);
            if (inCriticalWindow || justEnteredWindow) {
                this.renderTo(player, key, entry.getValue());
            }
        }
    }

    void renderTo(UUID player, BlockKey key, CustomBlockState blockState) {
        this.runSync(() -> {
            if (!Objects.equals(this.renderedBlocks_.get(key), blockState)) {
                return;
            }

            GeyserConnection connection = this.connection(player);
            if (connection != null) {
                this.renderTo(connection, key, blockState);
            }
        });
    }

    void restoreTo(UUID player, BlockKey key) {
        this.runSync(() -> {
            if (this.renderedBlocks_.containsKey(key)) {
                return;
            }

            GeyserConnection connection = this.connection(player);
            if (connection != null) {
                this.restoreTo(connection, key);
            }
        });
    }


    //HELPERS
    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }

        this.plugin_.getServer().getScheduler().runTask(this.plugin_, task);
    }

    private GeyserConnection connection(UUID player) {
        return org.geysermc.geyser.api.GeyserApi.api().connectionByUuid(player);
    }

    private void restoreToAll(BlockKey key) {
        for (GeyserConnection connection : org.geysermc.geyser.api.GeyserApi.api().onlineConnections()) {
            this.restoreTo(connection, key);
        }
    }

    private void renderTo(GeyserConnection connection, BlockKey key, CustomBlockState blockState) {
        GeyserSession session = this.readySession(connection, key);
        if (session == null) {
            return;
        }

        BlockDefinition definition = this.resolveDefinition(session, blockState);
        if (definition == null) {
            this.warnMissingDefinition(blockState);
            return;
        }

        this.sendBlock(session, key.toVector(), 0, definition);
        this.sendBlock(session, key.toVector(), 1, session.getBlockMappings().getBedrockAir());
    }

    private void restoreTo(GeyserConnection connection, BlockKey key) {
        GeyserSession session = this.readySession(connection, key);
        if (session == null) {
            return;
        }
        BlockUtils.restoreCorrectBlock(session, key.toVector(), session.getPlayerInventory().getHeldItemSlot());
    }

    private GeyserSession readySession(GeyserConnection connection, BlockKey key) {
        if (!(connection instanceof GeyserSession session) || session.isClosed() || !session.isSpawned()) {
            return null;
        }

        Player player = this.bukkitPlayer(connection);
        if (player == null || !player.getWorld().getUID().equals(key.world())) {
            return null;
        }

        return session;
    }

    private Player bukkitPlayer(GeyserConnection connection) {
        Player player = Bukkit.getPlayer(connection.javaUuid());
        if (player == null && connection.javaUsername() != null) {
            player = Bukkit.getPlayerExact(connection.javaUsername());
        }
        return player;
    }

    private void sendBlock(GeyserSession session, Vector3i position, int dataLayer, BlockDefinition definition) {
        UpdateBlockPacket packet = new UpdateBlockPacket();
        packet.setDataLayer(dataLayer);
        packet.setBlockPosition(position);
        packet.setDefinition(definition);
        packet.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(packet);
    }

    private BlockDefinition resolveDefinition(GeyserSession session, CustomBlockState blockState) {
        return session.getBlockMappings().getDefinition(this.blockStateTag(blockState));
    }

    private NbtMap blockStateTag(CustomBlockState blockState) {
        return NbtMap.builder()
                .putString("name", blockState.block().identifier())
                .putCompound("states", NbtMap.fromMap(normalize(blockState.properties())))
                .build();
    }

    private static Map<String, Object> normalize(Map<String, Object> properties) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean bool) {
                value = bool ? (byte) 1 : (byte) 0;
            }
            out.put(entry.getKey(), value);
        }
        return out;
    }

    private void warnMissingDefinition(CustomBlockState blockState) {
        String key = blockState.block().identifier() + blockState.properties();
        if (this.missingDefinitions_.add(key)) {
            this.logger_.warning("No Bedrock definition found for custom block state " + key + ".");
        }
    }


    //SUBTYPES
    record BlockKey(UUID world, int x, int y, int z) {
        private Vector3i toVector() {
            return Vector3i.from(this.x, this.y, this.z);
        }

        int chunkX() {
            return this.x >> 4;
        }

        int chunkZ() {
            return this.z >> 4;
        }

        boolean isInside(UUID world, int centerChunkX, int centerChunkZ, int radius) {
            return this.world.equals(world)
                    && Math.abs(this.chunkX() - centerChunkX) <= radius
                    && Math.abs(this.chunkZ() - centerChunkZ) <= radius;
        }
    }
}
