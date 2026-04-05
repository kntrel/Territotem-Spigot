package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.territotem.config.blueprint.TotemBlueprintRule;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.core.TotemCoreTracker;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TotemService ambient sound loop")
class TotemServiceTest {

    @Test
    @DisplayName("constructor schedules ambient playback at the configured rate")
    void constructorSchedulesAmbientPlaybackAtConfiguredRate() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        when(plugin.getName()).thenReturn("territotem");
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(BukkitTask.class));

        RegionContext regionContext = mock(RegionContext.class);
        when(regionContext.getPlugin()).thenReturn(plugin);
        when(regionContext.getServer()).thenReturn(server);
        when(regionContext.getNamespace()).thenReturn("territotem");

        Translator translator = mock(Translator.class);
        when(translator.getChild("deeds")).thenReturn(translator);

        TotemCoreTracker coreTracker = mock(TotemCoreTracker.class);

        new TotemService(
                regionContext,
                translator,
                ExpansionTable.empty(),
                coreTracker,
                0d,
                Set.of()
        );

        verify(scheduler).runTaskTimer(
                eq(plugin),
                any(Runnable.class),
                eq(Totem.AMBIENT_SOUND_RATE),
                eq(Totem.AMBIENT_SOUND_RATE)
        );
    }

    @Test
    @DisplayName("blueprint rules are copied into newly created regions")
    void applyBlueprintRulesSeedsRegionValues() {
        Region region = mock(Region.class);

        TotemService.applyBlueprintRules(
                region,
                List.of(
                        new TotemBlueprintRule("autoplant", true),
                        new TotemBlueprintRule("fire_spread_rate", 0.3d),
                        new TotemBlueprintRule("welcome_message", "hello"),
                        new TotemBlueprintRule("max_animals", 4)
                )
        );

        verify(region).setRuleValue("autoplant", true);
        verify(region).setRuleValue("fire_spread_rate", 0.3d);
        verify(region).setRuleValue("welcome_message", "hello");
        verify(region).setRuleValue("max_animals", 4);
    }
}
