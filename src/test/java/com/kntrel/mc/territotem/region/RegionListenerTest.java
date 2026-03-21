package com.kntrel.mc.territotem.region;

import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionListenerTest {

    @Test
    void diffPermissionsDetectsAddedRemovedAndChangedPlayers() {
        UUID unchanged = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID removed = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID changed = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID added = UUID.fromString("00000000-0000-0000-0000-000000000004");

        List<RegionListener.PermissionChange> changes = RegionListener.diffPermissions(
                List.of(
                        new RegionSnapshot.Permission(unchanged, 1),
                        new RegionSnapshot.Permission(removed, 2),
                        new RegionSnapshot.Permission(changed, 3)
                ),
                List.of(
                        new RegionSnapshot.Permission(unchanged, 1),
                        new RegionSnapshot.Permission(changed, 4),
                        new RegionSnapshot.Permission(added, 5)
                )
        );

        assertEquals(List.of(
                new RegionListener.PermissionChange(RegionListener.PermissionChangeType.REMOVE, removed, 2, null),
                new RegionListener.PermissionChange(RegionListener.PermissionChangeType.CHANGE, changed, 3, 4),
                new RegionListener.PermissionChange(RegionListener.PermissionChangeType.ADD, added, null, 5)
        ), changes);
    }

    @Test
    void collectRecipientIdsIncludesPlayersRemovedFromTheRegion() {
        UUID removed = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID current = UUID.fromString("00000000-0000-0000-0000-000000000011");

        Set<UUID> recipients = RegionListener.collectRecipientIds(
                List.of(new RegionSnapshot.Permission(removed, 1)),
                List.of(new RegionSnapshot.Permission(current, 1))
        );

        assertEquals(Set.of(removed, current), recipients);
    }

    @Test
    void translationKeyMatchesRecipientPerspective() {
        UUID responsible = UUID.fromString("00000000-0000-0000-0000-000000000020");
        UUID affected = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID observer = UUID.fromString("00000000-0000-0000-0000-000000000022");

        RegionListener.PermissionChange addition = new RegionListener.PermissionChange(
                RegionListener.PermissionChangeType.ADD,
                affected,
                null,
                1
        );

        assertEquals(
                "add.first_person_first_person",
                addition.translationKeyFor(affected, affected)
        );
        assertEquals(
                "add.first_person_third_person",
                addition.translationKeyFor(responsible, responsible)
        );
        assertEquals(
                "add.third_person_first_person",
                addition.translationKeyFor(affected, responsible)
        );
        assertEquals(
                "add.third_person_third_person",
                addition.translationKeyFor(observer, responsible)
        );
    }
}
