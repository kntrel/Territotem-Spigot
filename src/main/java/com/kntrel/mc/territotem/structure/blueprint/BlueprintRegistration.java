package com.kntrel.mc.territotem.structure.blueprint;

import com.kntrel.util.Vec3i;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

public interface BlueprintRegistration {

    //FACTORY
    static BlueprintRegistrationBuilder.EventSelector forBlueprint(Blueprint blueprint) {
        return BlueprintRegistrationBuilder.forBlueprint(blueprint);
    }


    //CONTRACT
    Blueprint blueprint();
    @Nullable Class<? extends Event> eventClass();
    boolean appliesTo(Event event);
    TrackingInfo track(Event event);
}
