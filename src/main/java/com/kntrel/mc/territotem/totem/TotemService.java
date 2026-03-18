package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.piece.TotemCorePiece;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.mc.territotem.totem.region.RegionAllocator;
import com.kntrel.mc.territotem.totem.region.RegionPlaceResult;
import com.kntrel.mc.territotem.util.ChunkKey;
import com.kntrel.util.IntBoundingBox;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TotemService {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(TotemService.class);
    private static final String TOTEM_DATA_KEY = TotemClaim.DATA_KEY;


    //FIELDS
    private final Plugin plugin_;
    private final TotemAssembler assembler_;
    private final RegionAllocator regionAllocator_;
    private final TotemStore totemStore_;


    //CONSTRUCTOR
    public TotemService(RegionContext regionContext, Runical runical) {
        this.plugin_ = regionContext.getPlugin();
        this.assembler_ = new TotemAssembler(this.plugin_);
        this.regionAllocator_ = new RegionAllocator(regionContext, Condition.hasDataKey(TOTEM_DATA_KEY));
        this.totemStore_ = new TotemStore();

        this.assembler_.consume((s, r, c) -> this.loadTotem(s, r, c.deedsVersion()));
        this.plugin_.getServer().getPluginManager().registerEvents(
                new TotemServiceListener(this, regionContext, runical),
                this.plugin_
        );
    }


    //API
    public List<Totem> totemsAtChunk(int x, int z, World world) {
        return this.totemStore_.getAroundChunk(x, z, world);
    }

    public Optional<Totem> totemOfRegion(Region region) {
        return this.totemStore_.getFromRegion(region);
    }

    public Optional<Totem> totemOfCore(TotemCore core) {
        return this.totemStore_.getByCore(core.getWorld(), core.getCoordinates());
    }

    public Optional<Totem> totemAtSign(Sign sign) {
        ChunkKey chunkKey = new ChunkKey(
                sign.getX() >> Constants.CHUNK_SHIFT,
                sign.getZ() >> Constants.CHUNK_SHIFT,
                sign.getWorld().getUID()
        );

        for (Totem totem : this.totemStore_.getAroundChunk(chunkKey)) {
            Sign candidate = totem.nameSign().orElse(null);
            if (sign.equals(candidate)) {
                return Optional.of(totem);
            }
        }
        return Optional.empty();
    }

    public NewTotemResult newTotem(Structure structure, String name) {
        if (!(structure.blueprint() instanceof TotemBlueprint blueprint)) {
            return NewTotemResult.ignored();
        }

        Totem totem = this.totemStore_.get(structure.id()).orElse(null);
        if (totem != null) {
            LOGGER.debug("Existing totem {} was been completed. Re-enabling", totem.id());
            totem.setEnabled(true);
            return NewTotemResult.reenabled(totem);
        }

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
            LOGGER.warn(
                    "Totem structure {} could not place region '{}'. Blocking regions: {}",
                    structure.id(),
                    name,
                    blockerIds
            );
            this.rejectPlacement(structure, blueprint);
            return NewTotemResult.blocked(blockers);
        }

        Region region = placed.region();
        totem = this.loadTotem(structure, region, 0);
        TotemClaim claim = TotemClaim.of(totem);
        TotemClaim.write(region, claim);
        return NewTotemResult.created(totem);
    }

    public void destroyTotem(Totem totem) {
        totem.destroy();
        this.totemStore_.remove(totem);
        LOGGER.debug("Core of totem {} was destroyed. The totem has been destroyed", totem.id());
    }

    public void updateTotemStructure(Structure structure, Tile tile, boolean match) {
        if (!(structure.blueprint() instanceof TotemBlueprint blueprint)) {
            return;
        }

        Totem totem = this.totemStore_.get(structure.id()).orElse(null);
        if (totem == null) {
            LOGGER.warn("A totem structure update was triggered, but no loaded totem claims the structure. Dropping the structure");
            structure.drop();
            return;
        }

        Tile changedTile = tile;
        boolean tileMatches = match;
        if (changedTile == null) {
            changedTile = blueprint.core();
            Vec3i coordinates = structure.origin().add(changedTile.offset());
            tileMatches = changedTile.matches(WorldTile.of(coordinates, structure.world()));
        }

        if (tileMatches) {
            return;
        }

        if (changedTile.piece() instanceof TotemCorePiece) {
            LOGGER.debug("Totem core of totem {} is no longer in the structure. The totem has been destroyed", totem.id());
            this.destroyTotem(totem);
            return;
        }

        LOGGER.debug("Piece at offset {} of totem {} is missing. Disabling", changedTile.offset(), totem.id());
        totem.setEnabled(false);
    }

    ExpansionResult expand(Totem totem, Expansion expansion) {
        ExpansionResult result = this.regionAllocator_.expand(totem.region(), expansion);
        if (result.hasGrowth()) {
            totem.region().save();
        }
        return result;
    }


    //HELPERS
    private Totem loadTotem(Structure structure, Region region, int deedsVersion) {
        Totem totem = new Totem(this, structure, region);
        totem.setDeedsVersion(deedsVersion);
        this.totemStore_.add(totem);
        return totem;
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


    //SUBTYPES
    record NewTotemResult(Status status, Totem totem, List<Region> blockers) {

        NewTotemResult {
            blockers = (blockers == null) ? List.of() : List.copyOf(blockers);
        }

        static NewTotemResult ignored() {
            return new NewTotemResult(Status.IGNORED, null, List.of());
        }

        static NewTotemResult reenabled(Totem totem) {
            return new NewTotemResult(Status.REENABLED, totem, List.of());
        }

        static NewTotemResult created(Totem totem) {
            return new NewTotemResult(Status.CREATED, totem, List.of());
        }

        static NewTotemResult blocked(List<Region> blockers) {
            return new NewTotemResult(Status.BLOCKED, null, blockers);
        }

        enum Status {
            IGNORED,
            REENABLED,
            CREATED,
            BLOCKED
        }
    }
}
