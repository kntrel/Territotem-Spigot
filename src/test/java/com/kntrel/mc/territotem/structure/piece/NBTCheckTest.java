package com.kntrel.mc.territotem.structure.piece;

import com.kntrel.mc.nbt.NBTTag;
import com.kntrel.mc.nbt.test.MockNBTCompound;
import com.kntrel.mc.nbt.test.MockNBTList;
import com.kntrel.mc.nbt.test.MockNBTPrimitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NBTCheckTest {

    private static MockNBTPrimitive p(Object value) {
        return new MockNBTPrimitive(value);
    }

    @Test
    @DisplayName("not/isEquals/isNotEquals support both matching and failing scenarios")
    void testNotAndEqualsFactories() {
        NBTCheck eq = NBTCheck.isEquals(42);
        assertTrue(eq.test(p(42L)));
        assertFalse(eq.test(p(7)));
        assertFalse(eq.test(null));

        NBTCheck arrEq = NBTCheck.isEquals(new Number[]{1, 2L, 3.0});
        assertTrue(arrEq.test(p(new Number[]{1L, 2, 3f})));
        assertFalse(arrEq.test(p(new Number[]{1, 2})));
        assertFalse(arrEq.test(p("[1,2,3]")));

        NBTCheck notEq = NBTCheck.not(eq);
        assertFalse(notEq.test(p(42)));
        assertTrue(notEq.test(p(41)));

        NBTCheck isNotEquals = NBTCheck.isNotEquals("hello");
        assertFalse(isNotEquals.test(p("hello")));
        assertTrue(isNotEquals.test(p("world")));
    }

    @Test
    @DisplayName("numeric comparison factories validate success and failure conditions")
    void testNumericComparisonFactories() {
        NBTTag ten = p(10);
        NBTTag eleven = p(11);
        NBTTag text = p("10");

        assertTrue(NBTCheck.isBiggerThan(10).test(eleven));
        assertFalse(NBTCheck.isBiggerThan(10).test(ten));
        assertFalse(NBTCheck.isBiggerThan(10).test(text));

        assertTrue(NBTCheck.isLessThan(10).test(p(9L)));
        assertFalse(NBTCheck.isLessThan(10).test(ten));

        assertTrue(NBTCheck.isBiggerThanEquals(10).test(ten));
        assertTrue(NBTCheck.isBiggerThanEquals(10).test(eleven));
        assertFalse(NBTCheck.isBiggerThanEquals(10).test(p(9)));

        assertTrue(NBTCheck.isLessThanEquals(10).test(ten));
        assertTrue(NBTCheck.isLessThanEquals(10).test(p(9)));
        assertFalse(NBTCheck.isLessThanEquals(10).test(eleven));
    }

    @Test
    @DisplayName("contains handles primitives, arrays, lists, compounds and failures")
    void testContainsFactory() {
        assertTrue(NBTCheck.contains(3).test(p(new Number[]{1, 2, 3L})));
        assertFalse(NBTCheck.contains(7).test(p(new Number[]{1, 2, 3L})));

        assertTrue(NBTCheck.contains(5).test(p(5L)));
        assertFalse(NBTCheck.contains(5).test(p(6L)));

        assertTrue(NBTCheck.contains("ell").test(p("hello")));
        assertFalse(NBTCheck.contains("zzz").test(p("hello")));

        MockNBTList list = new MockNBTList(List.of(p(1), p("abc"), p(3)));
        assertTrue(NBTCheck.contains("abc").test(list));
        assertFalse(NBTCheck.contains("def").test(list));

        MockNBTCompound compound = new MockNBTCompound(Map.of("key", p(1), "other", p(2)));
        assertTrue(NBTCheck.contains("key").test(compound));
        assertFalse(NBTCheck.contains("missing").test(compound));
    }

    @Test
    @DisplayName("regex and matches factories support string and pattern overloads")
    void testRegexFactories() {
        NBTTag value = p("alpha-beta");

        assertTrue(NBTCheck.matches("alpha").test(value));
        assertFalse(NBTCheck.matches("gamma").test(value));

        Pattern suffixPattern = Pattern.compile("beta$");
        assertTrue(NBTCheck.matches(suffixPattern).test(value));
        assertTrue(NBTCheck.regexMatch("alpha-.*").test(value));
        assertTrue(NBTCheck.regexMatch(suffixPattern).test(value));
        assertFalse(NBTCheck.regexMatch("^beta").test(value));
    }

    @Test
    @DisplayName("size factories validate list, compound and primitive cases")
    void testSizeFactories() {
        MockNBTList list = new MockNBTList(List.of(p(1), p(2), p(3)));
        MockNBTCompound compound = new MockNBTCompound(Map.of("a", p(1), "b", p(2)));
        NBTTag primitive = p(99);

        assertTrue(NBTCheck.sizeIs(3).test(list));
        assertFalse(NBTCheck.sizeIs(2).test(list));

        assertTrue(NBTCheck.sizeIsBiggerThan(2).test(list));
        assertFalse(NBTCheck.sizeIsBiggerThan(3).test(list));

        assertTrue(NBTCheck.sizeIsLessThan(3).test(compound));
        assertFalse(NBTCheck.sizeIsLessThan(2).test(compound));

        assertTrue(NBTCheck.sizeIsBiggerThanEquals(1).test(primitive));
        assertTrue(NBTCheck.sizeIsLessThanEquals(1).test(primitive));
        assertFalse(NBTCheck.sizeIsLessThanEquals(0).test(primitive));
    }

    @Test
    @DisplayName("exists/checkAt factories resolve both string and array paths")
    void testExistsAndCheckAtFactories() {
        MockNBTCompound root = new MockNBTCompound(Map.of(
                "level", new MockNBTCompound(Map.of(
                        "count", p(5),
                        "names", new MockNBTList(List.of(p("x"), p("y")))
                ))
        ));

        assertTrue(NBTCheck.exists("level.count").test(root));
        assertFalse(NBTCheck.exists("level.missing").test(root));
        assertTrue(NBTCheck.exists(" ").test(root));

        assertTrue(NBTCheck.exists("level", "names", 1).test(root));
        assertFalse(NBTCheck.exists("level", "names", 4).test(root));
        assertTrue(NBTCheck.exists().test(root));

        assertTrue(NBTCheck.checkAt(NBTCheck.isEquals(5), "level.count").test(root));
        assertFalse(NBTCheck.checkAt(NBTCheck.isEquals(6), "level.count").test(root));

        assertTrue(NBTCheck.checkAt(NBTCheck.isEquals("y"), "level", "names", 1).test(root));
        assertFalse(NBTCheck.checkAt(NBTCheck.isEquals("z"), "level", "names", 1).test(root));

        assertTrue(NBTCheck.checkAt(NBTCheck.isEquals(root)).test(root));
    }

    @Test
    @DisplayName("matches(reference) validates deep recursive matching")
    void testMatchesFactory() {
        MockNBTCompound reference = new MockNBTCompound(Map.of(
                "name", p("totem"),
                "stats", new MockNBTCompound(Map.of("power", p(10))),
                "list", new MockNBTList(List.of(p(1), p(2)))
        ));

        MockNBTCompound matching = new MockNBTCompound(Map.of(
                "name", p("totem"),
                "stats", new MockNBTCompound(Map.of("power", p(10), "extra", p(99))),
                "list", new MockNBTList(List.of(p(1), p(2))),
                "ignored", p("any")
        ));

        MockNBTCompound wrongPower = new MockNBTCompound(Map.of(
                "name", p("totem"),
                "stats", new MockNBTCompound(Map.of("power", p(9))),
                "list", new MockNBTList(List.of(p(1), p(2)))
        ));

        MockNBTCompound shortList = new MockNBTCompound(Map.of(
                "name", p("totem"),
                "stats", new MockNBTCompound(Map.of("power", p(10))),
                "list", new MockNBTList(List.of(p(1)))
        ));

        assertTrue(NBTCheck.matches(reference).test(matching));
        assertFalse(NBTCheck.matches(reference).test(wrongPower));
        assertFalse(NBTCheck.matches(reference).test(shortList));
        assertFalse(NBTCheck.matches(reference).test(null));
    }

    @Test
    @DisplayName("all/any/in and predicate combinators support pass/fail behavior")
    void testCollectionAndPredicateFactories() {
        NBTTag value = p(5);

        NBTCheck all = NBTCheck.all(NBTCheck.isBiggerThan(1), NBTCheck.isLessThan(10));
        assertTrue(all.test(value));
        assertFalse(all.test(p(11)));

        NBTCheck any = NBTCheck.any(NBTCheck.isEquals(1), NBTCheck.isEquals(5));
        assertTrue(any.test(value));
        assertFalse(any.test(p(9)));

        NBTCheck in = NBTCheck.in("a", 5, 10L);
        assertTrue(in.test(value));
        assertFalse(in.test(p("z")));

        NBTCheck andCombined = NBTCheck.isBiggerThan(0).and(tag -> NBTCheck.isLessThan(6).test(tag));
        assertTrue(andCombined.test(value));
        assertFalse(andCombined.test(p(6)));

        NBTCheck orCombined = NBTCheck.isEquals(1).or(NBTCheck.isEquals(2));
        assertTrue(orCombined.test(p(2)));
        assertFalse(orCombined.test(p(3)));

        assertFalse(NBTCheck.isEquals(5).negate().test(value));
    }
}
