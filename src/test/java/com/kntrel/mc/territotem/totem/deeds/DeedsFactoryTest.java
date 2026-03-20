package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.territotem.test.mock.MockPlugin;
import com.kntrel.mc.territotem.test.mock.MockWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeedsFactoryTest {

    @Test
    void interpretReportsCorrectPageLineAndContentForDeTranspileErrors() {
        Server server = mock(Server.class);
        Plugin plugin = new MockPlugin("Territotem", server);
        RegionContext context = mock(RegionContext.class);
        World world = mock(World.class);

        when(context.getNamespace()).thenReturn("territotem");
        when(context.getPlugin()).thenReturn(plugin);
        when(context.getServer()).thenReturn(server);
        when(context.getConfig()).thenReturn(RegionContextConfig.defaultConfig());

        Hierarchy hierarchy = new Hierarchy(1L, "test");
        hierarchy.addGroup("admins", 100, List.of());
        Region region = new Region(context, new BoundingBox(0, 0, 0, 1, 1, 1), world, "region", hierarchy);
        region.setId(42L);

        when(context.get(42L)).thenReturn(Optional.of(region));

        PersistentDataContainer pdc = MockWorld.mockPersistentDataContainer();
        BookMeta bookMeta = mock(BookMeta.class);
        when(bookMeta.getPersistentDataContainer()).thenReturn(pdc);
        when(bookMeta.getPages()).thenReturn(List.of(
                String.join("\n", Collections.nCopies(13, "")),
                "Ali-ce"
        ));

        pdc.set(
                new NamespacedKey(plugin, "deeds"),
                DeedsPersistentDataType.instance(),
                new DeedsPersistentData("territotem", 42L, 1)
        );

        DeedsInterpretationResult result = new DeedsFactory(context, mock(Translator.class)).interpret(bookMeta);

        DeedsInterpretationResult.DeTranspileError error = assertInstanceOf(
                DeedsInterpretationResult.DeTranspileError.class,
                result
        );
        assertEquals(2, error.page());
        assertEquals(1, error.lineNumber());
        assertEquals("Ali-ce", error.line());

        DeedsDeTranspilingError.UnexpectedCharacter cause = assertInstanceOf(
                DeedsDeTranspilingError.UnexpectedCharacter.class,
                error.error()
        );
        assertEquals(4, cause.index());
    }
}
