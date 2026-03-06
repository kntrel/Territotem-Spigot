package com.kntrel.mc.territotem.structure.event;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class StructureUnloadedEvent extends StructureEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //CONSTRUCTOR
    public StructureUnloadedEvent(Structure structure) {
        super(structure);
    }
}
