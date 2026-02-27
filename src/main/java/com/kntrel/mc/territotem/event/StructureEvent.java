package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.event.Event;

public abstract class StructureEvent extends Event {

    //FIELDS
    private final Structure structure_;


    //CONSTRUCTOR
    protected StructureEvent(Structure structure) {
        this.structure_ = structure;
    }


    //GETTERS
    public Structure getStructure() {
        return this.structure_;
    }
}
