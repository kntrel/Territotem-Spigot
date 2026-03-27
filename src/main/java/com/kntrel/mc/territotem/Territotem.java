package com.kntrel.mc.territotem;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.territotem.config.Config;
import com.kntrel.mc.territotem.region.RegionListener;
import com.kntrel.mc.territotem.region.TerritotemRegionFeatures;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import com.kntrel.mc.territotem.totem.event.TotemCoreCompletedEvent;
import com.kntrel.mc.territotem.structure.StructureService;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.mc.territotem.structure.piece.Piece;
import com.kntrel.mc.territotem.structure.piece.Tile;
import com.kntrel.mc.territotem.totem.TotemService;
import com.kntrel.mc.territotem.totem.TotemBlueprint;
import com.kntrel.mc.territotem.totem.piece.TotemPieceDirection;
import com.kntrel.mc.territotem.totem.piece.TotemPieces;
import com.kntrel.util.Vec3i;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;

public final class Territotem extends JavaPlugin {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(Territotem.class);

    //FIELDS
    private ChunkPersister chunkPersister_ = null;


    //IMPLEMENTATION
    @Override
    public void onEnable() {

        this.saveResource("hierarchies.json", false);
        Config config = Config.load(this, "config.yaml");

        RegionLib.enable(this);
        RegionContext regionContext = RegionLib.createContext(
                this,
                this.getName(),
                config.regionContextConfig(),
                new File(this.getDataFolder(), "hierarchies.json").toURI(),
                new File(this.getDataFolder(), ".db").toURI()
        );
        RegionLib.setDefault(regionContext);
        Hierarchy hierarchy = regionContext.getHierarchyRepository().get(1).orElse(null);

        Runical runical = new Runical(this, "translations");
        runical.setDefaultLocale("en");
        runical.mount("hierarchy", "totem.deeds.hierarchy");
        Translator totemTranslator = runical.getChild("totem");

        this.getServer().getPluginManager().registerEvents(
                new RegionListener(
                        this,
                        runical.getChild("not_allowed"),
                        runical.getChild("region").getChild("permissions"),
                        config.regionEnterTitle()
                ),
                this
        );

        this.chunkPersister_ = new ChunkPersister(this, config.chunkPersistencePeriodTicks());
        StructureService structureService = new StructureService(this, this.chunkPersister_);
        TotemCoreTracker totemCoreTracker = new TotemCoreTracker(this, this.chunkPersister_, config.directionalSelectorItem());
        TotemService totemService = new TotemService(
                regionContext,
                totemTranslator,
                config.expansionTable(),
                config.allowedDeedsRequestItems()
        );
        new TerritotemRegionFeatures(
                this,
                regionContext,
                totemService,
                config.allowedDeedsRequestItems(),
                config.regionFeatureMaterials()
        ).register();
        structureService.registerBlueprint(createTotemBlueprint(totemCoreTracker, hierarchy))
                .on(TotemCoreCompletedEvent.class)
                .when(e -> {
                    e.getCore().setState(TotemCore.State.ACTIVE);
                    e.setCancelled(true);
                    return true;
                })
                .track(e -> {
                    Vec3i origin = e.getCore().getCoordinates().subtract(new Vec3i(1, 2, 1));
                    return new TrackingInfo(origin, e.getCore().getWorld(), e.getCompleter());
                });


        LOGGER.info("Territotem is up and running");
    }
    @Override
    public void onDisable() {
        if (this.chunkPersister_ == null) {
            LOGGER.error("ChunkPersister instance was null on Territotem unload.");
        } else {
            this.chunkPersister_.shutdown();
        }
    }

    private static TotemBlueprint createTotemBlueprint(TotemCoreTracker service, Hierarchy hierarchy) {
        List<Tile> tile = List.of(
                new Tile(1, 2, 1, TotemPieces.core(service)),
                new Tile(1, 1, 1, Piece.block(Material.OBSIDIAN)),
                new Tile(1, 0, 1, Piece.block(Material.OBSIDIAN)),
                new Tile(2, 1, 1, TotemPieces.sign(TotemPieceDirection.EAST)),
                new Tile(0, 1, 1, TotemPieces.sign(TotemPieceDirection.WEST)),
                new Tile(1, 1, 2, TotemPieces.sign(TotemPieceDirection.SOUTH)),
                new Tile(1, 1, 0, TotemPieces.sign(TotemPieceDirection.NORTH)),
                new Tile(2, 0, 1, TotemPieces.lectern(TotemPieceDirection.EAST)),
                new Tile(0, 0, 1, TotemPieces.lectern(TotemPieceDirection.WEST)),
                new Tile(1, 0, 2, TotemPieces.lectern(TotemPieceDirection.SOUTH)),
                new Tile(1, 0, 0, TotemPieces.lectern(TotemPieceDirection.NORTH))

        );
        BoundingBox bb = new BoundingBox(-8, -2, -8, 8, 12, 8);
        return new TotemBlueprint(25, "totem", tile, bb, hierarchy);
    }
}
