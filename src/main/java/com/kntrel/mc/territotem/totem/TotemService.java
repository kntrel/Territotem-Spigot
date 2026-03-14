package com.kntrel.mc.territotem.totem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.event.RegionLoadEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.repository.Condition;
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
import org.bukkit.World;
import org.bukkit.block.Block;
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

import java.util.Collection;
import java.util.LinkedHashMap;
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
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TotemClaim.class, TotemClaim.serializer())
            .registerTypeAdapter(TotemClaim.class, TotemClaim.deserializer())
            .create();


    //FIELDS
    private final RegionContext regionContext_;
    private final Plugin plugin_;
    private final TotemAssembler assembler_;
    private final RegionAllocator regionAllocator_;
    private final Map<ChunkKey, Map<UUID, PendingExpectation>> pendingExpectationsByChunk_;
    private final Map<ChunkKey, Map<UUID, Structure>> pendingIngestionsByChunk_;
    private final Set<ChunkKey> scheduledAudits_;
    private final TotemStore totemStore_;


    //CONSTRUCTOR
    public TotemService(RegionContext regionContext) {
        this.regionContext_ = regionContext;
        this.plugin_ = this.regionContext_.getPlugin();
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
        return this.totemStore_.getAtChunk(x, z, world);
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

        Vector shift = structure.origin().toDouble();
        String name = (e.getCauser() == null || !(e.getCauser() instanceof Player p))
                ? "Unnamed region"
                : p.getName() + "'s region";

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
            Collection<Long> blockers = placement.getUnplaceable()
                    .map(RegionPlaceResult.Unplaceable::overlappingRegions)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(Region::getId)
                    .toList();
            LOGGER.warn("Totem structure {} could not place region '{}'. Blocking regions: {}", structure.id(), name, blockers);
            this.rejectPlacement(structure, blueprint);
            return;
        }

        Region region = placed.region();
        totem = new Totem(e.getStructure(), region);
        TotemClaim claim = TotemClaim.of(totem);
        var dataContainer = region.getDataContainer();
        if (dataContainer != null) {
            dataContainer.remove(TOTEM_DATA_KEY);
            dataContainer.add(new RegionData(TOTEM_DATA_KEY, GSON.toJsonTree(claim)));
            region.save();
        }

        if (e.getCauser() instanceof Player p) {
            region.display(p);
            p.sendMessage("Created " + name);
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

        Expansion expansion = expansionFor(e.getCore().getDirection());
        ExpansionResult result = this.regionAllocator_.expand(totem.region(), expansion);
        if (!result.hasGrowth()) {
            return;
        }

        Player player = e.getPlayer();
        Region region = totem.region();
        consumeOneItem(player, e.getHand(), itemStack);
        swingHand(e.getPlayer(), e.getHand());
        region.display(player);
        region.save();
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
            Vec3i coordinates = e.getStructure().origin().add(tile.offset());
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
        for (Totem t : this.totemStore_.getAtChunk(ck)) {
            Sign s = t.nameSign().orElse(null);
            if (s == null) { continue; }
            if (sign.equals(s)) {
                totem = t;
                break;
            }
        }

        if (totem == null) { return; }

        String content = String.join(" ", e.getLines()).trim();

        int len = content.length();
        RegionContextConfig conf = this.regionContext_.getConfig();
        if (len < conf.minNameLength || len > conf.maxNameLength) {
            e.setCancelled(true);
            return;
        }

        Region region = totem.region();
        String oldName = region.getName();
        region.setName(content);
        region.save();

        e.getPlayer().sendMessage(oldName + "'s name has bee changed to '" + content + "'");
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


    //SUBTYPES
    private record PendingExpectation(TotemClaim claim, Region region) {}
}
