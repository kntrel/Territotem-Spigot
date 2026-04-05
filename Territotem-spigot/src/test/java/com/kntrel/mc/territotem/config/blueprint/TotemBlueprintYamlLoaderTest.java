package com.kntrel.mc.territotem.config.blueprint;

import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import com.kntrel.mc.territotem.totem.TotemBlueprint;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import com.kntrel.util.Vec3i;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TotemBlueprintYamlLoaderTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void readBuildsTotemBlueprintFromYaml() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 1
                id: 25
                name: totem
                hierarchy-id: 1
                initial-region-bounds:
                  - [8, 12, 8]
                  - [-8, -2, -8]
                rules:
                  autoplant: true
                  fire_spread_rate: 0.3
                  welcome_message: hello
                  max_animals: 4
                palette:
                  C:
                    piece: core
                  O:
                    block: OBSIDIAN
                  T:
                    block: OAK_STAIRS
                    facing: EAST
                    half: BOTTOM
                    shape: STRAIGHT
                  N:
                    piece: name_sign
                    direction: north
                  E:
                    piece: name_sign
                    direction: east
                  S:
                    piece: name_sign
                    direction: south
                  W:
                    piece: name_sign
                    direction: west
                  n:
                    piece: deeds_lectern
                    direction: north
                  e:
                    piece: deeds_lectern
                    direction: east
                  s:
                    piece: deeds_lectern
                    direction: south
                  w:
                    piece: deeds_lectern
                    direction: west
                layout:
                  skip: "."
                  layers:
                    - y: 2
                      rows:
                        - "..."
                        - ".C."
                        - "..."
                    - y: 1
                      rows:
                        - ".N."
                        - "WOE"
                        - ".S."
                    - y: 0
                      rows:
                        - ".n."
                        - "wTe"
                        - ".s."
                """);

        TotemCoreTracker coreTracker = mock(TotemCoreTracker.class);
        HierarchyRepository hierarchyRepository = mock(HierarchyRepository.class);
        RuleRegistry ruleRegistry = mock(RuleRegistry.class);
        Hierarchy hierarchy = mock(Hierarchy.class);
        @SuppressWarnings("unchecked")
        Rule<Boolean> autoPlantRule = mock(Rule.class);
        @SuppressWarnings("unchecked")
        Rule<Double> fireSpreadRule = mock(Rule.class);
        @SuppressWarnings("unchecked")
        Rule<String> welcomeMessageRule = mock(Rule.class);
        @SuppressWarnings("unchecked")
        Rule<Integer> maxAnimalsRule = mock(Rule.class);
        when(hierarchyRepository.get(1L)).thenReturn(Optional.of(hierarchy));
        when(autoPlantRule.name()).thenReturn("autoplant");
        when(autoPlantRule.valueType()).thenReturn(ValueType.BOOL);
        when(fireSpreadRule.name()).thenReturn("fire_spread_rate");
        when(fireSpreadRule.valueType()).thenReturn(ValueType.DOUBLE);
        when(welcomeMessageRule.name()).thenReturn("welcome_message");
        when(welcomeMessageRule.valueType()).thenReturn(ValueType.STRING);
        when(maxAnimalsRule.name()).thenReturn("max_animals");
        when(maxAnimalsRule.valueType()).thenReturn(ValueType.INT);
        when(ruleRegistry.get("autoplant")).thenReturn(Optional.of(autoPlantRule));
        when(ruleRegistry.get("fire_spread_rate")).thenReturn(Optional.of(fireSpreadRule));
        when(ruleRegistry.get("welcome_message")).thenReturn(Optional.of(welcomeMessageRule));
        when(ruleRegistry.get("max_animals")).thenReturn(Optional.of(maxAnimalsRule));

        TotemBlueprint blueprint = new TotemBlueprintYamlLoader(coreTracker, hierarchyRepository, ruleRegistry)
                .read(yaml, "memory:domestic.yml");

        assertEquals(25L, blueprint.id());
        assertEquals("totem", blueprint.name());
        assertSame(hierarchy, blueprint.hierarchy());
        assertEquals(new Vec3i(3, 3, 3), blueprint.dimensions());
        assertEquals(new Vec3i(1, 2, 1), blueprint.core().offset());
        assertEquals(4, blueprint.lecterns().size());
        assertEquals(4, blueprint.nameSings().size());
        assertEquals(-8d, blueprint.initialRegionBounds().getMinX(), DELTA);
        assertEquals(-2d, blueprint.initialRegionBounds().getMinY(), DELTA);
        assertEquals(-8d, blueprint.initialRegionBounds().getMinZ(), DELTA);
        assertEquals(8d, blueprint.initialRegionBounds().getMaxX(), DELTA);
        assertEquals(12d, blueprint.initialRegionBounds().getMaxY(), DELTA);
        assertEquals(8d, blueprint.initialRegionBounds().getMaxZ(), DELTA);
        assertEquals(
                List.of(
                        new TotemBlueprintRule("autoplant", true),
                        new TotemBlueprintRule("fire_spread_rate", 0.3d),
                        new TotemBlueprintRule("welcome_message", "hello"),
                        new TotemBlueprintRule("max_animals", 4)
                ),
                blueprint.rules()
        );
    }

    @Test
    void readRejectsMultiCharacterPaletteSymbols() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 1
                id: 1
                name: invalid
                hierarchy-id: 1
                initial-region-bounds:
                  - [0, 0, 0]
                  - [1, 1, 1]
                palette:
                  SE:
                    block: STONE
                layout:
                  skip: "."
                  layers:
                    - y: 0
                      rows:
                        - "S"
                """);

        TotemCoreTracker coreTracker = mock(TotemCoreTracker.class);
        HierarchyRepository hierarchyRepository = mock(HierarchyRepository.class);
        RuleRegistry ruleRegistry = mock(RuleRegistry.class);
        when(hierarchyRepository.get(1L)).thenReturn(Optional.of(mock(Hierarchy.class)));

        assertThrows(
                BlueprintValidationException.class,
                () -> new TotemBlueprintYamlLoader(coreTracker, hierarchyRepository, ruleRegistry)
                        .read(yaml, "memory:invalid.yml")
        );
    }

    @Test
    void readRejectsUnknownRuleNames() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 1
                id: 2
                name: invalid-rules
                hierarchy-id: 1
                initial-region-bounds:
                  - [0, 0, 0]
                  - [1, 1, 1]
                rules:
                  not_registered: true
                palette:
                  C:
                    piece: core
                layout:
                  skip: "."
                  layers:
                    - y: 0
                      rows:
                        - "C"
                """);

        TotemCoreTracker coreTracker = mock(TotemCoreTracker.class);
        HierarchyRepository hierarchyRepository = mock(HierarchyRepository.class);
        RuleRegistry ruleRegistry = mock(RuleRegistry.class);
        when(hierarchyRepository.get(1L)).thenReturn(Optional.of(mock(Hierarchy.class)));
        when(ruleRegistry.get("not_registered")).thenReturn(Optional.empty());
    }
}
