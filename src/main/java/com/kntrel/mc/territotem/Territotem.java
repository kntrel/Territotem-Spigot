package com.kntrel.mc.territotem;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.territotem.mock.MockBlueprint;
import com.kntrel.mc.territotem.structure.StructureService;
import com.kntrel.mc.territotem.totem.TotemCoreListener;
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

        StructureService structureService = new StructureService(this);
        structureService.registerBlueprint(MockBlueprint.registration());
        new TotemService(ctx, structureService);

        this.getServer().getPluginManager().registerEvents(new TotemCoreListener(), this);

        LOGGER.info("Territotem is up and running");
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

