package com.kntrel.mc.territotem.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

public final class TerritotemGeyserExtension implements Extension {

    private static final String WELCOME_MESSAGE = "Welcome to Territotem Bedrock!";

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.logger().info("Territotem Geyser extension loaded.");
    }

    @Subscribe
    public void onSessionJoin(SessionJoinEvent event) {
        event.connection().sendMessage(WELCOME_MESSAGE);
    }
}
