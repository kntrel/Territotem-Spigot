package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.event.RegionLoadEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.event.StructureLoadedEvent;
import com.kntrel.mc.territotem.util.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class TottemAssenblyListener implements Listener {

    private final Plugin plugin_;
    private final TotemAssembler assembler_;
    private final Map<ChunkKey, Map<UUID, PendingExpectation>> pendingExpectationsByChunk_;
    private final Map<ChunkKey, Map<UUID, Structure>> pendingIngestionsByChunk_;
    private final Set<ChunkKey> scheduledAudits_;

    TottemAssenblyListener(Plugin plugin, TotemAssembler assembler) {
        this.plugin_ = plugin;
        this.assembler_ = assembler;
        this.pendingExpectationsByChunk_ = new ConcurrentHashMap<>();
        this.pendingIngestionsByChunk_ = new ConcurrentHashMap<>();
        this.scheduledAudits_ = ConcurrentHashMap.newKeySet();
    }

    @EventHandler
    void onRegionLoad(RegionLoadEvent e) {
        TotemClaim claim = TotemClaim.read(e.getRegion());
        if (claim == null) { return; }

        ChunkKey chunkKey = new ChunkKey(claim.chunkX(), claim.chunkZ(), e.getRegion().getWorld());
        this.enqueueExpectation(chunkKey, claim, e.getRegion());
        this.scheduleAudit(chunkKey);
    }

    @EventHandler
    void onStructureLoad(StructureLoadedEvent e) {
        Structure structure = e.getStructure();
        if (!(structure.blueprint() instanceof TotemBlueprint)) { return; }

        ChunkKey chunkKey = ChunkKey.ofBlock(structure.origin(), structure.world().getUID());
        this.enqueueIngestion(chunkKey, structure);
        this.scheduleAudit(chunkKey);
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent e) {
        this.scheduleAudit(new ChunkKey(e.getChunk()));
    }

    private void scheduleAudit(ChunkKey chunkKey) {
        if (!this.scheduledAudits_.add(chunkKey)) {
            return;
        }

        // Delay so RegionLoadEvent/StructureLoadedEvent can be captured before auditing.
        this.plugin_.getServer().getScheduler().runTaskLater(this.plugin_, () -> this.processChunk(chunkKey), 8);
    }

    private void processChunk(ChunkKey chunkKey) {
        this.scheduledAudits_.remove(chunkKey);

        World world = this.plugin_.getServer().getWorld(chunkKey.world());
        if (world == null || !world.isChunkLoaded(chunkKey.x(), chunkKey.z())) {
            return;
        }

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
        this.pendingExpectationsByChunk_.compute(chunkKey, (ignored, pending) -> {
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

    private record PendingExpectation(TotemClaim claim, Region region) {}
}
