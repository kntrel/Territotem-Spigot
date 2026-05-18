package com.kntrel.mc.territotem.geyser.render;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.display.DisplayToken;
import com.kntrel.mc.regionLib.region.display.RegionDisplayer;
import com.kntrel.mc.territotem.region.RegionColor;
import com.kntrel.mc.territotem.region.RegionColors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugBox;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugLine;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.packet.DebugDrawerPacket;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class BedrockRegionDisplayer implements RegionDisplayer {

    //CONSTANTS
    private static final float SCALE = 1.0f;
    private static final float MINIMUM_AXIS_SIZE = 0.05f;
    private static final double FACE_GRID_SPACING_BLOCKS = 1.0d;
    private static final int FACE_GRID_MAX_LINES_PER_DIRECTION = 64;
    private static final int DEFAULT_DISPLAY_DURATION_SECONDS = 10;
    private static final int OUTLINE_ALPHA = 208;
    private static final int FACE_ALPHA = 144;
    private static final long FIRST_SHAPE_ID = Long.MIN_VALUE;


    //FIELDS
    private final Plugin plugin_;
    private final AtomicLong nextId_ = new AtomicLong(1L);
    private final AtomicLong nextShapeId_ = new AtomicLong(FIRST_SHAPE_ID);
    private final Map<Long, RenderedDisplay> activeDisplays_ = new ConcurrentHashMap<>();
    private final int displayDurationSeconds_;


    //CONSTRUCTORS
    public BedrockRegionDisplayer(Plugin plugin) {
        this(plugin, DEFAULT_DISPLAY_DURATION_SECONDS);
    }
    public BedrockRegionDisplayer(Plugin plugin, int displayDurationSeconds) {
        this.plugin_ = Objects.requireNonNull(plugin, "plugin");
        this.displayDurationSeconds_ = Math.max(1, displayDurationSeconds);
    }


    //IMPLEMENTATION
    @Override
    public DisplayToken display(Region region) {
        return this.display(region, null);
    }
    @Override
    public DisplayToken display(Region region, Player player) {
        long id = this.nextId_.getAndIncrement();
        RenderedDisplay display = new RenderedDisplay(this.shapeIds());
        this.activeDisplays_.put(id, display);
        this.runSync(() -> this.startDisplay(id, display, region, player));
        return new DisplayToken(this, id);
    }
    @Override
    public void stop(DisplayToken token) {
        if (token == null || token.displayer() != this) {
            return;
        }
        RenderedDisplay display = this.activeDisplays_.remove(token.id());
        if (display != null) {
            this.runSync(() -> this.clear(display));
        }
    }

    public void stopAll() {
        List<RenderedDisplay> displays = List.copyOf(this.activeDisplays_.values());
        this.activeDisplays_.clear();
        this.runSync(() -> displays.forEach(this::clear));
    }

    private void clear(RenderedDisplay display) {
        for (Recipient recipient : display.recipients()) {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(recipient.playerId());
            if (!(connection instanceof GeyserSession session) || session.isClosed() || !session.isSpawned()) {
                continue;
            }
            this.send(session, this.removalShapes(display.shapeIds(), recipient.dimension()));
        }
    }


    //HELPERS
    private void startDisplay(long id, RenderedDisplay display, Region region, Player player) {
        if (this.activeDisplays_.get(id) != display) {
            return;
        }

        List<TargetSession> targets = this.targetSessions(region, player);
        if (targets.isEmpty()) {
            this.activeDisplays_.remove(id, display);
            return;
        }

        RegionSnapshot snapshot = RegionSnapshot.of(region);
        List<Recipient> recipients = targets.stream()
                .map(target -> new Recipient(target.playerId(), target.dimension()))
                .toList();
        display.recipients(recipients);

        this.runAsync(() -> {
            Map<Integer, List<DebugShape>> shapesByDimension = this.shapesByDimension(
                    display.shapeIds(),
                    snapshot,
                    recipients,
                    this.displayDurationSeconds_ + 1.0f
            );
            this.runSync(() -> this.sendBuiltShapes(id, display, snapshot.worldId(), shapesByDimension));
        });
    }

    private void sendBuiltShapes(
            long id,
            RenderedDisplay display,
            UUID worldId,
            Map<Integer, List<DebugShape>> shapesByDimension
    ) {
        if (this.activeDisplays_.get(id) != display) {
            return;
        }

        for (Recipient recipient : display.recipients()) {
            GeyserSession session = this.readySession(recipient.playerId(), worldId);
            if (session == null) {
                continue;
            }

            List<DebugShape> shapes = shapesByDimension.get(recipient.dimension());
            if (shapes != null) {
                this.send(session, shapes);
            }
        }
    }

    private List<TargetSession> targetSessions(Region region, Player player) {
        if (player != null) {
            TargetSession target = this.targetSession(GeyserApi.api().connectionByUuid(player.getUniqueId()), region.getWorld());
            return (target == null) ? List.of() : List.of(target);
        }

        List<TargetSession> out = new ArrayList<>();
        for (GeyserConnection connection : GeyserApi.api().onlineConnections()) {
            TargetSession target = this.targetSession(connection, region.getWorld());
            if (target != null) {
                out.add(target);
            }
        }
        return out;
    }

    private TargetSession targetSession(GeyserConnection connection, World world) {
        if (!(connection instanceof GeyserSession session) || session.isClosed() || !session.isSpawned()) {
            return null;
        }

        Player player = this.bukkitPlayer(connection);
        if (player == null || !player.getWorld().getUID().equals(world.getUID())) {
            return null;
        }

        return new TargetSession(session, player.getUniqueId(), session.getBedrockDimension().bedrockId());
    }

    private GeyserSession readySession(UUID playerId, UUID worldId) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(playerId);
        if (!(connection instanceof GeyserSession session) || session.isClosed() || !session.isSpawned()) {
            return null;
        }

        Player player = this.bukkitPlayer(connection);
        if (player == null || !player.getWorld().getUID().equals(worldId)) {
            return null;
        }

        return session;
    }

    private List<Long> shapeIds() {
        List<Long> out = new ArrayList<>();
        for (int i = 0; i < 1 + (6 * FACE_GRID_MAX_LINES_PER_DIRECTION * 2); i++) {
            out.add(this.nextShapeId_.getAndIncrement());
        }
        return out;
    }

    private Map<Integer, List<DebugShape>> shapesByDimension(
            List<Long> shapeIds,
            RegionSnapshot region,
            List<Recipient> recipients,
            float durationSeconds
    ) {
        Set<Integer> dimensions = new LinkedHashSet<>();
        for (Recipient recipient : recipients) {
            dimensions.add(recipient.dimension());
        }

        Map<Integer, List<DebugShape>> out = new HashMap<>();
        for (int dimension : dimensions) {
            out.put(dimension, this.shapes(shapeIds, region, dimension, durationSeconds));
        }
        return out;
    }

    private List<DebugShape> shapes(List<Long> shapeIds, RegionSnapshot region, int dimension, float durationSeconds) {
        List<DebugShape> out = new ArrayList<>(shapeIds.size());

        out.add(this.outline(shapeIds.get(0), region, dimension, durationSeconds));
        addFaceGrid(out, shapeIds, region, dimension, durationSeconds);
        return out;
    }

    private List<DebugShape> removalShapes(List<Long> shapeIds, int dimension) {
        List<DebugShape> out = new ArrayList<>(shapeIds.size());
        for (long shapeId : shapeIds) {
            out.add(new DebugShape(shapeId, dimension));
        }
        return out;
    }

    private Player bukkitPlayer(GeyserConnection connection) {
        Player player = Bukkit.getPlayer(connection.javaUuid());
        if (player == null && connection.javaUsername() != null) {
            player = Bukkit.getPlayerExact(connection.javaUsername());
        }
        return player;
    }

    private DebugBox outline(long id, RegionSnapshot region, int dimension, float durationSeconds) {
        return new DebugBox(
                id,
                dimension,
                center(region),
                SCALE,
                Vector3f.ZERO,
                durationSeconds,
                new Color(region.red(), region.green(), region.blue(), OUTLINE_ALPHA),
                bounds(region)
        );
    }

    private static void addFaceGrid(
            List<DebugShape> out,
            List<Long> shapeIds,
            RegionSnapshot region,
            int dimension,
            float durationSeconds
    ) {
        Color faceColor = new Color(region.red(), region.green(), region.blue(), FACE_ALPHA);
        long[] ids = shapeIds.stream().skip(1).mapToLong(Long::longValue).toArray();
        int index = 0;

        double minX = region.minX();
        double minY = region.minY();
        double minZ = region.minZ();
        double maxX = region.maxX();
        double maxY = region.maxY();
        double maxZ = region.maxZ();

        index = addXFaceGrid(out, ids, index, dimension, durationSeconds, faceColor, minX, minY, maxY, minZ, maxZ);
        index = addXFaceGrid(out, ids, index, dimension, durationSeconds, faceColor, maxX, minY, maxY, minZ, maxZ);
        index = addYFaceGrid(out, ids, index, dimension, durationSeconds, faceColor, minY, minX, maxX, minZ, maxZ);
        index = addYFaceGrid(out, ids, index, dimension, durationSeconds, faceColor, maxY, minX, maxX, minZ, maxZ);
        index = addZFaceGrid(out, ids, index, dimension, durationSeconds, faceColor, minZ, minX, maxX, minY, maxY);
        addZFaceGrid(out, ids, index, dimension, durationSeconds, faceColor, maxZ, minX, maxX, minY, maxY);
    }

    private static int addXFaceGrid(
            List<DebugShape> out,
            long[] ids,
            int index,
            int dimension,
            float durationSeconds,
            Color color,
            double x,
            double minY,
            double maxY,
            double minZ,
            double maxZ
    ) {
        for (double y : gridPositions(minY, maxY)) {
            out.add(line(ids[index++], dimension, durationSeconds, color, x, y, minZ, x, y, maxZ));
        }
        for (double z : gridPositions(minZ, maxZ)) {
            out.add(line(ids[index++], dimension, durationSeconds, color, x, minY, z, x, maxY, z));
        }
        return index;
    }

    private static int addYFaceGrid(
            List<DebugShape> out,
            long[] ids,
            int index,
            int dimension,
            float durationSeconds,
            Color color,
            double y,
            double minX,
            double maxX,
            double minZ,
            double maxZ
    ) {
        for (double x : gridPositions(minX, maxX)) {
            out.add(line(ids[index++], dimension, durationSeconds, color, x, y, minZ, x, y, maxZ));
        }
        for (double z : gridPositions(minZ, maxZ)) {
            out.add(line(ids[index++], dimension, durationSeconds, color, minX, y, z, maxX, y, z));
        }
        return index;
    }

    private static int addZFaceGrid(
            List<DebugShape> out,
            long[] ids,
            int index,
            int dimension,
            float durationSeconds,
            Color color,
            double z,
            double minX,
            double maxX,
            double minY,
            double maxY
    ) {
        for (double x : gridPositions(minX, maxX)) {
            out.add(line(ids[index++], dimension, durationSeconds, color, x, minY, z, x, maxY, z));
        }
        for (double y : gridPositions(minY, maxY)) {
            out.add(line(ids[index++], dimension, durationSeconds, color, minX, y, z, maxX, y, z));
        }
        return index;
    }

    private static DebugLine line(
            long id,
            int dimension,
            float durationSeconds,
            Color color,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ
    ) {
        return new DebugLine(
                id,
                dimension,
                Vector3f.from((float) startX, (float) startY, (float) startZ),
                SCALE,
                Vector3f.ZERO,
                durationSeconds,
                color,
                Vector3f.from((float) endX, (float) endY, (float) endZ)
        );
    }

    private static List<Double> gridPositions(double min, double max) {
        double width = Math.max(0.0d, max - min);
        if (width <= MINIMUM_AXIS_SIZE) {
            return List.of();
        }

        int count = Math.min(
                FACE_GRID_MAX_LINES_PER_DIRECTION,
                Math.max(1, (int) Math.floor(width / FACE_GRID_SPACING_BLOCKS))
        );
        double step = width / (count + 1);
        List<Double> out = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            out.add(min + (step * i));
        }
        return out;
    }

    private void send(GeyserSession session, List<DebugShape> shapes) {
        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().addAll(shapes);
        session.sendUpstreamPacket(packet);
    }

    private void runAsync(Runnable task) {
        this.plugin_.getServer().getScheduler().runTaskAsynchronously(this.plugin_, task);
    }

    private void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }

        this.plugin_.getServer().getScheduler().runTask(this.plugin_, task);
    }

    private static Vector3f center(RegionSnapshot region) {
        return Vector3f.from(
                (float) ((region.minX() + region.maxX()) * 0.5d),
                (float) ((region.minY() + region.maxY()) * 0.5d),
                (float) ((region.minZ() + region.maxZ()) * 0.5d)
        );
    }

    private static Vector3f bounds(RegionSnapshot region) {
        return Vector3f.from(
                (float) Math.max(MINIMUM_AXIS_SIZE, region.maxX() - region.minX()),
                (float) Math.max(MINIMUM_AXIS_SIZE, region.maxY() - region.minY()),
                (float) Math.max(MINIMUM_AXIS_SIZE, region.maxZ() - region.minZ())
        );
    }


    //SUBTYPES
    private static final class RenderedDisplay {

        private final List<Long> shapeIds_;
        private volatile List<Recipient> recipients_ = List.of();

        private RenderedDisplay(List<Long> shapeIds) {
            this.shapeIds_ = List.copyOf(shapeIds);
        }

        private List<Long> shapeIds() {
            return this.shapeIds_;
        }

        private List<Recipient> recipients() {
            return this.recipients_;
        }

        private void recipients(List<Recipient> recipients) {
            this.recipients_ = List.copyOf(recipients);
        }
    }

    private record Recipient(UUID playerId, int dimension) {}

    private record TargetSession(GeyserSession session, UUID playerId, int dimension) {}

    private record RegionSnapshot(
            UUID worldId,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            int red,
            int green,
            int blue
    ) {

        private static RegionSnapshot of(Region region) {
            RegionColor color = RegionColors.getOrDefault(region);
            return new RegionSnapshot(
                    region.getWorld().getUID(),
                    region.getMinX(),
                    region.getMinY(),
                    region.getMinZ(),
                    region.getMaxX(),
                    region.getMaxY(),
                    region.getMaxZ(),
                    color.red(),
                    color.green(),
                    color.blue()
            );
        }
    }
}
