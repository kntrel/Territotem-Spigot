package com.kntrel.mc.territotem;

import com.kntrel.mc.chunkPersistence.ChunkPersister;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.runical.bukkit.Runical;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.territotem.config.Config;
import com.kntrel.mc.territotem.config.blueprint.TotemBlueprintDirectoryLoader;
import com.kntrel.mc.territotem.region.RegionListener;
import com.kntrel.mc.territotem.region.TerritotemRegionFeatures;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import com.kntrel.mc.territotem.totem.event.TotemCoreCompletedEvent;
import com.kntrel.mc.territotem.structure.StructureService;
import com.kntrel.mc.territotem.structure.blueprint.TrackingInfo;
import com.kntrel.mc.territotem.totem.TotemService;
import com.kntrel.mc.territotem.totem.TotemBlueprint;
import com.kntrel.util.Vec3i;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

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
                totemCoreTracker,
                config.dropBackRate(),
                config.allowedDeedsRequestItems()
        );
        new TerritotemRegionFeatures(
                this,
                regionContext,
                totemService,
                config.allowedDeedsRequestItems(),
                config.regionFeatureMaterials()
        ).register();
        TotemBlueprintDirectoryLoader blueprintLoader = new TotemBlueprintDirectoryLoader(
                this,
                totemCoreTracker,
                regionContext.getHierarchyRepository(),
                regionContext.getRuleRegistry()
        );
        for (TotemBlueprint blueprint : blueprintLoader.loadAll()) {
            registerTotemBlueprint(structureService, blueprint);
        }


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

    private static void registerTotemBlueprint(StructureService structureService, TotemBlueprint blueprint) {
        structureService.registerBlueprint(blueprint)
                .on(TotemCoreCompletedEvent.class)
                .when(e -> {
                    e.getCore().setState(TotemCore.State.FULL);
                    e.setCancelled(true);
                    return true;
                })
                .track(e -> {
                    Vec3i origin = e.getCore().getCoordinates().subtract(blueprint.core().offset());
                    return new TrackingInfo(origin, e.getCore().getWorld(), e.getCompleter());
                });
    }
}
