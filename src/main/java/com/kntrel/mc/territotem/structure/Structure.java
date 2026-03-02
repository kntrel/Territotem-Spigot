package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.util.Vec3i;
import org.bukkit.World;

public class Structure {

    //FIELDS
    private final StructureService service_;
    private final Blueprint blueprint_;
    private final Vec3i origin_;
    private final World world_;


    //CONSTRUCTOR
    public Structure(StructureService service, Blueprint blueprint, World world, Vec3i origin) {
        this.service_ = service;
        this.blueprint_ = blueprint;
        this.origin_ = origin;
        this.world_ = world;
    }


    //GETTERS
    public Blueprint blueprint() {
        return this.blueprint_;
    }
    public Vec3i origin() {
        return this.origin_;
    }
    public World world() {
        return this.world_;
    }
}
