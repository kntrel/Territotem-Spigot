package com.kntrel.mc.territotem.structure.blueprint;

import com.kntrel.mc.territotem.structure.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

public interface BlueprintRegistration {

    //FACTORY
    static BlueprintRegistrationBuilder.Completer forBlueprint(Blueprint blueprint) {
        return BlueprintRegistrationBuilder.forBlueprint(blueprint);
    }


    //CONTRACT
    Blueprint blueprint();
    @Nullable Class<? extends Event> eventClass();
    boolean appliesTo(Event event);
    TrackingInfo track(Event event);
    void onCompletion(Structure structure, Entity completer);
    void onDestruction(Structure structure, Entity destructor);
}
