package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.blueprint.InvalidBlueprintException;
import com.kntrel.mc.territotem.structure.worldTile.WorldTile;
import com.kntrel.mc.territotem.totem.piece.TotemLecternPiece;
import com.kntrel.mc.territotem.totem.piece.TotemNameSignPiece;
import com.kntrel.mc.territotem.totem.piece.TotemTile;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class Totem {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Totem.class);

    //FIELDS
    private final Structure structure_;
    private final Region region_;


    //CONSTRUCTORS
    Totem(Structure structure, Region region) {
        if (!(structure.blueprint() instanceof TotemBlueprint)) {
            throw new InvalidBlueprintException(
                "A totem must be backed by a totem blueprint. Passed structure's blueprint member is not an instance of TotemBlueprint"
            );
        }
        this.structure_ = structure;
        this.region_ = region;
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
    public World world() {
        return this.structure_.world();
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
        this.region_.enabled(enabled);
        this.region_.save();
    }
    public boolean isDestroyed() {
        return this.region_.isDestroyed();
    }
    public void destroy() {
        this.structure_.drop();
        this.region_.destroy();
        this.region_.save();
    }
}
