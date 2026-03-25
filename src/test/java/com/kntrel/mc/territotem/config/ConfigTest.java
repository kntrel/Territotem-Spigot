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
                totem:
                  directional_selector_item: AMETHYST_BLOCK
                  expansion:
                    - item: DIAMOND
                      consume: 2
                      min: 1.5
                      max: 3.25
                  deeds_request_items:
                    - WRITABLE_BOOK
                """);

        Config config = Config.read(yaml);
        ExpansionTable.Row row = config.expansionTable().rows().get(0);

        assertEquals(Material.DIAMOND, row.item());
        assertEquals(2, row.consumption());
        assertEquals(1.5d, row.expansionMin(), DELTA);
        assertEquals(3.25d, row.expansionMax(), DELTA);
        assertEquals(null, row.nbt());
    }

    @Test
    void readUsesDefaultExpansionTableWhenMissing() {
        Config config = Config.read(new YamlConfiguration());
        ExpansionTable.Row row = config.expansionTable().rows().get(0);

        assertEquals(Material.DIAMOND, row.item());
        assertEquals(1, row.consumption());
        assertEquals(6d, row.expansionMin(), DELTA);
        assertEquals(6d, row.expansionMax(), DELTA);
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
