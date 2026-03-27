package com.kntrel.mc.territotem.config;

import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.territotem.region.ColoredRegionDisplayer;
import com.kntrel.mc.territotem.region.RegionEnterTitleConfig;
import com.kntrel.mc.territotem.region.RegionFeatureMaterialsConfig;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record Config(
        Material directionalSelectorItem,
        ExpansionTable expansionTable,
        Set<Material> allowedDeedsRequestItems,
        RegionContextConfig regionContextConfig,
        RegionEnterTitleConfig regionEnterTitle,
        RegionFeatureMaterialsConfig regionFeatureMaterials
) {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private static final Config DEFAULT = new Config(
            Material.AMETHYST_BLOCK,
            ExpansionTable.of(
                    /* =============== Farmable by default; enable manually if desired. ===============
                    ExpansionTable.row(Material.RAW_COPPER, 0.15, 0.25),
                    ExpansionTable.row(Material.COPPER_NUGGET, 0.20, 0.30),
                    ExpansionTable.row(Material.COPPER_INGOT, 0.20, 0.30),
                    ExpansionTable.row(Material.COPPER_BLOCK, 2.0, 3.0),

                    ExpansionTable.row(Material.RAW_IRON, 0.30, 0.45),
                    ExpansionTable.row(Material.IRON_NUGGET, 0.35, 0.50),
                    ExpansionTable.row(Material.IRON_INGOT, 0.35, 0.50),
                    ExpansionTable.row(Material.IRON_BLOCK, 3.5, 5.0),

                    ExpansionTable.row(Material.RAW_GOLD, 0.40, 0.60),
                    ExpansionTable.row(Material.GOLD_NUGGET, 0.50, 0.70),
                    ExpansionTable.row(Material.GOLD_INGOT, 0.50, 0.70),
                    ExpansionTable.row(Material.GOLD_BLOCK, 5.0, 7.0),*/

                    ExpansionTable.row(Material.DIAMOND, 6.0, 6.0),
                    ExpansionTable.row(Material.DIAMOND_BLOCK, 54.0, 60.0),

                    ExpansionTable.row(Material.NETHERITE_INGOT, 24.0, 30.0),
                    ExpansionTable.row(Material.NETHERITE_BLOCK, 216.0, 270.0)
            ),
            immutableLinkedSet(List.of(Material.WRITABLE_BOOK)),
            RegionContextConfig.build().withRegionDisplayerFactory(ColoredRegionDisplayer::new).end(),
            new RegionEnterTitleConfig(true, 10, 20, 10),
            new RegionFeatureMaterialsConfig(
                    immutableLinkedSet(List.of(
                            Material.STONE_BUTTON,
                            Material.POLISHED_BLACKSTONE_BUTTON
                    )),
                    immutableLinkedSet(List.of(
                            Material.COPPER_BLOCK,
                            Material.CUT_COPPER,
                            Material.EXPOSED_COPPER,
                            Material.EXPOSED_CUT_COPPER,
                            Material.WEATHERED_COPPER,
                            Material.WEATHERED_CUT_COPPER,
                            Material.WAXED_COPPER_BLOCK,
                            Material.WAXED_CUT_COPPER,
                            Material.WAXED_EXPOSED_COPPER,
                            Material.WAXED_EXPOSED_CUT_COPPER,
                            Material.WAXED_WEATHERED_COPPER,
                            Material.WAXED_WEATHERED_CUT_COPPER,
                            Material.CUT_COPPER_SLAB,
                            Material.CUT_COPPER_STAIRS,
                            Material.EXPOSED_CUT_COPPER_SLAB,
                            Material.EXPOSED_CUT_COPPER_STAIRS,
                            Material.WEATHERED_CUT_COPPER_SLAB,
                            Material.WEATHERED_CUT_COPPER_STAIRS,
                            Material.WAXED_CUT_COPPER_SLAB,
                            Material.WAXED_CUT_COPPER_STAIRS,
                            Material.WAXED_EXPOSED_CUT_COPPER_SLAB,
                            Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,
                            Material.WAXED_WEATHERED_CUT_COPPER_SLAB,
                            Material.WAXED_WEATHERED_CUT_COPPER_STAIRS,
                            Material.IRON_BLOCK
                    ))
            )
    );

    public static Config load(JavaPlugin plugin, String relativePath) {
        File file = ensureFileExists(plugin, relativePath);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return read(yaml);
    }

    static Config read(YamlConfiguration yaml) {
        ConfigNode root = ConfigNode.root(yaml, LOGGER);
        ConfigNode totem = root.child("totem");
        ConfigNode region = root.child("region");
        ConfigNode cache = region.child("cache");
        ConfigNode playerSampling = region.child("player_sampling");
        ConfigNode enterTitle = region.child("enter_title");

        RegionContextConfig defaultRegionContextConfig = DEFAULT.regionContextConfig();

        return new Config(
                totem.blockMaterial("directional_selector_item", DEFAULT.directionalSelectorItem()),
                ExpansionTableParser.read(totem, "expansion", DEFAULT.expansionTable()),
                totem.materialSet("deeds_request_items", DEFAULT.allowedDeedsRequestItems()),
                new RegionContextConfig(
                        region.intValue("min_name_length", defaultRegionContextConfig.minNameLength),
                        region.intValue("max_name_length", defaultRegionContextConfig.maxNameLength),
                        region.enumValue(
                                "permissions_overlap_mode",
                                Permission.OverlapMode.class,
                                defaultRegionContextConfig.permissionsOverlapMode
                        ),
                        region.intValue(
                                "display_duration_seconds",
                                defaultRegionContextConfig.regionDisplayDurationSeconds
                        ),
                        cache.cellSize("cell_size", defaultRegionContextConfig.cellSize),
                        cache.intValue("capacity", defaultRegionContextConfig.cacheCapacity),
                        playerSampling.longValue("period_ticks", defaultRegionContextConfig.playerSamplingPeriodTicks),
                        playerSampling.doubleValue(
                                "movement_tolerance",
                                defaultRegionContextConfig.playerMovementTolerance
                        ),
                        ColoredRegionDisplayer::new
                ),
                new RegionEnterTitleConfig(
                        enterTitle.booleanValue("enabled", DEFAULT.regionEnterTitle().enabled()),
                        enterTitle.intValue("fade_in", DEFAULT.regionEnterTitle().fadeIn()),
                        enterTitle.intValue("stay", DEFAULT.regionEnterTitle().stay()),
                        enterTitle.intValue("fade_out", DEFAULT.regionEnterTitle().fadeOut())
                ),
                new RegionFeatureMaterialsConfig(
                        region.materialSet("enforced_buttons", DEFAULT.regionFeatureMaterials().enforcedButtons()),
                        region.materialSet("lever_locker_blocks", DEFAULT.regionFeatureMaterials().leverLockerBlocks())
                )
        );
    }

    private static File ensureFileExists(JavaPlugin plugin, String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        if (file.exists()) {
            return file;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.error("Could not create directories for config path '{}'.", file.getAbsolutePath());
        }

        plugin.saveResource(relativePath, false);
        LOGGER.info(
                "Config file '{}' was missing from the file system and has been copied from the jar.",
                relativePath
        );
        return file;
    }

    private static Set<Material> immutableLinkedSet(List<Material> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
