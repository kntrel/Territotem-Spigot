package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class StructureCompletedEvent extends StructureStateChangedEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //CONSTRUCTOR
    public StructureCompletedEvent(Structure structure, @Nullable Entity completer, Structure.State previousState) {
        super(structure, completer, previousState, Structure.State.COMPLETE);
    }
}
