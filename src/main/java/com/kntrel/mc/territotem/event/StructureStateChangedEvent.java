package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

public abstract class StructureStateChangedEvent extends StructureEvent {

    //FIELDS
    private final Structure.State previousState_, currentState_;
    private final Entity causer_;


    //CONSTRUCTOR
    protected StructureStateChangedEvent(Structure structure, @Nullable Entity causer, Structure.State previous, Structure.State current) {
        super(structure);
        this.previousState_ = previous;
        this.currentState_ = current;
        this.causer_ = causer;
    }


    //GETTERS
    public Structure.State getPreviousState() { return this.previousState_; }
    public Structure.State getCurrentState() { return this.currentState_; }
    public @Nullable Entity getCauser() { return this.causer_; }
}
