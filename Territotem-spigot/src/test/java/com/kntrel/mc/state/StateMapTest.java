package com.kntrel.mc.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateMapTest {

    @Test
    @DisplayName("from parses real BlockData-like strings into key/value entries")
    void testFromParsesBlockDataString() {
        String blockData = "minecraft:oak_stairs[facing=east,half=top,shape=straight,waterlogged=false]";

        StateMap state = StateMap.from(blockData);

        assertEquals(4, state.size());
        assertTrue(state.containsKey("facing"));
        assertTrue(state.containsKey("half"));
        assertTrue(state.containsKey("shape"));
        assertTrue(state.containsKey("waterlogged"));

        assertEquals("east", state.get("facing").getAsString());
        assertEquals("top", state.get("half").getAsString());
        assertEquals("straight", state.get("shape").getAsString());
        assertEquals("false", state.get("waterlogged").getAsString());
    }

    @Test
    @DisplayName("from parses numeric entries as numeric State.Value instances")
    void testFromParsesNumericValues() {
        StateMap state = StateMap.from("minecraft:wheat[age=3,moisture=7]");

        assertEquals(2, state.size());
        assertTrue(state.get("age").isNumeric());
        assertTrue(state.get("moisture").isNumeric());
        assertEquals(3.0, state.get("age").getAsNumber().doubleValue());
        assertEquals(7.0, state.get("moisture").getAsNumber().doubleValue());
    }

    @Test
    @DisplayName("from returns empty state when no block state section is present")
    void testFromNoBracketsReturnsEmpty() {
        StateMap state = StateMap.from("minecraft:stone");

        assertTrue(state.isEmpty());
    }

    @Test
    @DisplayName("from rejects malformed entries")
    void testFromMalformedEntryThrows() {
        assertThrows(InvalidStateException.class, () -> StateMap.from("minecraft:stone[facing]"));
        assertThrows(InvalidStateException.class, () -> StateMap.from("minecraft:stone[=east]"));
    }
}
