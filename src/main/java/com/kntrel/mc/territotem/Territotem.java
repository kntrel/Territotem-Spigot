package com.kntrel.mc.territotem;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.territotem.event.TotemCoreCreatedEvent;
import com.kntrel.mc.territotem.mock.MockBlueprint;
import com.kntrel.mc.territotem.structure.StructureService;
import com.kntrel.mc.territotem.structure.blueprint.Blueprint;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.totem.TotemCorePiece;
import com.kntrel.mc.territotem.totem.TotemService;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.logging.Level;


public final class Territotem extends JavaPlugin {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Territotem.class);


    //IMPLEMENTATION
    @Override
    public void onEnable() {
        wireSlf4jToPluginLogger("com.kntrel", this);

        RegionLib.enable(this);
        RegionContext regionContext = RegionLib.createDefaultContext(this);
        Hierarchy hierarchy = regionContext.getHierarchyRepository().get(1).orElse(null);

        StructureService structureService = new StructureService(this);
        TotemService totemService = new TotemService(regionContext);
        structureService.registerBlueprint(createTotemBlueprint(totemService, hierarchy))
                    .on(TotemCoreCreatedEvent.class)
                    .track(e -> {
                        Vec3i origin = e.getCore().getCoordinates().subtract(new Vec3i(0, 2, 0));
                        return new TrackingInfo(origin, e.getCore().getWorld(), e.getCreator());
                    });


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

    private static Blueprint createTotemBlueprint(TotemService service, Hierarchy hierarchy) {
        List<Tile> tile = List.of(
                new Tile(0, 2, 0, TotemCorePiece.ofService(service)),
                new Tile(0, 1, 0, Piece.block(Material.OBSIDIAN)),
                new Tile(0, 0, 0, Piece.block(Material.OBSIDIAN))

        );
        BoundingBox bb = new BoundingBox(-8, -2, -8, 8, 12, 8);
        return new Blueprint(25, "totem", tile, bb, hierarchy);
    }
}
