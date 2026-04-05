package com.kntrel.mc.state.check;

import com.kntrel.mc.state.StateMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StateCheckTest {

    private static StateMap state(String blockDataString) {
        return StateMap.from(blockDataString);
    }

    @Test
    @DisplayName("state-level factories cover passing and failing scenarios")
    void testStateLevelFactories() {
        StateMap sample = state("minecraft:oak_stairs[facing=east,half=top,waterlogged=false]");
        StateMap other = state("minecraft:oak_stairs[facing=east,waterlogged=false]");

        assertTrue(StateCheck.has("facing").test(sample));
        assertFalse(StateCheck.has("missing").test(sample));

        assertTrue(StateCheck.sizeIs(3).test(sample));
        assertFalse(StateCheck.sizeIs(2).test(sample));

        assertTrue(StateCheck.sizeIsBiggerThan(2).test(sample));
        assertFalse(StateCheck.sizeIsBiggerThan(3).test(sample));

        assertTrue(StateCheck.sizeIsLessThan(4).test(sample));
        assertFalse(StateCheck.sizeIsLessThan(3).test(sample));

        assertTrue(StateCheck.sizeIsBiggerEquals(3).test(sample));
        assertFalse(StateCheck.sizeIsBiggerEquals(4).test(sample));

        assertTrue(StateCheck.sizeIsLessEquals(3).test(sample));
        assertFalse(StateCheck.sizeIsLessEquals(2).test(sample));

        assertTrue(StateCheck.matches(other).test(sample));
        assertFalse(StateCheck.matches(state("minecraft:oak_stairs[facing=west]")).test(sample));

        assertTrue(StateCheck.all().test(sample));
        assertFalse(StateCheck.any().test(sample));

        assertTrue(StateCheck.all(StateCheck.has("facing"), StateCheck.has("half")).test(sample));
        assertFalse(StateCheck.all(StateCheck.has("facing"), StateCheck.has("missing")).test(sample));

        assertTrue(StateCheck.any(StateCheck.has("missing"), StateCheck.has("half")).test(sample));
        assertFalse(StateCheck.any(StateCheck.has("missing"), StateCheck.has("other")).test(sample));

        assertTrue(StateCheck.not(StateCheck.has("missing")).test(sample));
        assertFalse(StateCheck.not(StateCheck.has("facing")).test(sample));
    }

    @Test
    @DisplayName("value-level factories cover matching and failing scenarios")
    void testValueLevelFactories() {
        StateMap sample = state("minecraft:oak_stairs[facing=east,half=top,age=3,shape=straight]");

        assertTrue(StateCheck.isEquals("facing", "east").test(sample));
        assertFalse(StateCheck.isEquals("facing", "west").test(sample));

        assertTrue(StateCheck.isNotEquals("facing", "west").test(sample));
        assertFalse(StateCheck.isNotEquals("facing", "east").test(sample));

        assertTrue(StateCheck.contains("shape", "trai").test(sample));
        assertFalse(StateCheck.contains("shape", "curve").test(sample));

        assertTrue(StateCheck.isBiggerThan("age", 2).test(sample));
        assertFalse(StateCheck.isBiggerThan("age", 3).test(sample));

        assertTrue(StateCheck.isLessThan("age", 4).test(sample));
        assertFalse(StateCheck.isLessThan("age", 3).test(sample));

        assertTrue(StateCheck.isBiggerEquals("age", 3).test(sample));
        assertFalse(StateCheck.isBiggerEquals("age", 4).test(sample));

        assertTrue(StateCheck.isLessEquals("age", 3).test(sample));
        assertFalse(StateCheck.isLessEquals("age", 2).test(sample));

        assertTrue(StateCheck.in("half", "bottom", "top").test(sample));
        assertFalse(StateCheck.in("half", "left", "right").test(sample));

        assertTrue(StateCheck.in("half", List.of("top", "bottom")).test(sample));
        assertFalse(StateCheck.in("half", List.of("west", "east")).test(sample));

        assertTrue(StateCheck.all("shape",
                value -> value.getAsString().contains("stra"),
                value -> value.getAsString().endsWith("ght")
        ).test(sample));
        assertFalse(StateCheck.all("shape",
                value -> value.getAsString().contains("stra"),
                value -> value.getAsString().startsWith("curve")
        ).test(sample));

        assertTrue(StateCheck.any("shape",
                value -> value.getAsString().startsWith("curve"),
                value -> value.getAsString().startsWith("stra")
        ).test(sample));
        assertFalse(StateCheck.any("shape",
                value -> value.getAsString().startsWith("curve"),
                value -> value.getAsString().endsWith("left")
        ).test(sample));

        assertTrue(StateCheck.regexMatches("shape", "straight").test(sample));
        assertFalse(StateCheck.regexMatches("shape", "stra.*x").test(sample));

        Pattern straightPattern = Pattern.compile("stra.*");
        assertTrue(StateCheck.regexMatches("shape", straightPattern).test(sample));
        assertFalse(StateCheck.regexMatches("shape", Pattern.compile("curve.*")).test(sample));

        assertTrue(StateCheck.startsWith("shape", "stra").test(sample));
        assertFalse(StateCheck.startsWith("shape", "ght").test(sample));

        assertTrue(StateCheck.endsWith("shape", "ight").test(sample));
        assertFalse(StateCheck.endsWith("shape", "stra").test(sample));

        assertTrue(StateCheck.regexContains("shape", "rai").test(sample));
        assertFalse(StateCheck.regexContains("shape", "xyz").test(sample));

        assertTrue(StateCheck.regexContains("shape", Pattern.compile("tra")).test(sample));
        assertFalse(StateCheck.regexContains("shape", Pattern.compile("xyz")).test(sample));

        assertTrue(StateCheck.keyMatches("^fa").test(sample));
        assertFalse(StateCheck.keyMatches("^zz").test(sample));

        assertTrue(StateCheck.keyMatches(Pattern.compile("^ha")).test(sample));
        assertFalse(StateCheck.keyMatches(Pattern.compile("^xx")).test(sample));

        assertTrue(StateCheck.missing("missing").test(sample));
        assertFalse(StateCheck.missing("facing").test(sample));

        assertTrue(StateCheck.check("age", value -> value.isNumeric() && value.getAsNumber().intValue() == 3).test(sample));
        assertFalse(StateCheck.check("age", value -> value.getAsString().equals("4")).test(sample));
    }

    @Test
    @DisplayName("default and/or/negate combinators retain StateCheck semantics")
    void testDefaultCombinators() {
        StateMap sample = state("minecraft:oak_stairs[facing=east,age=3]");

        StateCheck andCheck = StateCheck.has("facing").and(StateCheck.isEquals("age", 3));
        assertTrue(andCheck.test(sample));
        assertFalse(andCheck.test(state("minecraft:oak_stairs[facing=east,age=2]")));

        StateCheck orCheck = StateCheck.has("missing").or(StateCheck.isEquals("age", 3));
        assertTrue(orCheck.test(sample));
        assertFalse(orCheck.test(state("minecraft:oak_stairs[facing=east,age=2]")));

        StateCheck negated = StateCheck.has("facing").negate();
        assertFalse(negated.test(sample));
        assertTrue(negated.test(state("minecraft:oak_stairs[age=3]")));
    }
}
