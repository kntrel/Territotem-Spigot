package com.kntrel.mc.territotem.totem.region;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpansionTableTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void rowMatchesByMaterialWhenNoNbtConstraintIsConfigured() {
        ExpansionTable.Row row = new ExpansionTable.Row(Material.DIAMOND, 1, null, 4d, 6d, null);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.DIAMOND);

        assertTrue(row.matches(stack, null, null));
    }

    @Test
    void rowRejectsDifferentMaterial() {
        ExpansionTable.Row row = new ExpansionTable.Row(Material.DIAMOND, 1, null, 4d, 6d, null);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.EMERALD);

        assertFalse(row.matches(stack, null, null));
    }

    @Test
    void pickExpansionScalarReturnsFixedValueForSinglePointRange() {
        ExpansionTable.Row row = new ExpansionTable.Row(Material.DIAMOND, 1, null, 6d, 6d, null);
        assertEquals(6d, row.pickExpansionScalar(), DELTA);
    }

    @Test
    void dropBackRateFallsBackToGlobalDefaultWhenRowOmitsIt() {
        ExpansionTable.Row row = new ExpansionTable.Row(Material.DIAMOND, 1, null, 6d, 6d, null);
        assertEquals(0.5d, row.dropBackRateOr(0.5d), DELTA);
    }
}
