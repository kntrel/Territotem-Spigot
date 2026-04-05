package com.kntrel.mc.territotem.totem;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.territotem.structure.Structure;
import com.kntrel.mc.territotem.totem.core.TotemCore;
import com.kntrel.mc.territotem.totem.deeds.Deeds;
import com.kntrel.mc.territotem.totem.deeds.DeedsFactory;
import com.kntrel.mc.territotem.totem.region.Expansion;
import com.kntrel.mc.territotem.totem.region.ExpansionResult;
import com.kntrel.mc.territotem.totem.region.ExpansionTable;
import com.kntrel.mc.territotem.util.ItemStackInfo;
import com.kntrel.util.Vec3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TotemTest {

    @Test
    void feedDelegatesExpansionFromCoreDirectionAndPlaysSoundOnGrowth() {
        Fixture fixture = fixture(ExpansionTable.of(ExpansionTable.row(Material.DIAMOND, 2, null, 2d, 2d)));
        when(fixture.core().getDirection()).thenReturn(TotemCore.Direction.ALL);

        ExpansionResult expected = new ExpansionResult(
                Expansion.all(2d / 6d),
                Expansion.all(2d / 6d)
        );
        when(fixture.service().expand(any(), any(), any(), anyDouble())).thenReturn(expected);

        ItemStack payment = new ItemStack(Material.DIAMOND, 3);
        Totem.FeedResult actual = fixture.totem().feed(payment);

        assertSame(Totem.FeedResult.Status.EXPANDED, actual.status());
        assertSame(expected, actual.expansionResult());
        verify(fixture.service()).expand(
                same(fixture.totem()),
                eq(Expansion.all(2d / 6d)),
                eq(new ItemStackInfo(Material.DIAMOND, 2, null)),
                eq(0.5d)
        );
        verify(fixture.world()).playSound(
                same(fixture.location()),
                eq(Totem.FEED_SOUND),
                eq(5f),
                eq(1.5f)
        );
        assertSame(payment, actual.updatedStack());
        assertEquals(1, payment.getAmount());
    }

    @Test
    void feedIgnoresNonExpansionItems() {
        Fixture fixture = fixture();

        Totem.FeedResult actual = fixture.totem().feed(new ItemStack(Material.STICK, 1));

        assertSame(Totem.FeedResult.Status.IGNORED, actual.status());
        verify(fixture.service(), never()).expand(any(), any(), any(), anyDouble());
    }

    @Test
    void feedRejectsInsufficientMatchingPayment() {
        Fixture fixture = fixture(ExpansionTable.of(ExpansionTable.row(Material.DIAMOND, 2, null, 2d, 2d)));

        Totem.FeedResult actual = fixture.totem().feed(new ItemStack(Material.DIAMOND, 1));

        assertSame(Totem.FeedResult.Status.INSUFFICIENT_ITEMS, actual.status());
        verify(fixture.service(), never()).expand(any(), any(), any(), anyDouble());
    }

    @Test
    void takeDamageRollsBackDropsRefundsAndPlaysSound() {
        Fixture fixture = fixture();
        TotemGrowthEntry growth = new TotemGrowthEntry(
                new Expansion(1, 0, 0, 0, 0, 0),
                new ItemStackInfo(Material.IRON_INGOT, 2, null),
                1d
        );
        when(fixture.service().rollbackLastGrowth(fixture.totem())).thenReturn(growth);

        TotemGrowthEntry actual = fixture.totem().takeDamage(() -> 0d);

        assertSame(growth, actual);
        verify(fixture.world()).dropItemNaturally(
                same(fixture.center()),
                argThat(stack -> stack.getType() == Material.IRON_INGOT && stack.getAmount() == 2)
        );
        verify(fixture.world()).playSound(
                same(fixture.location()),
                eq(Totem.HIT_SOUND),
                eq(5f),
                eq(.8f)
        );
    }

    @Test
    void takeDamageReturnsNullWhenNothingCanShrink() {
        Fixture fixture = fixture();
        when(fixture.service().rollbackLastGrowth(fixture.totem())).thenReturn(null);

        TotemGrowthEntry actual = fixture.totem().takeDamage(() -> 0d);

        assertNull(actual);
        verify(fixture.world(), never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
        verify(fixture.world(), never()).playSound(
                same(fixture.location()),
                eq(Totem.HIT_SOUND),
                anyFloat(),
                anyFloat()
        );
    }

    @Test
    void emitAmbientSoundPlaysOnlyWhenActive() {
        Fixture fixture = fixture();
        when(fixture.core().getState()).thenReturn(TotemCore.State.ACTIVE);

        fixture.totem().emitAmbientSound();

        verify(fixture.world()).playSound(
                same(fixture.center()),
                eq(Totem.AMBIENT_SOUND),
                eq(SoundCategory.BLOCKS),
                eq(1f),
                eq(.8f)
        );
    }

    @Test
    void createDeedsBookDelegatesToFactorySavesAndPlaysSound() {
        Fixture fixture = fixture();
        DeedsFactory deedsFactory = mock(DeedsFactory.class);
        Player player = mock(Player.class);
        Deeds expected = mock(Deeds.class);

        when(fixture.service().getDeedsFactory()).thenReturn(deedsFactory);
        when(deedsFactory.generate(player, fixture.totem())).thenReturn(expected);

        Deeds actual = fixture.totem().createDeedsBook(player);

        assertSame(expected, actual);
        verify(deedsFactory).generate(player, fixture.totem());
        verify(fixture.region()).save();
        verify(fixture.world()).playSound(
                same(fixture.location()),
                eq(Totem.DEEDS_CREATE_SOUND),
                eq(5f),
                eq(1f)
        );
    }

    @Test
    void rollDropBackCountEvaluatesEachConsumedItemIndependently() {
        double[] rolls = {0.1d, 0.6d, 0.49d, 0.5d, 0.9d};
        AtomicInteger index = new AtomicInteger();

        int refunded = Totem.rollDropBackCount(
                5,
                0.5d,
                () -> rolls[index.getAndIncrement()]
        );

        assertEquals(3, refunded);
    }

    private static Fixture fixture() {
        return fixture(ExpansionTable.empty());
    }

    private static Fixture fixture(ExpansionTable expansionTable) {
        TotemService service = mock(TotemService.class);
        Structure structure = mock(Structure.class);
        TotemBlueprint blueprint = mock(TotemBlueprint.class);
        Region region = mock(Region.class);
        TotemCore core = mock(TotemCore.class);
        World world = mock(World.class);
        Location center = new Location(world, 10.5, 64.5, 10.5);
        Location location = new Location(world, 10, 64, 10);

        when(structure.blueprint()).thenReturn(blueprint);
        when(structure.id()).thenReturn(UUID.randomUUID());
        when(structure.origin()).thenReturn(new Vec3i(1, 2, 3));
        when(structure.world()).thenReturn(world);
        when(region.getBoundingBox()).thenReturn(new BoundingBox(0, 0, 0, 1, 1, 1));
        when(core.getWorld()).thenReturn(world);
        when(core.getCenter()).thenReturn(center);
        when(core.getLocation()).thenReturn(location);
        when(core.getDirection()).thenReturn(TotemCore.Direction.ALL);

        return new Fixture(new Totem(service, structure, region, core, expansionTable, 0.5d), service, region, core, world, center, location);
    }

    private static final class Fixture {
        private final Totem totem_;
        private final TotemService service_;
        private final Region region_;
        private final TotemCore core_;
        private final World world_;
        private final Location center_;
        private final Location location_;

        private Fixture(
                Totem totem,
                TotemService service,
                Region region,
                TotemCore core,
                World world,
                Location center,
                Location location
        ) {
            this.totem_ = totem;
            this.service_ = service;
            this.region_ = region;
            this.core_ = core;
            this.world_ = world;
            this.center_ = center;
            this.location_ = location;
        }

        private Totem totem() {
            return this.totem_;
        }

        private TotemService service() {
            return this.service_;
        }

        private Region region() {
            return this.region_;
        }

        private TotemCore core() {
            return this.core_;
        }

        private World world() {
            return this.world_;
        }

        private Location center() {
            return this.center_;
        }

        private Location location() {
            return this.location_;
        }
    }
}
