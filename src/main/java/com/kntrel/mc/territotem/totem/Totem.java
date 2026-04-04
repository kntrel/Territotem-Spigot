package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.blueprint.InvalidBlueprintException;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.piece.TotemLecternPiece;
import com.kntrel.mc.territotem.totem.piece.TotemNameSignPiece;
import com.kntrel.mc.territotem.totem.piece.TotemTile;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.mc.territotem.util.ItemStackInfo;
import com.kntrel.util.Vec3i;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.util.BoundingBox;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Totem {

    //CONSTANTS
    public static final long
            AMBIENT_SOUND_RATE  = 80L;
    public static final Sound
            AMBIENT_SOUND       = Sound.BLOCK_BEACON_AMBIENT,
            FEED_SOUND          = Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
            HIT_SOUND           = Sound.ENTITY_BLAZE_HURT;


    //FIELDS
    private final TotemService provenance_;
    private final Structure structure_;
    private final Region region_;
    private final TotemCore core_;
    private final Deque<TotemGrowthEntry> growthHistory_;
    private BoundingBox baseBounds_;
    private int deedsVersion_;


    //CONSTRUCTORS
    Totem(TotemService provenance, Structure structure, Region region, TotemCore core) {
        if (!(structure.blueprint() instanceof TotemBlueprint)) {
            throw new InvalidBlueprintException(
                "A totem must be backed by a totem blueprint. Passed structure's blueprint member is not an instance of TotemBlueprint"
            );
        }
        this.provenance_ = provenance;
        this.structure_ = structure;
        this.region_ = region;
        this.core_ = core;
        this.growthHistory_ = new ArrayDeque<>();
        this.baseBounds_ = region.getBoundingBox();
        this.deedsVersion_ = 0;
    }


    //API
    public Structure structure() {
        return this.structure_;
    }
    public UUID id() {
        return this.structure_.id();
    }
    public Vec3i origin() {
        return this.structure_.origin();
    }
    public TotemBlueprint blueprint() {
        return (TotemBlueprint) this.structure_.blueprint();
    }
    public Region region() {
        return this.region_;
    }
    public TotemCore core() {
        return this.core_;
    }
    public BoundingBox baseBounds() {
        return this.baseBounds_.clone();
    }
    public List<TotemGrowthEntry> growthHistory() {
        return List.copyOf(this.growthHistory_);
    }
    public ExpansionResult expand(Expansion expansion, ItemStackInfo refundStack, double dropBackRate) {
        return this.provenance_.expand(this, expansion, refundStack, dropBackRate);
    }
    public World world() {
        return this.structure_.world();
    }
    public int getDeedsVersion() {
        return this.deedsVersion_;
    }
    public void setDeedsVersion(int newVersion) {
        this.deedsVersion_ = newVersion;
    }
    public int incrementAndGetDeedsVersion() {
        return ++this.deedsVersion_;
    }
    public void loadGrowthState(BoundingBox baseBounds, Iterable<TotemGrowthEntry> history) {
        this.baseBounds_ = baseBounds.clone();
        this.growthHistory_.clear();
        if (history == null) {
            return;
        }
        for (TotemGrowthEntry growth : history) {
            if (growth == null || growth.expansion().isZero()) {
                continue;
            }
            this.growthHistory_.addLast(growth);
        }
    }
    public void recordGrowth(TotemGrowthEntry growth) {
        if (growth == null || growth.expansion().isZero()) {
            return;
        }
        this.growthHistory_.addLast(growth);
    }
    public TotemGrowthEntry latestGrowth() {
        return this.growthHistory_.peekLast();
    }
    public TotemGrowthEntry discardLatestGrowth() {
        return this.growthHistory_.pollLast();
    }
    public Optional<Sign> nameSign() {
        World world = this.world();
        for (TotemTile<TotemNameSignPiece> sign : this.blueprint().nameSings()) {
            Vec3i cords = sign.offset().add(this.origin());
            WorldTile worldTile = WorldTile.of(cords, world);
            if (!sign.piece().isPlaced(worldTile)) { continue; }
            Block b = this.world().getBlockAt(cords.x(), cords.y(), cords.z());
            if (b.getState() instanceof Sign s) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
    public Optional<Lectern> lectern() {
        World world = this.world();
        for (TotemTile<TotemLecternPiece> lectern : this.blueprint().lecterns()) {
            Vec3i cords = lectern.offset().add(this.origin());
            WorldTile worldTile = WorldTile.of(cords, world);
            if (!lectern.piece().isPlaced(worldTile)) { continue; }
            Block b = this.world().getBlockAt(cords.x(), cords.y(), cords.z());
            if (b.getState() instanceof Lectern l) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }
    public boolean isEnabled() {
        return this.region_.isEnabled();
    }
    public void setEnabled(boolean enabled) {
        if (enabled == this.isEnabled()) { return; }
        this.core().setState(enabled ? TotemCore.State.ACTIVE : TotemCore.State.INACTIVE);
        this.region_.enabled(enabled);
        this.region_.save();
    }
    public boolean isDestroyed() {
        return this.region_.isDestroyed();
    }
    public void rename(String newName) {
        this.region_.setName(newName);
        this.region_.save();
    }
    public void save() {
        TotemClaim claim = TotemClaim.of(this);
        TotemClaim.write(this.region(), claim);
        TotemGrowthHistory.write(this.region(), TotemGrowthHistory.of(this));
        this.region().save();
    }
    public void destroy() {
        this.structure_.drop();
        this.region_.destroy();
        this.region_.save();
    }
}
