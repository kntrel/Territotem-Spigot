package com.kntrel.mc.territotem.event;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class StructureDestroyedEvent extends StructureEvent {

    //EVENT-REQUIRED ================================================
    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() { return HANDLERS; }
    @Override @NonNull
    public HandlerList getHandlers() { return HANDLERS; }
    //===============================================================


    //FIELDS
    private final Entity causer_;


    //CONSTRUCTOR
    public StructureDestroyedEvent(Structure structure, @Nullable Entity byWhom) {
        super(structure);
        this.causer_ = byWhom;
    }


    //GETTERS
    @Nullable public Entity getDestructor() {
        return this.causer_;
    }
}
