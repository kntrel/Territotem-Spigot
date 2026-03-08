package com.kntrel.mc.territotem.totem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.event.RegionLoadEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.event.StructureCompletedEvent;
import com.kntrel.mc.territotem.structure.event.StructureLoadedEvent;
import com.kntrel.mc.territotem.util.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TotemService implements Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemService.class);
    private static final String TOTEM_DATA_KEY = "totemData";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TotemClaim.class, TotemClaim.serializer())
            .registerTypeAdapter(TotemClaim.class, TotemClaim.deserializer())
            .create();


    //FIELDS
    private final RegionContext regionContext_;
    private final Plugin plugin_;
    private final TotemAssembler assembler_;
    private final Map<UUID, Totem> totemsByStructure_;
    private final Map<ChunkKey, Map<UUID, PendingExpectation>> pendingExpectationsByChunk_;
    private final Map<ChunkKey, Map<UUID, Structure>> pendingIngestionsByChunk_;
    private final Set<ChunkKey> scheduledAudits_;


    //CONSTRUCTOR
    public TotemService(RegionContext regionContext) {
        this.regionContext_ = regionContext;
        this.plugin_ = this.regionContext_.getPlugin();
        this.assembler_ = new TotemAssembler();
        this.totemsByStructure_ = new ConcurrentHashMap<>();
        this.pendingExpectationsByChunk_ = new ConcurrentHashMap<>();
        this.pendingIngestionsByChunk_ = new ConcurrentHashMap<>();
        this.scheduledAudits_ = ConcurrentHashMap.newKeySet();

        this.assembler_.consume(totem -> this.totemsByStructure_.put(totem.structure().id(), totem));
        this.plugin_.getServer().getPluginManager().registerEvents(this, this.plugin_);
    }


    //API
    public Map<UUID, Totem> totemsByStructure() {
        return Collections.unmodifiableMap(this.totemsByStructure_);
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
    @EventHandler void onTotemCompletedEvent(StructureCompletedEvent e) {
        if (!(e.getStructure().blueprint() instanceof TotemBlueprint blueprint)) { return; }

        Vector shift = e.getStructure().origin().toDouble();
        Region region = this.regionContext_.create(
                e.getCauser(),
                blueprint.initialRegionBounds().shift(shift),
                e.getStructure().world(),
                "totem_region",
                blueprint.hierarchy()
        );
        Totem totem = new Totem(e.getStructure(), region);
        TotemClaim claim = TotemClaim.of(totem);
        var dataContainer = region.getDataContainer();
        if (dataContainer != null) {
            dataContainer.remove(TOTEM_DATA_KEY);
            dataContainer.add(new RegionData(TOTEM_DATA_KEY, GSON.toJsonTree(claim)));
            region.save();
        }

        if (e.getCauser() instanceof Player p) {
            region.display(p);
            p.sendMessage("region created");
        }
    }


    //HELPERS
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


    //SUBTYPES
    private record PendingExpectation(TotemClaim claim, Region region) {}
}

