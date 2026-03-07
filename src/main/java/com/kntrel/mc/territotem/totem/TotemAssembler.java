package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.util.ChunkKey;
import org.bukkit.Chunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class TotemAssembler {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemAssembler.class);


    //FIELDS
    private final Map<ChunkKey, Set<PendingAudit>> pendingAuditsByChunk_;
    private final Map<UUID, PendingAudit> pendingAuditsByStructure_;
    private final Set<UUID> verifiedClaims_;
    private final List<Consumer<Totem>> consumers_;


    //CONSTRUCTOR
    public TotemAssembler() {
        this.pendingAuditsByChunk_ = new ConcurrentHashMap<>();
        this.pendingAuditsByStructure_ = new ConcurrentHashMap<>();
        this.verifiedClaims_ = ConcurrentHashMap.newKeySet();
        this.consumers_ = new ArrayList<>();
    }


    //API
    public void expect(TotemClaim claim, Region region) {
        if (claim == null || region == null || region.isDestroyed()) { return; }

        UUID structureId = claim.structureId();
        if (this.verifiedClaims_.contains(structureId)) {
            LOGGER.debug("Totem {} is already verified on this boot. Audit waved.", structureId);
            return;
        }

        LOGGER.debug("Region {} claims to be backed by totem {} at chunk ({}, {})", region.getId(), structureId, claim.chunkX(), claim.chunkZ());

        final PendingAudit audit = new PendingAudit(claim, region);
        boolean alreadyQueued = this.pendingAuditsByStructure_.putIfAbsent(structureId, audit) != null;
        if (alreadyQueued) {
            LOGGER.trace("Verification of structure {} for region {} is already enqueued", structureId, region.getId());
            return;
        }

        ChunkKey chunk = new ChunkKey(claim.chunkX(), claim.chunkZ(), region.getWorld());
        this.pendingAuditsByChunk_.compute(chunk, (ignored, pending) -> {
            Set<PendingAudit> audits = (pending == null) ? new HashSet<>() : pending;
            audits.add(audit);
            return audits;
        });
    }
    public void ingest(Structure structure) {
        PendingAudit audit = this.pendingAuditsByStructure_.remove(structure.id());
        if (audit == null) {
            if (structure.blueprint() instanceof TotemBlueprint b) {
                LOGGER.warn(
                          "[Inconsistency] A totem structure of blueprint {} was present at {}, "
                        + "but either no region claims it or its region is out of reach. "
                        + "Dropping the structure.",
                        b.name(),
                        structure.origin()
                );
                structure.drop();
            }
            return;
        }

        TotemClaim claim = audit.claim;
        Region region = audit.region;
        this.pendingAuditsByChunk_.compute(new ChunkKey(claim.chunkX(), claim.chunkZ(), region.getWorld()), (ign, set) -> {
            if (set == null) { return null; }
            set.remove(audit);
            return set.isEmpty() ? null : set;
        });

        if (this.verify(claim, structure, region)) {
            this.release(new Totem(structure, region));
        }
    }
    public void audit(Chunk chunk) {
        ChunkKey chunkKey = new ChunkKey(chunk);

        Set<PendingAudit> pending = this.pendingAuditsByChunk_.get(chunkKey);
        if (pending == null || pending.isEmpty()) {
            LOGGER.trace("Loaded chunk ({}, {}) has no orphan claimed totems", chunkKey.x(), chunkKey.z());
            return;
        }

        for (PendingAudit audit : pending) {
            this.pendingAuditsByStructure_.remove(audit.claim().structureId());
            this.verify(audit.claim, null, audit.region);
        }
    }
    public void consume(Consumer<Totem> consumer) {
        this.consumers_.add(consumer);
    }


    //HELPERS
    private void release(Totem totem) {
        for (Consumer<Totem> c : this.consumers_) { c.accept(totem); }
    }
    private boolean verify(TotemClaim claim, @Nullable Structure structure, Region region) {

        if (structure == null || !isClaimChunkMatch(claim, structure, region)) {
            LOGGER.warn(
                    "[Inconsistency] Region {} claims it's backed by totem {} at chunk ({}, {}), but the structure want's found. Destroying region",
                    region.getId(),
                    claim.structureId(),
                    claim.chunkX(),
                    claim.chunkZ()
            );
            destroy(region);
            return false;
        }

        LOGGER.debug("Verified totem {} backing region {}", claim.structureId(), region.getId());
        this.verifiedClaims_.add(structure.id());

        boolean structureComplete = structure.isComplete();
        boolean regionEnabled = region.isEnabled();
        if (structureComplete == regionEnabled) { return true; }

        LOGGER.warn(
                "[Inconsistency] Completion mismatch for totem claim {} in region {}. structureComplete={}, regionEnabled={}. Fixing region state.",
                claim,
                region.getId(),
                structureComplete,
                regionEnabled
        );
        region.enabled(structureComplete);
        region.save();
        return true;
    }

    private static boolean isClaimChunkMatch(TotemClaim claim, Structure structure, Region region) {
        if (!structure.world().getUID().equals(region.getWorld().getUID())) {
            return false;
        }

        ChunkKey structureChunk = ChunkKey.ofBlock(structure.origin(), structure.world().getUID());
        return structureChunk.x() == claim.chunkX() && structureChunk.z() == claim.chunkZ();
    }

    private static void destroy(Region region) {
        if (region.isDestroyed()) {
            return;
        }

        region.destroy();
        region.save();
    }


    //SUBTYPES
    private record PendingAudit(TotemClaim claim, Region region) {}
}

