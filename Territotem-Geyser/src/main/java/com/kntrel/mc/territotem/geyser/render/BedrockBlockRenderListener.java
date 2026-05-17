package com.kntrel.mc.territotem.geyser.render;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.session.GeyserSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

final class BedrockBlockRenderListener implements Listener {

    //CONSTANTS
    private static final int CRITICAL_WINDOW_RADIUS = 6;
    private static final long DIRTY_SCAN_PERIOD_TICKS = 20L;


    //FIELDS
    private final Plugin plugin_;
    private final ExtensionLogger logger_;
    private final BedrockBlockRenderer renderer_;
    private final Set<UUID> dirtyPlayers_;
    private final Map<UUID, PlayerView> playerViews_;
    private final BukkitTask dirtyScanTask_;


    //CONSTRUCTORS
    BedrockBlockRenderListener(Plugin plugin, ExtensionLogger logger, BedrockBlockRenderer renderer) {
        this.plugin_ = plugin;
        this.logger_ = logger;
        this.renderer_ = renderer;
        this.dirtyPlayers_ = ConcurrentHashMap.newKeySet();
        this.playerViews_ = new ConcurrentHashMap<>();
        this.dirtyScanTask_ = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::scanDirtyPlayers,
                DIRTY_SCAN_PERIOD_TICKS,
                DIRTY_SCAN_PERIOD_TICKS
        );
    }


    //LISTENERS
    @Subscribe
    public void onSessionJoin(SessionJoinEvent event) {
        UUID player = event.connection().javaUuid();
        if (player != null) {
            this.markDirty(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        this.markDirty(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        this.markDirty(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        this.markDirty(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.markDirty(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        this.dirtyPlayers_.remove(player);
        this.playerViews_.remove(player);
    }


    //SERVICES
    void markDirty(Player player) {
        this.markDirty(player.getUniqueId());
    }

    void markDirty(UUID player) {
        this.dirtyPlayers_.add(player);
    }

    void stop() {
        this.dirtyScanTask_.cancel();
    }

    void blockRendered(BedrockBlockRenderer.BlockKey key, CustomBlockState blockState) {
        for (PlayerView view : this.playerViews_.values()) {
            if (!view.contains(key)) {
                continue;
            }

            this.renderer_.renderTo(view.player(), key, blockState);
        }
    }

    void blockCleared(BedrockBlockRenderer.BlockKey key) {
        for (PlayerView view : this.playerViews_.values()) {
            if (!view.contains(key)) {
                continue;
            }

            this.renderer_.restoreTo(view.player(), key);
        }
    }


    //HELPERS
    private void scanDirtyPlayers() {
        Set<UUID> dirty = this.drainDirtyPlayers();
        if (dirty.isEmpty()) {
            return;
        }

        Future<List<PlayerSnapshot>> future = this.plugin_.getServer().getScheduler().callSyncMethod(
                this.plugin_,
                () -> this.snapshotPlayers(dirty)
        );

        List<PlayerSnapshot> snapshots;
        try {
            snapshots = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException e) {
            this.logger_.warning("Failed to snapshot Bedrock player render windows: " + e.getCause());
            return;
        }

        List<RenderJob> jobs = new ArrayList<>();
        for (PlayerSnapshot snapshot : snapshots) {
            PlayerView view = snapshot.toView();
            PlayerView previous = this.playerViews_.put(snapshot.player(), view);
            RenderJob job = this.renderJob(previous, view);
            if (job != null) {
                jobs.add(job);
            }
        }

        this.render(jobs);
    }

    private Set<UUID> drainDirtyPlayers() {
        Set<UUID> dirty = new HashSet<>();
        for (UUID player : this.dirtyPlayers_) {
            if (this.dirtyPlayers_.remove(player)) {
                dirty.add(player);
            }
        }
        return dirty;
    }

    private List<PlayerSnapshot> snapshotPlayers(Collection<UUID> playerIds) {
        List<PlayerSnapshot> out = new ArrayList<>();
        for (UUID playerId : playerIds) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                this.playerViews_.remove(playerId);
                continue;
            }

            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerId);
            if (!(connection instanceof GeyserSession session) || session.isClosed()) {
                this.playerViews_.remove(playerId);
                continue;
            }

            if (!session.isSpawned()) {
                this.markDirty(playerId);
                continue;
            }

            Location location = player.getLocation();
            out.add(new PlayerSnapshot(
                    playerId,
                    player.getWorld().getUID(),
                    location.getBlockX() >> 4,
                    location.getBlockZ() >> 4,
                    renderDistance(session)
            ));
        }
        return out;
    }

    private RenderJob renderJob(PlayerView previous, PlayerView current) {
        if (previous == null || !previous.world().equals(current.world())) {
            return RenderJob.full(current);
        }

        if (   previous.chunkX() == current.chunkX()
            && previous.chunkZ() == current.chunkZ()
            && previous.renderDistance() == current.renderDistance()
        ) {
            return null;
        }

        return RenderJob.change(previous, current);
    }

    private void render(List<RenderJob> jobs) {
        for (RenderJob job : jobs) {
            if (job.previous() == null) {
                this.renderer_.renderWindow(job.view().player(), job.view().world(), job.view().chunkX(), job.view().chunkZ(), job.view().renderDistance());
                continue;
            }

            this.renderer_.renderWindowChange(
                    job.view().player(),
                    job.view().world(),
                    job.previous().chunkX(),
                    job.previous().chunkZ(),
                    job.previous().renderDistance(),
                    job.view().chunkX(),
                    job.view().chunkZ(),
                    job.view().renderDistance(),
                    Math.min(job.view().renderDistance(), CRITICAL_WINDOW_RADIUS)
            );
        }
    }

    private static int renderDistance(GeyserSession session) {
        int client = session.getClientRenderDistance();
        int server = session.getServerRenderDistance();
        if (client <= 0) {
            return Math.max(server, CRITICAL_WINDOW_RADIUS);
        }
        if (server <= 0) {
            return Math.max(client, CRITICAL_WINDOW_RADIUS);
        }
        return Math.min(client, server);
    }


    //SUBTYPES
    private record PlayerSnapshot(UUID player, UUID world, int chunkX, int chunkZ, int renderDistance) {
        private PlayerView toView() {
            return new PlayerView(this.player, this.world, this.chunkX, this.chunkZ, this.renderDistance);
        }
    }

    private record PlayerView(UUID player, UUID world, int chunkX, int chunkZ, int renderDistance) {
        private boolean contains(BedrockBlockRenderer.BlockKey key) {
            return key.isInside(this.world, this.chunkX, this.chunkZ, this.renderDistance);
        }
    }

    private record RenderJob(PlayerView view, PlayerView previous) {
        private static RenderJob full(PlayerView view) {
            return new RenderJob(view, null);
        }

        private static RenderJob change(PlayerView previous, PlayerView view) {
            return new RenderJob(view, previous);
        }
    }
}
