package com.kntrel.mc.territotem.config;

import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void readUsesConfiguredExpansionRows() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                chunk_persistence:
                  period_ticks: 40
                totem:
                  directional_selector_item: AMETHYST_BLOCK
                  drop_back_rate: 0.6
                  expansion:
                    - item: DIAMOND
                      consume: 2
                      min: 1.5
                      max: 3.25
                      drop_back_rate: 0.25
                  deeds_request_items:
                    - WRITABLE_BOOK
                """);

        Config config = Config.read(yaml);
        ExpansionTable.Row row = config.expansionTable().rows().get(0);

        assertEquals(0.6d, config.dropBackRate(), DELTA);
        assertEquals(Material.DIAMOND, row.item());
        assertEquals(2, row.consumption());
        assertEquals(1.5d, row.expansionMin(), DELTA);
        assertEquals(3.25d, row.expansionMax(), DELTA);
        assertEquals(0.25d, row.dropBackRate(), DELTA);
        assertEquals(null, row.nbt());
        assertEquals(40L, config.chunkPersistencePeriodTicks());
    }

    @Test
    void readUsesDefaultExpansionTableWhenMissing() {
        Config config = Config.read(new YamlConfiguration());
        assertEquals(0.5d, config.dropBackRate(), DELTA);
        assertEquals(200L, config.chunkPersistencePeriodTicks());
        assertEquals(4, config.expansionTable().rows().size());

        ExpansionTable.Row diamondRow = config.expansionTable().rows().getFirst();
        assertEquals(Material.DIAMOND, diamondRow.item());
        assertEquals(1, diamondRow.consumption());
        assertEquals(6d, diamondRow.expansionMin(), DELTA);
        assertEquals(6d, diamondRow.expansionMax(), DELTA);
        assertEquals(null, diamondRow.dropBackRate());

        ExpansionTable.Row lastRow = config.expansionTable().rows().getLast();
        assertEquals(Material.NETHERITE_BLOCK, lastRow.item());
        assertEquals(1, lastRow.consumption());
        assertEquals(216d, lastRow.expansionMin(), DELTA);
        assertEquals(270d, lastRow.expansionMax(), DELTA);
        assertEquals(null, lastRow.dropBackRate());
    }

    @Test
    void readAllowsExplicitlyEmptyExpansionTable() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                totem:
                  expansion: []
                """);

        Config config = Config.read(yaml);
        assertTrue(config.expansionTable().rows().isEmpty());
    }
}
