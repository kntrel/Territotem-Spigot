package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.structure.blueprint.InvalidBlueprintException;
import com.kntrel.util.Vec3i;
import org.bukkit.World;

public class Totem {

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
        this.region_.destroy();
        this.region_.save();
    }
}
