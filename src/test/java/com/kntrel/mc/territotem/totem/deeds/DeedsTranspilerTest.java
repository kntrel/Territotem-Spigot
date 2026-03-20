package com.kntrel.mc.territotem.totem.deeds;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.runical.bukkit.Translator;
import com.kntrel.mc.runical.core.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeedsTranspilerTest {


    @Test
    void deTranspileParsesRepeatedGroupsAndIgnoresEmptySections() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");
        UUID bobId = fixture.addOfflinePlayer("Bob");
        UUID claraId = fixture.addOfflinePlayer("Clara");

        List<Permission> permissions = fixture.transpiler().deTranspile(fixture.region(), """
                admins:
                Alice
                members:
                admins:
                Bob

                members:
                Clara
                """);

        assertEquals(List.of(aliceId, bobId, claraId), permissions.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("admins", "admins", "members"), permissions.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    @Test
    void deTranspileAllowsWhitespaceBeforeGroupColon() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");

        List<Permission> permissions = fixture.transpiler().deTranspile(fixture.region(), """
                admins  :
                Alice
                """);

        assertEquals(List.of(aliceId), permissions.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("admins"), permissions.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    @Test
    void deTranspileIgnoresSingleLineAndBlockComments() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");
        UUID bobId = fixture.addOfflinePlayer("Bob");

        List<Permission> permissions = fixture.transpiler().deTranspile(fixture.region(), """
                - this is a prologue line
                ---
                this is a longer
                multiline comment
                ---
                admins:
                Alice
                - Bob is recorded below
                members:
                Bob
                """);

        assertEquals(List.of(aliceId, bobId), permissions.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("admins", "members"), permissions.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    @Test
    void deTranspileIgnoresMinecraftFormattingCodes() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");
        UUID bobId = fixture.addOfflinePlayer("Bob");

        List<Permission> permissions = fixture.transpiler().deTranspile(fixture.region(), """
                §8- comment with formatting
                §7---
                §aformatted prologue
                §7---
                §cadmins§r:
                §bAlice
                members:
                §6Bob
                """);

        assertEquals(List.of(aliceId, bobId), permissions.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("admins", "members"), permissions.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    @Test
    void deTranspileAllowsUnterminatedBlockCommentsAtEndOfFile() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");

        List<Permission> permissions = fixture.transpiler().deTranspile(fixture.region(), """
                admins:
                Alice
                ---
                unfinished comment
                still unfinished
                """);

        assertEquals(List.of(aliceId), permissions.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("admins"), permissions.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    @Test
    void deTranspileFailsWhenTheFirstLineIsNotAGroup() {
        Fixture fixture = fixture();

        DeedsDeTranspilingException exception = assertThrows(
                DeedsDeTranspilingException.class,
                () -> fixture.transpiler().deTranspile(fixture.region(), """
                        Alice
                        admins:
                        """)
        );

        assertEquals(1, exception.getLineNumber());
        assertInstanceOf(DeedsDeTranspilingError.NoGroupNameProvided.class, exception.getErrorCause());
    }

    @Test
    void deTranspileFailsForUnknownGroups() {
        Fixture fixture = fixture();
        fixture.addOfflinePlayer("Alice");

        DeedsDeTranspilingException exception = assertThrows(
                DeedsDeTranspilingException.class,
                () -> fixture.transpiler().deTranspile(fixture.region(), """
                        owners:
                        Alice
                        """)
        );

        DeedsDeTranspilingError.UnknownGroup error = assertInstanceOf(
                DeedsDeTranspilingError.UnknownGroup.class,
                exception.getErrorCause()
        );
        assertEquals(1, exception.getLineNumber());
        assertEquals("owners", error.groupName());
    }

    @Test
    void deTranspileFailsForUnknownPlayers() {
        Fixture fixture = fixture();

        DeedsDeTranspilingException exception = assertThrows(
                DeedsDeTranspilingException.class,
                () -> fixture.transpiler().deTranspile(fixture.region(), """
                        admins:
                        Alice
                        """)
        );

        DeedsDeTranspilingError.PlayerNotFound error = assertInstanceOf(
                DeedsDeTranspilingError.PlayerNotFound.class,
                exception.getErrorCause()
        );
        assertEquals(2, exception.getLineNumber());
        assertEquals("Alice", error.name());
    }

    @Test
    void deTranspileFailsForIllegalNicknameCharacters() {
        Fixture fixture = fixture();

        DeedsDeTranspilingException exception = assertThrows(
                DeedsDeTranspilingException.class,
                () -> fixture.transpiler().deTranspile(fixture.region(), """
                        admins:
                        Ali-ce
                        """)
        );

        DeedsDeTranspilingError.UnexpectedCharacter error = assertInstanceOf(
                DeedsDeTranspilingError.UnexpectedCharacter.class,
                exception.getErrorCause()
        );
        assertEquals(2, exception.getLineNumber());
        assertEquals(4, error.index());
    }

    @Test
    void deTranspileFailsWhenMoreThanOneColonIsPresent() {
        Fixture fixture = fixture();

        DeedsDeTranspilingException exception = assertThrows(
                DeedsDeTranspilingException.class,
                () -> fixture.transpiler().deTranspile(fixture.region(), """
                        admins::
                        Alice
                        """)
        );

        DeedsDeTranspilingError.UnexpectedCharacter error = assertInstanceOf(
                DeedsDeTranspilingError.UnexpectedCharacter.class,
                exception.getErrorCause()
        );
        assertEquals(1, exception.getLineNumber());
        assertEquals(7, error.index());
    }

    @Test
    void transpileChunksGroupsAndRepeatsTheHeaderOnEveryPage() {
        Fixture fixture = fixture();
        UUID alphaId = fixture.addOfflinePlayer("Alpha");
        UUID bravoId = fixture.addOfflinePlayer("Bravo");
        UUID charlieId = fixture.addOfflinePlayer("Charlie");
        UUID deltaId = fixture.addOfflinePlayer("Delta");
        UUID echoId = fixture.addOfflinePlayer("Echo");

        String[] sections = fixture.transpile(List.of(
                new Permission(bravoId, fixture.region(), 100),
                new Permission(echoId, fixture.region(), 100),
                new Permission(alphaId, fixture.region(), 100),
                new Permission(deltaId, fixture.region(), 100),
                new Permission(charlieId, fixture.region(), 100)
        ));

        assertArrayEquals(new String[] {
                """
                members:
                """,
                """
                admins:
                Alpha
                Bravo
                """,
                """
                admins:
                Charlie
                Delta
                """,
                """
                admins:
                Echo
                """
        }, sections);
    }

    @Test
    void transpileSplitsPrologueOnExplicitPageDelimitersAndTrimsBlankLinesBeforeThem() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");

        String[] sections = fixture.transpile(
                List.of(new Permission(aliceId, fixture.region(), 100)),
                "Prologue line 1\n\n\n\n/page\nPrologue line 2",
                10
        );

        assertArrayEquals(new String[] {
                """
                ---
                Prologue line 1
                """,
                """
                Prologue line 2
                ---
                """,
                """
                members:
                """,
                """
                admins:
                Alice
                """
        }, sections);

        List<Permission> reparsed = fixture.transpiler().deTranspile(fixture.region(), sections);
        assertEquals(List.of(aliceId), reparsed.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("admins"), reparsed.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    @Test
    void transpileReturnsOnlyThePrologueCommentPagesWhenPermissionsAreEmpty() {
        Fixture fixture = fixture();

        String[] sections = fixture.transpile(List.of(), "Prologue line", 10);

        assertArrayEquals(new String[] {
                """
                ---
                Prologue line
                ---
                """
        }, sections);
    }

    @Test
    void transpileReturnsAtLeastOnePagePerGroupEvenWhenEmpty() {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");

        String[] sections = fixture.transpile(List.of(
                new Permission(aliceId, fixture.region(), 100)
        ), 10);

        assertArrayEquals(new String[] {
                """
                members:
                """,
                """
                admins:
                Alice
                """
        }, sections);
    }

    @Test
    void transpileProducesRoundTrippableSectionsWhenTheChunkFitsTheGroup() throws Exception {
        Fixture fixture = fixture();
        UUID aliceId = fixture.addOfflinePlayer("Alice");
        UUID bobId = fixture.addOfflinePlayer("Bob");

        String[] sections = fixture.transpile(List.of(
                new Permission(bobId, fixture.region(), 10),
                new Permission(aliceId, fixture.region(), 100)
        ), 10);

        assertArrayEquals(new String[] {
                """
                members:
                Bob
                """,
                """
                admins:
                Alice
                """
        }, sections);

        List<Permission> reparsed = fixture.transpiler().deTranspile(fixture.region(), sections);
        assertEquals(List.of(bobId, aliceId), reparsed.stream().map(Permission::getPlayerId).toList());
        assertEquals(List.of("members", "admins"), reparsed.stream().map(permission -> permission.getGroup().getName()).toList());
    }

    private static Fixture fixture() {
        Server server = mock(Server.class);
        Plugin plugin = mock(Plugin.class);
        RegionContext context = mock(RegionContext.class);
        World world = mock(World.class);

        when(context.getConfig()).thenReturn(RegionContextConfig.defaultConfig());
        when(context.getServer()).thenReturn(server);
        when(plugin.getServer()).thenReturn(server);

        Directory directory = new Directory(server);
        Hierarchy hierarchy = new Hierarchy(1L, "test");
        hierarchy.addGroup("admins", 100, List.of());
        hierarchy.addGroup("members", 10, List.of());

        Region region = new Region(context, new BoundingBox(0, 0, 0, 1, 1, 1), world, "region", hierarchy);
        region.setId(1L);

        Player player = mock(Player.class);
        when(player.getLocale()).thenReturn("en-US");

        Translator translator = mock(Translator.class);
        return new Fixture(region, new DeedsTranspiler(server, translator), translator, directory, player);
    }

    private record Fixture(Region region, DeedsTranspiler transpiler, Translator translator, Directory directory, Player player) {

        private UUID addOfflinePlayer(String name) {
            return this.directory.addOfflinePlayer(name);
        }

        private String[] transpile(List<Permission> permissions) {
            return this.transpile(permissions, null, 3);
        }

        private String[] transpile(List<Permission> permissions, int pageSize) {
            return this.transpile(permissions, null, pageSize);
        }

        private String[] transpile(List<Permission> permissions, String prologue, int pageSize) {
            this.transpiler.setPageSizeLines(pageSize);
            if (prologue == null) {
                this.transpiler.setPrologueTranslationKey(null);
            } else {
                this.transpiler.setPrologueTranslationKey("test.prologue");
                when(this.translator.translateOrNull(any(Player.class), anyString(), any(Placeholder[].class))).thenReturn(prologue);
            }

            return this.transpiler.transpile(this.player, new Deeds(this.region, mock(BookMeta.class), permissions, 1));
        }
    }

    private static final class Directory {

        private final Server server_;
        private final Map<String, Player> onlineByName_ = new HashMap<>();
        private final Map<UUID, Player> onlineById_ = new HashMap<>();
        private final Map<String, OfflinePlayer> offlineByName_ = new HashMap<>();
        private final Map<UUID, OfflinePlayer> offlineById_ = new HashMap<>();

        private Directory(Server server) {
            this.server_ = server;

            when(this.server_.getPlayerExact(anyString())).thenAnswer(invocation -> this.onlineByName_.get(key(invocation.getArgument(0, String.class))));
            when(this.server_.getPlayer(any(UUID.class))).thenAnswer(invocation -> this.onlineById_.get(invocation.getArgument(0, UUID.class)));
            when(this.server_.getOfflinePlayers()).thenAnswer(invocation -> this.offlineById_.values().toArray(OfflinePlayer[]::new));
            when(this.server_.getOfflinePlayer(any(UUID.class))).thenAnswer(invocation -> this.offlineById_.get(invocation.getArgument(0, UUID.class)));
        }

        private UUID addOfflinePlayer(String name) {
            UUID id = UUID.randomUUID();
            OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
            when(offlinePlayer.getUniqueId()).thenReturn(id);
            when(offlinePlayer.getName()).thenReturn(name);

            this.offlineByName_.put(key(name), offlinePlayer);
            this.offlineById_.put(id, offlinePlayer);
            return id;
        }

        private static String key(String name) {
            return name.toLowerCase(Locale.ROOT);
        }
    }
}
