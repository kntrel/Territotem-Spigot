package com.kntrel.mc.territotem.totem.event;

import com.kntrel.mc.territotem.totem.core.TotemCore;
import org.bukkit.event.Event;

public abstract class TotemCoreEvent extends Event {

    //FIELDS
    private final TotemCore core_;


    //CONSTRUCTOR
    protected TotemCoreEvent(TotemCore core) {
        this.core_ = core;
    }


    //GETTERS
    public TotemCore getCore() {
        return this.core_;
    }
}
