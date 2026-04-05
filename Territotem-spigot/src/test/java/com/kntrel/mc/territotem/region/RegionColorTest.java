package com.kntrel.mc.territotem.region;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionColorTest {

    @Test
    void parseHexRoundTripsThroughSerialization() {
        RegionColor color = RegionColor.parseHex("#12ab34");

        assertEquals(0x12AB34, color.packedRgb());
        assertEquals("#12AB34", color.serialize());
        assertEquals(color, RegionColor.deserialize(color.serialize()));
    }

    @Test
    void minecraftCodeUsesExactHexDigits() {
        RegionColor color = RegionColor.parseHex("#12AB34");

        assertEquals("\u00A7x\u00A71\u00A72\u00A7A\u00A7B\u00A73\u00A74", color.minecraftCode());
        assertEquals("\u00A7x\u00A71\u00A72\u00A7A\u00A7B\u00A73\u00A74Land\u00A7r", color.apply("Land"));
    }

    @Test
    void exactChatColorKeepsTheOriginalRgb() {
        RegionColor color = RegionColor.parseHex("#12AB34");
        java.awt.Color awt = color.chatColor().getColor();

        assertEquals(0x12, awt.getRed());
        assertEquals(0xAB, awt.getGreen());
        assertEquals(0x34, awt.getBlue());
    }

    @Test
    void exactFactoriesFromLegacyAndDyeColorsUseTheirCanonicalRgbValues() {
        assertEquals("#55FF55", RegionColor.fromLegacyColor(ChatColor.GREEN).hex());
        assertEquals(Color.fromRGB(DyeColor.LIME.getColor().asRGB()), RegionColor.fromDyeColor(DyeColor.LIME).bukkitColor());
    }

    @Test
    void approximationsPickNearestLegacyAndDyeColors() {
        RegionColor color = RegionColor.parseHex("#5AF85A");

        assertEquals(ChatColor.GREEN, color.legacyChatColor());
        assertEquals(ChatColor.GREEN.toString(), color.legacyCode());
        assertEquals(DyeColor.LIME, color.dyeColor());
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RegionColor.parseHex("#12345"));
        assertThrows(IllegalArgumentException.class, () -> RegionColor.ofRgb(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> RegionColor.ofPackedRgb(0x1_000000));
        assertThrows(IllegalArgumentException.class, () -> RegionColor.fromLegacyColor(ChatColor.BOLD));
    }
}
