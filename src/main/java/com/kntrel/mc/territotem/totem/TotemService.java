package com.kntrel.mc.territotem.totem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.event.RegionLoadEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.runical.core.Placeholder;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.event.StructureChangedEvent;
import com.kntrel.mc.territotem.structure.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.structure.event.StructureLoadedEvent;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.event.TotemCoreBreakEvent;
import com.kntrel.mc.territotem.totem.event.TotemCoreRightClickedEvent;
import com.kntrel.mc.territotem.totem.piece.TotemCorePiece;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.mc.territotem.totem.region.RegionAllocator;
import com.kntrel.mc.territotem.totem.region.RegionPlaceResult;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.IntBoundingBox;
import com.kntrel.util.Vec3i;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TotemService implements Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemService.class);
    private static final String TOTEM_DATA_KEY = "totemData";
    private static final double REGION_GROWTH_RATE = 6d;
    private static final double EPSILON = 1.0E-9;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TotemClaim.class, TotemClaim.serializer())
            .registerTypeAdapter(TotemClaim.class, TotemClaim.deserializer())
            .create();


    //FIELDS
    private final RegionContext regionContext_;
    private final Plugin plugin_;
    private final Runical runical_;
    private final TotemAssembler assembler_;
    private final RegionAllocator regionAllocator_;
    private final Map<ChunkKey, Map<UUID, PendingExpectation>> pendingExpectationsByChunk_;
    private final Map<ChunkKey, Map<UUID, Structure>> pendingIngestionsByChunk_;
    private final Set<ChunkKey> scheduledAudits_;
    private final TotemStore totemStore_;


    //CONSTRUCTOR
    public TotemService(RegionContext regionContext, Runical runical) {
        this.regionContext_ = regionContext;
        this.plugin_ = this.regionContext_.getPlugin();
        this.runical_ = runical;
        this.assembler_ = new TotemAssembler();
        this.regionAllocator_ = new RegionAllocator(this.regionContext_, Condition.hasDataKey(TOTEM_DATA_KEY));
        this.pendingExpectationsByChunk_ = new ConcurrentHashMap<>();
        this.pendingIngestionsByChunk_ = new ConcurrentHashMap<>();
        this.scheduledAudits_ = ConcurrentHashMap.newKeySet();
        this.totemStore_ = new TotemStore();

        this.assembler_.consume(this::loadTotem);
        this.plugin_.getServer().getPluginManager().registerEvents(this, this.plugin_);
    }


    //API
    public List<Totem> totemsAtChunk(int x, int z, World world) {
        return this.totemStore_.getAroundChunk(x, z, world);
    }
    public Optional<Totem> totemOfRegion(Region region) {
        return this.totemStore_.getFromRegion(region);
    }


    //LISTENERS
    @EventHandler void onRegionLoad(RegionLoadEvent e) {
        Region region = e.getRegion();
        TotemClaim claim = readClaim(region);
        if (claim == null) { return; }

        ChunkKey chunkKey = new ChunkKey(claim.chunkX(), claim.chunkZ(), region.getWorld());
        this.enqueueExpectation(chunkKey, claim, region);
        this.scheduleAudit(chunkKey);
    }

    @EventHandler void onStructureLoad(StructureLoadedEvent e) {
        Structure structure = e.getStructure();
        if (!(structure.blueprint() instanceof TotemBlueprint)) { return; }

        ChunkKey chunkKey = ChunkKey.ofBlock(structure.origin(), structure.world().getUID());
        this.enqueueIngestion(chunkKey, structure);
        this.scheduleAudit(chunkKey);
    }

    @EventHandler void onChunkLoad(ChunkLoadEvent e) {
        this.scheduleAudit(new ChunkKey(e.getChunk()));
    }

    @EventHandler void onTotemCompleted(StructureCompletedEvent e) {
        Structure structure = e.getStructure();
        if (!(structure.blueprint() instanceof TotemBlueprint blueprint)) { return; }

        Totem totem = this.totemStore_.get(structure.id()).orElse(null);
        if (totem != null) {
            LOGGER.debug("Existing totem {} was been completed. Re-enabling", totem.id());
            totem.setEnabled(true);
            return;
        }

        Player placer = (e.getCauser() instanceof Player p) ? p : null;
        String name = (placer != null)
                ? this.runical_.translateOrDefault(
                        placer,
                        "totem.default_name.player_placed",
                        "{player}'s lands",
                        Placeholder.of("player", placer.getName())
                ) : this.runical_.translateOrDefault(
                        this.runical_.getDefaultLocale(),
                        "totem.default_name.undefined_placer",
                        "Unnamed region"
                );

        Vector shift = structure.origin().toDouble();
        BoundingBox proposedBounds = blueprint.initialRegionBounds().shift(shift);
        BoundingBox criticalBounds = toBoundingBox(structure.boundingBox());
        RegionPlaceResult placement = this.regionAllocator_.place(
                structure.world(),
                proposedBounds,
                criticalBounds,
                name,
                blueprint.hierarchy()
        );
        RegionPlaceResult.Placed placed = placement.getPlaced().orElse(null);
        if (placed == null) {
            List<Region> blockers = placement.getUnplaceable()
                    .map(RegionPlaceResult.Unplaceable::overlappingRegions)
                    .orElse(List.of());
            Collection<Long> blockerIds = blockers.stream().map(Region::getId).toList();
            LOGGER.warn("Totem structure {} could not place region '{}'. Blocking regions: {}", structure.id(), name, blockerIds);
            if (placer != null) {
                this.sendPlacementRejectedMessage(placer, blockers);
            }
            this.rejectPlacement(structure, blueprint);
            return;
        }

        Region region = placed.region();
        totem = new Totem(structure, region);
        TotemClaim claim = TotemClaim.of(totem);
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer != null) {
            dataContainer.remove(TOTEM_DATA_KEY);
            dataContainer.add(new RegionData(TOTEM_DATA_KEY, GSON.toJsonTree(claim)));
            region.save();
        }

        if (placer != null) {
            region.display(placer);
            this.runical_.sendTranslationOrDefault(
                    placer,
                    "totem.creation",
                    "Region created: '{region}'.",
                    Placeholder.of("region", name)
            );
        }

        this.loadTotem(totem);
    }

    @EventHandler
    void onCoreRightClicked(TotemCoreRightClickedEvent e) {
        ItemStack itemStack = e.getItemStack();
        if (itemStack == null || itemStack.getType() != Material.DIAMOND) {
            return;
        }

        e.setCancelled(true);

        Totem totem = this.totemStore_.getByCore(e.getCore().getWorld(), e.getCore().getCoordinates()).orElse(null);
        if (totem == null) {
            return;
        }

        TotemCore.Direction direction = e.getCore().getDirection();
        ExpansionResult result = this.regionAllocator_.expand(totem.region(), expansionFor(direction));
        Player player = e.getPlayer();
        if (!result.hasGrowth()) {
            this.sendExpansionBlockedMessage(player, direction, result);
            return;
        }

        Region region = totem.region();
        consumeOneItem(player, e.getHand(), itemStack);
        swingHand(player, e.getHand());
        region.display(player);
        region.save();
        this.sendExpansionFeedback(player, region, direction, result);
    }

    @EventHandler
    void onCoreDestroyed(TotemCoreBreakEvent e) {
        TotemCore c = e.getCore();
        Totem totem = this.totemStore_.getByCore(c.getWorld(), c.getCoordinates()).orElse(null);
        if (totem == null) { return; }

        totem.destroy();
        this.totemStore_.remove(totem);
        LOGGER.debug("Core of totem {} was destroyed. The totem has been destroyed", totem.id());
    }

    @EventHandler
    void onTotemChanged(StructureChangedEvent e) {
        if (e.getCurrentState() == Structure.State.COMPLETE) { return; }

        Structure structure = e.getStructure();
        if (!(structure.blueprint() instanceof TotemBlueprint blueprint)) { return; }

        Totem totem = this.totemStore_.get(structure.id()).orElse(null);
        if (totem == null) {
            LOGGER.warn("A totem structure update was triggered, but no loaded totem claims the structure. Dropping the structure");
            structure.drop();
            return;
        }

        Tile tile = e.getChangedPiece();
        boolean match = e.getPieceMatched();
        if (tile == null) {
            tile = blueprint.core();
            Vec3i coordinates = structure.origin().add(tile.offset());
            match = tile.matches(WorldTile.of(coordinates, structure.world()));
        }

        if (match) { return; }

        if (tile.piece() instanceof TotemCorePiece) {
            LOGGER.debug("Totem core of totem {} is no longer in the structure. The totem has been destroyed", totem.id());
            totem.destroy();
            this.totemStore_.remove(totem);
            return;
        }

        LOGGER.debug("Piece at offset {} of totem {} is missing. Disabling", tile.offset(), totem.id());
        totem.setEnabled(false);
    }

    @EventHandler
    void onSignEdited(SignChangeEvent e) {
        if (!(e.getBlock().getState() instanceof Sign sign)) { return; }

        ChunkKey ck = new ChunkKey(
                sign.getX() >> Constants.CHUNK_SHIFT,
                sign.getZ() >> Constants.CHUNK_SHIFT,
                e.getBlock().getWorld().getUID()
        );

        Totem totem = null;
        for (Totem t : this.totemStore_.getAroundChunk(ck)) {
            Sign candidate = t.nameSign().orElse(null);
            if (candidate == null) { continue; }
            if (sign.equals(candidate)) {
                totem = t;
                break;
            }
        }

        if (totem == null) { return; }

        String content = String.join(" ", e.getLines()).trim();
        RegionContextConfig conf = this.regionContext_.getConfig();
        int length = content.length();
        if (length < conf.minNameLength || length > conf.maxNameLength) {
            e.setCancelled(true);
            this.runical_.sendTranslationOrDefault(
                    e.getPlayer(),
                    "totem.rename.invalid_length",
                    "Name length must be {min}-{max} characters.",
                    Placeholder.of("min", Integer.toString(conf.minNameLength)),
                    Placeholder.of("max", Integer.toString(conf.maxNameLength))
            );
            return;
        }

        Region region = totem.region();
        String oldName = region.getName();
        region.setName(content);
        region.save();

        this.runical_.sendTranslationOrDefault(
                e.getPlayer(),
                "totem.rename.success",
                "Region renamed to '{new_name}'.",
                Placeholder.of("old_name", oldName),
                Placeholder.of("new_name", content)
        );
    }

    @EventHandler
    void onBlockPlaceAttempt(BlockRightClickedEvent e) {
        ItemStack item = e.getItem();
        Material type = item.getType();

        boolean isSign = Tag.SIGNS.isTagged(type),
                isLectern = type == Material.LECTERN;

        if (!(isSign || isLectern)) { return; }

        Block block = e.getBlock().getRelative(e.getBlockFace());
        ChunkKey ck = new ChunkKey(
                block.getX() >> Constants.CHUNK_SHIFT,
                block.getZ() >> Constants.CHUNK_SHIFT,
                block.getWorld().getUID()
        );
        List<Totem> totems = this.totemStore_.getAroundChunk(ck);
        if (totems.isEmpty()) { return; }
        Vec3i coordinates = new Vec3i(block.getX(), block.getY(), block.getZ());

        if (isLectern) {
            for (Totem totem : totems) {
                Lectern lectern = totem.lectern().orElse(null);
                if (lectern == null) { continue; }

                Vec3i offset = coordinates.subtract(totem.origin());
                for (Tile tile : totem.blueprint().lecterns()) {
                    if (offset.equals(tile.offset())) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            return;
        }

        for (Totem totem : totems) {
            Sign candidate = totem.nameSign().orElse(null);
            if (candidate == null) { continue; }

            Vec3i offset = coordinates.subtract(totem.origin());
            for (Tile tile : totem.blueprint().nameSings()) {
                if (offset.equals(tile.offset())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }


    //HELPERS
    private void loadTotem(Totem totem) {
        this.totemStore_.add(totem);
    }

    private void scheduleAudit(ChunkKey chunkKey) {
        if (!this.scheduledAudits_.add(chunkKey)) { return; }

        // Delay one tick so RegionLoadEvent/StructureLoadedEvent can be captured first.
        // RegionLoadEvent is deferred by RegionLib one tick after ChunkLoadEvent, so the delay is 2
        this.plugin_.getServer().getScheduler().runTaskLater(this.plugin_, () -> this.processChunk(chunkKey), 8);
    }

    private void processChunk(ChunkKey chunkKey) {
        this.scheduledAudits_.remove(chunkKey);

        World world = this.plugin_.getServer().getWorld(chunkKey.world());
        if (world == null || !world.isChunkLoaded(chunkKey.x(), chunkKey.z())) { return; }

        Chunk chunk = world.getChunkAt(chunkKey.x(), chunkKey.z());

        Map<UUID, PendingExpectation> expectations = this.pendingExpectationsByChunk_.remove(chunkKey);
        if (expectations != null) {
            for (PendingExpectation expectation : expectations.values()) {
                this.assembler_.expect(expectation.claim(), expectation.region());
            }
        }

        Map<UUID, Structure> ingestions = this.pendingIngestionsByChunk_.remove(chunkKey);
        if (ingestions != null) {
            for (Structure structure : ingestions.values()) {
                this.assembler_.ingest(structure);
            }
        }

        this.assembler_.audit(chunk);
    }

    private void enqueueExpectation(ChunkKey chunkKey, TotemClaim claim, Region region) {
        this.pendingExpectationsByChunk_.compute(chunkKey, (ign, pending) -> {
            Map<UUID, PendingExpectation> out = (pending == null) ? new LinkedHashMap<>() : pending;
            out.put(claim.structureId(), new PendingExpectation(claim, region));
            return out;
        });
    }

    private void enqueueIngestion(ChunkKey chunkKey, Structure structure) {
        this.pendingIngestionsByChunk_.compute(chunkKey, (ignored, pending) -> {
            Map<UUID, Structure> out = (pending == null) ? new LinkedHashMap<>() : pending;
            out.put(structure.id(), structure);
            return out;
        });
    }

    private void rejectPlacement(Structure structure, TotemBlueprint blueprint) {
        Vec3i coreCoordinates = structure.origin().add(blueprint.core().offset());
        TotemCore core = blueprint.core().piece().service().getCoreAt(structure.world().getUID(), coreCoordinates);
        if (core != null) {
            this.plugin_.getServer().getScheduler().runTaskLater(this.plugin_, () -> {
                core.setState(TotemCore.State.FULL);
                structure.drop();
            }, 1);
        }
    }

    private void sendPlacementRejectedMessage(Player player, Collection<Region> blockers) {
        String blockerNames = this.formatRegionList(player, blockers);
        if (!blockerNames.isBlank()) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.creation_failed.blocked",
                    "Cannot claim here: {blockers}. Move the totem.",
                    Placeholder.of("blockers", blockerNames)
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.creation_failed.generic",
                "Cannot claim here. Move the totem."
        );
    }

    private void sendExpansionFeedback(Player player, Region region, TotemCore.Direction direction, ExpansionResult result) {
        Placeholder[] placeholders = this.expansionPlaceholders(player, region, direction, result);
        if (this.isShifted(result)) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.expansion.shifted",
                    "Expanded {direction}; shifted around {blockers}. Size: H {height}, X {x}, Z {z}.",
                    placeholders
            );
            return;
        }

        if (result.unachievedTotal() > EPSILON) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.expansion.partial",
                    "Expanded {direction}; {blockers} blocked the rest. Size: H {height}, X {x}, Z {z}.",
                    placeholders
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.expansion.success",
                "Expanded {direction}. Size: H {height}, X {x}, Z {z}.",
                placeholders
        );
    }

    private void sendExpansionBlockedMessage(Player player, TotemCore.Direction direction, ExpansionResult result) {
        String blockerNames = this.formatRegionList(player, result.blockingRegions());
        if (!blockerNames.isBlank()) {
            this.runical_.sendTranslationOrDefault(
                    player,
                    "totem.expansion.blocked",
                    "Cannot expand {direction}: {blockers}.",
                    Placeholder.of("direction", this.translateDirection(player, direction)),
                    Placeholder.of("blockers", blockerNames)
            );
            return;
        }

        this.runical_.sendTranslationOrDefault(
                player,
                "totem.expansion.blocked_generic",
                "Cannot expand {direction}.",
                Placeholder.of("direction", this.translateDirection(player, direction))
        );
    }

    private Placeholder[] expansionPlaceholders(Player player, Region region, TotemCore.Direction direction, ExpansionResult result) {
        BoundingBox bounds = region.getBoundingBox();
        return new Placeholder[] {
                Placeholder.of("direction", this.translateDirection(player, direction)),
                Placeholder.of("height", formatMeasure(bounds.getHeight())),
                Placeholder.of("x", formatMeasure(bounds.getWidthX())),
                Placeholder.of("z", formatMeasure(bounds.getWidthZ())),
                Placeholder.of("blockers", this.formatRegionList(player, result.blockingRegions()))
        };
    }

    private String translateDirection(Player player, TotemCore.Direction direction) {
        String key = "totem.expansion.direction." + direction.name().toLowerCase();
        String fallback = switch (direction) {
            case ALL -> "in all directions";
            case UP -> "upward";
            case DOWN -> "downward";
            case NORTH -> "to the north";
            case SOUTH -> "to the south";
            case EAST -> "to the east";
            case WEST -> "to the west";
        };

        if (player != null) {
            return this.runical_.translateOrDefault(player, key, fallback);
        }
        return this.runical_.translateOrDefault(this.runical_.getDefaultLocale(), key, fallback);
    }

    private String formatRegionList(Player player, Collection<Region> regions) {
        if (regions == null || regions.isEmpty()) {
            return "";
        }

        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Region region : regions) {
            names.add(this.regionName(player, region));
        }

        if (player != null) {
            return this.runical_.formatList(player, names);
        }
        return this.runical_.formatList(this.runical_.getDefaultLocale(), names);
    }

    private String regionName(Player player, Region region) {
        String name = region.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }

        String id = (region.getId() == null) ? "?" : region.getId().toString();
        if (player != null) {
            return this.runical_.translateOrDefault(
                    player,
                    "totem.region.unnamed",
                    "Region #{id}",
                    Placeholder.of("id", id)
            );
        }
        return this.runical_.translateOrDefault(
                this.runical_.getDefaultLocale(),
                "totem.region.unnamed",
                "Region #{id}",
                Placeholder.of("id", id)
        );
    }

    private boolean isShifted(ExpansionResult result) {
        if (result.unachievedTotal() > EPSILON) {
            return false;
        }

        Expansion intended = result.intended();
        Expansion accomplished = result.accomplished();
        return !same(intended.up(), accomplished.up())
                || !same(intended.down(), accomplished.down())
                || !same(intended.north(), accomplished.north())
                || !same(intended.south(), accomplished.south())
                || !same(intended.east(), accomplished.east())
                || !same(intended.west(), accomplished.west());
    }

    private static String formatMeasure(double value) {
        double rounded = Math.rint(value);
        if (same(value, rounded)) {
            return Long.toString(Math.round(rounded));
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static TotemClaim readClaim(Region region) {
        RegionDataContainer dataContainer = region.getDataContainer();
        if (dataContainer == null || !dataContainer.has(TOTEM_DATA_KEY)) {
            return null;
        }

        RegionData raw = dataContainer.get(TOTEM_DATA_KEY);
        if (raw == null) {
            return null;
        }

        JsonElement value = raw.getValue();
        if (value == null || value.isJsonNull()) {
            return null;
        }

        try {
            TotemClaim claim = GSON.fromJson(value, TotemClaim.class);
            if (claim != null) { return claim; }
        } catch (Exception ignored) {}

        LOGGER.warn(
                "Region {} is totemized, but its totemData entry is corrupted or invalid. Destroying\nEntry: '{}'",
                region.getId(),
                value
        );
        region.destroy();
        return null;
    }

    private static Expansion expansionFor(TotemCore.Direction direction) {
        if (direction == TotemCore.Direction.ALL) {
            return Expansion.all(REGION_GROWTH_RATE / 6d);
        }
        return Expansion.forDirection(direction, REGION_GROWTH_RATE);
    }

    private static BoundingBox toBoundingBox(IntBoundingBox boundingBox) {
        return new BoundingBox(
                boundingBox.minX(),
                boundingBox.minY(),
                boundingBox.minZ(),
                boundingBox.maxX() + 1d,
                boundingBox.maxY() + 1d,
                boundingBox.maxZ() + 1d
        );
    }

    private static void consumeOneItem(Player player, EquipmentSlot hand, ItemStack stack) {
        if (stack.getAmount() < 2) {
            stack = new ItemStack(Material.AIR);
        } else {
            stack.setAmount(stack.getAmount() - 1);
        }
        player.getInventory().setItem(hand, stack);
    }

    private static void swingHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.swingOffHand();
        } else {
            player.swingMainHand();
        }
    }

    private static boolean same(double a, double b) {
        return Math.abs(a - b) <= EPSILON;
    }


    //SUBTYPES
    private record PendingExpectation(TotemClaim claim, Region region) {}
}
