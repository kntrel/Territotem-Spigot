package com.kntrel.mc.territotem;

import com.kntrel.mc.territotem.blueprint.BlueprintRegistry;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.totem.TotemService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.logging.Level;

public final class Territotem extends JavaPlugin {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Territotem.class);


    //IMPLEMENTATION
    @Override
    public void onEnable() {
        wireSlf4jToPluginLogger("com.kntrel", this);

        RegionLib.enable(this);
        RegionContext ctx = RegionLib.createDefaultContext(this);

        BlueprintRegistry blueprintRegistry = new BlueprintRegistry(this);
        new TotemService(ctx, blueprintRegistry);

        LOGGER.debug("Debug message");
        LOGGER.info("Info message");
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    private static void wireSlf4jToPluginLogger(String rootPackage, Plugin plugin) {
        var pluginLogger = plugin.getLogger();
        var pkgLogger = java.util.logging.Logger.getLogger(rootPackage);

        pkgLogger.setParent(pluginLogger);
        pkgLogger.setUseParentHandlers(true);

        pluginLogger.setLevel(Level.ALL);
        pkgLogger.setLevel(Level.ALL);
    }
}

