package com.kntrel.mc.territotem.structure.event;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class StructureUncompletedEvent extends StructureStateChangedEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //CONSTRUCTOR
    public StructureUncompletedEvent(Structure structure, @Nullable Entity uncompleter, Structure.State currentState) {
        super(structure, uncompleter, Structure.State.COMPLETE, currentState);
    }
}
