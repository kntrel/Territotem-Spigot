package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.territotem.blueprint.Blueprint;
import com.kntrel.util.Vec3i;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Totem {

    //FIELDS
    private final Blueprint blueprint_;
    private final Vec3i origin_;
    private final World world_;


    //CONSTRUCTORS
    public Totem(Blueprint blueprint, Vec3i origin, World world) {
        this.blueprint_ = blueprint;
        this.origin_ = origin;
        this.world_ = world;
    }


    //API
    public Blueprint blueprint() {
        return this.blueprint_;
    }
    public Vec3i origin() {
        return this.origin_;
    }
    public World world() {
        return this.world_;
    }
    public Vector regionOrigin() {
        return this.blueprint_.regionOrigin().add(this.origin_.toDouble());
    }

}
