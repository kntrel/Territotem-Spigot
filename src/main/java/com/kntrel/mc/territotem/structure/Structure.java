package com.kntrel.mc.territotem.structure;

import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.util.Vec3i;

public class Structure {

    //FIELDS
    private final StructureService service_;
    private final Blueprint blueprint_;
    private final Vec3i origin_;


    //CONSTRUCTOR
    public Structure(StructureService service, Blueprint blueprint, Vec3i origin) {
        this.service_ = service;
        this.blueprint_ = blueprint;
        this.origin_ = origin;
    }
}
