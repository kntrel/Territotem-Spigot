package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.util.Vec3i;
import org.bukkit.World;

public class Totem {

    //FIELDS
    private final Blueprint blueprint_;
    private final Vec3i origin_;
    private final Region region_;


    //CONSTRUCTORS
    Totem(Blueprint blueprint, Vec3i origin, Region region) {
        this.blueprint_ = blueprint;
        this.origin_ = origin;
        this.region_ = region;
    }


    //API
    public Blueprint blueprint() {
        return this.blueprint_;
    }
    public Vec3i origin() {
        return this.origin_;
    }
    public Region region() {
        return this.region_;
    }
    public World world() {
        return this.region_.getWorld();
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
