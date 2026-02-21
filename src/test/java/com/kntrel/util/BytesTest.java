package com.kntrel.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bytes Utility Class Tests")
public class BytesTest {

    @Nested
    @DisplayName("toShort() Tests")
    class ToShortTests {

        @Test
        @DisplayName("Should convert byte array to short at offset 0")
        void testToShortBasic() {
            byte[] data = {0x12, 0x34};
            short result = Bytes.toShort(data, 0);
            assertEquals((short) 0x1234, result);
        }

        @Test
        @DisplayName("Should convert byte array to short at arbitrary offset")
        void testToShortWithOffset() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x56, 0x78};
            short result = Bytes.toShort(data, 2);
            assertEquals((short) 0x5678, result);
        }

        @Test
        @DisplayName("Should handle zero value")
        void testToShortZero() {
            byte[] data = {0x00, 0x00};
            short result = Bytes.toShort(data, 0);
            assertEquals((short) 0, result);
        }

        @Test
        @DisplayName("Should handle negative short value")
        void testToShortNegative() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF};
            short result = Bytes.toShort(data, 0);
            assertEquals((short) -1, result);
        }

        @Test
        @DisplayName("Should handle maximum short value (32767)")
        void testToShortMaxValue() {
            byte[] data = {0x7F, (byte) 0xFF};
            short result = Bytes.toShort(data, 0);
            assertEquals(Short.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Should handle minimum short value (-32768)")
        void testToShortMinValue() {
            byte[] data = {(byte) 0x80, 0x00};
            short result = Bytes.toShort(data, 0);
            assertEquals(Short.MIN_VALUE, result);
        }

        @Test
        @DisplayName("Should handle offset at array boundary")
        void testToShortAtBoundary() {
            byte[] data = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
            short result = Bytes.toShort(data, 2);
            assertEquals((short) 0xCCDD, result);
        }

        @Test
        @DisplayName("Should return 0 when offset equals array length")
        void testToShortOffsetEqualsLength() {
            byte[] data = {0x12, 0x34};
            short result = Bytes.toShort(data, 2);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should throw IndexOutOfBoundsException when offset is out of bounds")
        void testToShortOffsetOutOfBounds() {
            byte[] data = {0x12, 0x34};
            assertThrows(IndexOutOfBoundsException.class, () -> Bytes.toShort(data, 3));
        }

        @Test
        @DisplayName("Should throw IndexOutOfBoundsException when offset is negative")
        void testToShortNegativeOffset() {
            byte[] data = {0x12, 0x34};
            assertThrows(IndexOutOfBoundsException.class, () -> Bytes.toShort(data, -1));
        }

        @Test
        @DisplayName("Should handle partial read when offset + bytes exceeds array length")
        void testToShortPartialRead() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x12};
            short result = Bytes.toShort(data, 2);
            assertEquals((short) 0x1200, result);
        }
    }

    @Nested
    @DisplayName("toInt() Tests")
    class ToIntTests {

        @Test
        @DisplayName("Should convert byte array to int at offset 0")
        void testToIntBasic() {
            byte[] data = {0x12, 0x34, 0x56, 0x78};
            int result = Bytes.toInt(data, 0);
            assertEquals(0x12345678, result);
        }

        @Test
        @DisplayName("Should convert byte array to int at arbitrary offset")
        void testToIntWithOffset() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x12, 0x34, 0x56, 0x78};
            int result = Bytes.toInt(data, 2);
            assertEquals(0x12345678, result);
        }

        @Test
        @DisplayName("Should handle zero value")
        void testToIntZero() {
            byte[] data = {0x00, 0x00, 0x00, 0x00};
            int result = Bytes.toInt(data, 0);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle negative int value")
        void testToIntNegative() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            int result = Bytes.toInt(data, 0);
            assertEquals(-1, result);
        }

        @Test
        @DisplayName("Should handle maximum int value (2147483647)")
        void testToIntMaxValue() {
            byte[] data = {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            int result = Bytes.toInt(data, 0);
            assertEquals(Integer.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Should handle minimum int value (-2147483648)")
        void testToIntMinValue() {
            byte[] data = {(byte) 0x80, 0x00, 0x00, 0x00};
            int result = Bytes.toInt(data, 0);
            assertEquals(Integer.MIN_VALUE, result);
        }

        @Test
        @DisplayName("Should return 0 when offset equals array length")
        void testToIntOffsetEqualsLength() {
            byte[] data = {0x12, 0x34, 0x56, 0x78};
            int result = Bytes.toInt(data, 4);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should throw IndexOutOfBoundsException when offset is out of bounds")
        void testToIntOffsetOutOfBounds() {
            byte[] data = {0x12, 0x34, 0x56, 0x78};
            assertThrows(IndexOutOfBoundsException.class, () -> Bytes.toInt(data, 5));
        }

        @Test
        @DisplayName("Should throw IndexOutOfBoundsException when offset is negative")
        void testToIntNegativeOffset() {
            byte[] data = {0x12, 0x34, 0x56, 0x78};
            assertThrows(IndexOutOfBoundsException.class, () -> Bytes.toInt(data, -1));
        }

        @Test
        @DisplayName("Should handle partial read when offset + bytes exceeds array length")
        void testToIntPartialRead() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x12};
            int result = Bytes.toInt(data, 3);
            assertEquals(0x12000000, result);
        }

        @Test
        @DisplayName("Should handle multiple byte partial reads")
        void testToIntMultiBytePartialRead() {
            byte[] data = {(byte) 0xFF, 0x12, 0x34, 0x56};
            int result = Bytes.toInt(data, 1);
            assertEquals(0x12345600, result);
        }
    }

    @Nested
    @DisplayName("toLong() Tests")
    class ToLongTests {

        @Test
        @DisplayName("Should convert byte array to long at offset 0")
        void testToLongBasic() {
            byte[] data = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
            long result = Bytes.toLong(data, 0);
            assertEquals(0x123456789ABCDEF0L, result);
        }

        @Test
        @DisplayName("Should convert byte array to long at arbitrary offset")
        void testToLongWithOffset() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
            long result = Bytes.toLong(data, 2);
            assertEquals(0x123456789ABCDEF0L, result);
        }

        @Test
        @DisplayName("Should handle zero value")
        void testToLongZero() {
            byte[] data = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            long result = Bytes.toLong(data, 0);
            assertEquals(0L, result);
        }

        @Test
        @DisplayName("Should handle negative long value")
        void testToLongNegative() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            long result = Bytes.toLong(data, 0);
            assertEquals(-1L, result);
        }

        @Test
        @DisplayName("Should handle maximum long value")
        void testToLongMaxValue() {
            byte[] data = {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            long result = Bytes.toLong(data, 0);
            assertEquals(Long.MAX_VALUE, result);
        }

        @Test
        @DisplayName("Should handle minimum long value")
        void testToLongMinValue() {
            byte[] data = {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            long result = Bytes.toLong(data, 0);
            assertEquals(Long.MIN_VALUE, result);
        }

        @Test
        @DisplayName("Should return 0 when offset equals array length")
        void testToLongOffsetEqualsLength() {
            byte[] data = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
            long result = Bytes.toLong(data, 8);
            assertEquals(0L, result);
        }

        @Test
        @DisplayName("Should throw IndexOutOfBoundsException when offset is out of bounds")
        void testToLongOffsetOutOfBounds() {
            byte[] data = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
            assertThrows(IndexOutOfBoundsException.class, () -> Bytes.toLong(data, 9));
        }

        @Test
        @DisplayName("Should throw IndexOutOfBoundsException when offset is negative")
        void testToLongNegativeOffset() {
            byte[] data = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
            assertThrows(IndexOutOfBoundsException.class, () -> Bytes.toLong(data, -1));
        }

        @Test
        @DisplayName("Should handle partial read when offset + bytes exceeds array length")
        void testToLongPartialRead() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x12, 0x34};
            long result = Bytes.toLong(data, 3);
            assertEquals(0x1234000000000000L, result);
        }

        @Test
        @DisplayName("Should handle single byte read")
        void testToLongSingleByteRead() {
            byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x42};
            long result = Bytes.toLong(data, 7);
            assertEquals(0x4200000000000000L, result);
        }
    }

    @Nested
    @DisplayName("fromShort() Tests")
    class FromShortTests {

        @Test
        @DisplayName("Should convert short to byte array at offset 0")
        void testFromShortBasic() {
            byte[] target = new byte[2];
            Bytes.fromShort((short) 0x1234, target, 0);
            assertEquals(0x12, target[0]);
            assertEquals(0x34, target[1]);
        }

        @Test
        @DisplayName("Should convert short to byte array at arbitrary offset")
        void testFromShortWithOffset() {
            byte[] target = new byte[4];
            Bytes.fromShort((short) 0x5678, target, 2);
            assertEquals(0x56, target[2]);
            assertEquals(0x78, target[3]);
        }

        @Test
        @DisplayName("Should handle zero value")
        void testFromShortZero() {
            byte[] target = new byte[2];
            Bytes.fromShort((short) 0, target, 0);
            assertEquals(0x00, target[0]);
            assertEquals(0x00, target[1]);
        }

        @Test
        @DisplayName("Should handle negative short value")
        void testFromShortNegative() {
            byte[] target = new byte[2];
            Bytes.fromShort((short) -1, target, 0);
            assertEquals((byte) 0xFF, target[0]);
            assertEquals((byte) 0xFF, target[1]);
        }

        @Test
        @DisplayName("Should handle maximum short value")
        void testFromShortMaxValue() {
            byte[] target = new byte[2];
            Bytes.fromShort(Short.MAX_VALUE, target, 0);
            assertEquals(0x7F, target[0]);
            assertEquals((byte) 0xFF, target[1]);
        }

        @Test
        @DisplayName("Should handle minimum short value")
        void testFromShortMinValue() {
            byte[] target = new byte[2];
            Bytes.fromShort(Short.MIN_VALUE, target, 0);
            assertEquals((byte) 0x80, target[0]);
            assertEquals(0x00, target[1]);
        }

        @Test
        @DisplayName("Should write without affecting other bytes in target array")
        void testFromShortDoesNotAffectOtherBytes() {
            byte[] target = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            Bytes.fromShort((short) 0x1234, target, 1);
            assertEquals((byte) 0xFF, target[0]);
            assertEquals(0x12, target[1]);
            assertEquals(0x34, target[2]);
            assertEquals((byte) 0xFF, target[3]);
        }
    }

    @Nested
    @DisplayName("fromInt() Tests")
    class FromIntTests {

        @Test
        @DisplayName("Should convert int to byte array at offset 0")
        void testFromIntBasic() {
            byte[] target = new byte[4];
            Bytes.fromInt(0x12345678, target, 0);
            assertEquals(0x12, target[0]);
            assertEquals(0x34, target[1]);
            assertEquals(0x56, target[2]);
            assertEquals(0x78, target[3]);
        }

        @Test
        @DisplayName("Should convert int to byte array at arbitrary offset")
        void testFromIntWithOffset() {
            byte[] target = new byte[6];
            Bytes.fromInt(0x12345678, target, 1);
            assertEquals(0x12, target[1]);
            assertEquals(0x34, target[2]);
            assertEquals(0x56, target[3]);
            assertEquals(0x78, target[4]);
        }

        @Test
        @DisplayName("Should handle zero value")
        void testFromIntZero() {
            byte[] target = new byte[4];
            Bytes.fromInt(0, target, 0);
            assertEquals(0x00, target[0]);
            assertEquals(0x00, target[1]);
            assertEquals(0x00, target[2]);
            assertEquals(0x00, target[3]);
        }

        @Test
        @DisplayName("Should handle negative int value")
        void testFromIntNegative() {
            byte[] target = new byte[4];
            Bytes.fromInt(-1, target, 0);
            assertEquals((byte) 0xFF, target[0]);
            assertEquals((byte) 0xFF, target[1]);
            assertEquals((byte) 0xFF, target[2]);
            assertEquals((byte) 0xFF, target[3]);
        }

        @Test
        @DisplayName("Should handle maximum int value")
        void testFromIntMaxValue() {
            byte[] target = new byte[4];
            Bytes.fromInt(Integer.MAX_VALUE, target, 0);
            assertEquals(0x7F, target[0]);
            assertEquals((byte) 0xFF, target[1]);
            assertEquals((byte) 0xFF, target[2]);
            assertEquals((byte) 0xFF, target[3]);
        }

        @Test
        @DisplayName("Should handle minimum int value")
        void testFromIntMinValue() {
            byte[] target = new byte[4];
            Bytes.fromInt(Integer.MIN_VALUE, target, 0);
            assertEquals((byte) 0x80, target[0]);
            assertEquals(0x00, target[1]);
            assertEquals(0x00, target[2]);
            assertEquals(0x00, target[3]);
        }

        @Test
        @DisplayName("Should write without affecting other bytes in target array")
        void testFromIntDoesNotAffectOtherBytes() {
            byte[] target = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            Bytes.fromInt(0x12345678, target, 1);
            assertEquals((byte) 0xFF, target[0]);
            assertEquals(0x12, target[1]);
            assertEquals(0x34, target[2]);
            assertEquals(0x56, target[3]);
            assertEquals(0x78, target[4]);
        }
    }

    @Nested
    @DisplayName("fromLong() Tests")
    class FromLongTests {

        @Test
        @DisplayName("Should convert long to byte array at offset 0")
        void testFromLongBasic() {
            byte[] target = new byte[8];
            Bytes.fromLong(0x123456789ABCDEF0L, target, 0);
            assertEquals(0x12, target[0]);
            assertEquals(0x34, target[1]);
            assertEquals(0x56, target[2]);
            assertEquals(0x78, target[3]);
            assertEquals((byte) 0x9A, target[4]);
            assertEquals((byte) 0xBC, target[5]);
            assertEquals((byte) 0xDE, target[6]);
            assertEquals((byte) 0xF0, target[7]);
        }

        @Test
        @DisplayName("Should convert long to byte array at arbitrary offset")
        void testFromLongWithOffset() {
            byte[] target = new byte[10];
            Bytes.fromLong(0x123456789ABCDEF0L, target, 1);
            assertEquals(0x12, target[1]);
            assertEquals(0x34, target[2]);
            assertEquals(0x56, target[3]);
            assertEquals(0x78, target[4]);
            assertEquals((byte) 0x9A, target[5]);
            assertEquals((byte) 0xBC, target[6]);
            assertEquals((byte) 0xDE, target[7]);
            assertEquals((byte) 0xF0, target[8]);
        }

        @Test
        @DisplayName("Should handle zero value")
        void testFromLongZero() {
            byte[] target = new byte[8];
            Bytes.fromLong(0L, target, 0);
            for (byte b : target) {
                assertEquals(0x00, b);
            }
        }

        @Test
        @DisplayName("Should handle negative long value")
        void testFromLongNegative() {
            byte[] target = new byte[8];
            Bytes.fromLong(-1L, target, 0);
            for (byte b : target) {
                assertEquals((byte) 0xFF, b);
            }
        }

        @Test
        @DisplayName("Should handle maximum long value")
        void testFromLongMaxValue() {
            byte[] target = new byte[8];
            Bytes.fromLong(Long.MAX_VALUE, target, 0);
            assertEquals(0x7F, target[0]);
            for (int i = 1; i < 8; i++) {
                assertEquals((byte) 0xFF, target[i]);
            }
        }

        @Test
        @DisplayName("Should handle minimum long value")
        void testFromLongMinValue() {
            byte[] target = new byte[8];
            Bytes.fromLong(Long.MIN_VALUE, target, 0);
            assertEquals((byte) 0x80, target[0]);
            for (int i = 1; i < 8; i++) {
                assertEquals(0x00, target[i]);
            }
        }

        @Test
        @DisplayName("Should write without affecting other bytes in target array")
        void testFromLongDoesNotAffectOtherBytes() {
            byte[] target = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            Bytes.fromLong(0x123456789ABCDEF0L, target, 1);
            assertEquals((byte) 0xFF, target[0]);
            assertEquals(0x12, target[1]);
            assertEquals(0x34, target[2]);
            assertEquals(0x56, target[3]);
            assertEquals(0x78, target[4]);
            assertEquals((byte) 0x9A, target[5]);
            assertEquals((byte) 0xBC, target[6]);
            assertEquals((byte) 0xDE, target[7]);
            assertEquals((byte) 0xF0, target[8]);
        }
    }

    @Nested
    @DisplayName("Round-trip Conversion Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should successfully convert short to bytes and back")
        void testShortRoundTrip() {
            short original = (short) 0x5678;
            byte[] buffer = new byte[2];
            Bytes.fromShort(original, buffer, 0);
            short result = Bytes.toShort(buffer, 0);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Should successfully convert int to bytes and back")
        void testIntRoundTrip() {
            int original = 0x12345678;
            byte[] buffer = new byte[4];
            Bytes.fromInt(original, buffer, 0);
            int result = Bytes.toInt(buffer, 0);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Should successfully convert long to bytes and back")
        void testLongRoundTrip() {
            long original = 0x123456789ABCDEF0L;
            byte[] buffer = new byte[8];
            Bytes.fromLong(original, buffer, 0);
            long result = Bytes.toLong(buffer, 0);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Should successfully convert short to bytes and back at arbitrary offset")
        void testShortRoundTripWithOffset() {
            short original = (short) 0x5678;
            byte[] buffer = new byte[4];
            Bytes.fromShort(original, buffer, 1);
            short result = Bytes.toShort(buffer, 1);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Should successfully convert int to bytes and back at arbitrary offset")
        void testIntRoundTripWithOffset() {
            int original = 0x12345678;
            byte[] buffer = new byte[6];
            Bytes.fromInt(original, buffer, 1);
            int result = Bytes.toInt(buffer, 1);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Should successfully convert long to bytes and back at arbitrary offset")
        void testLongRoundTripWithOffset() {
            long original = 0x123456789ABCDEF0L;
            byte[] buffer = new byte[10];
            Bytes.fromLong(original, buffer, 1);
            long result = Bytes.toLong(buffer, 1);
            assertEquals(original, result);
        }

        @ParameterizedTest
        @ValueSource(shorts = {0, 1, -1, 100, -100, Short.MAX_VALUE, Short.MIN_VALUE})
        @DisplayName("Should round-trip various short values")
        void testShortRoundTripVariousValues(short original) {
            byte[] buffer = new byte[2];
            Bytes.fromShort(original, buffer, 0);
            short result = Bytes.toShort(buffer, 0);
            assertEquals(original, result);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, -1, 1000, -1000, Integer.MAX_VALUE, Integer.MIN_VALUE})
        @DisplayName("Should round-trip various int values")
        void testIntRoundTripVariousValues(int original) {
            byte[] buffer = new byte[4];
            Bytes.fromInt(original, buffer, 0);
            int result = Bytes.toInt(buffer, 0);
            assertEquals(original, result);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, -1L, 1000L, -1000L, Long.MAX_VALUE, Long.MIN_VALUE})
        @DisplayName("Should round-trip various long values")
        void testLongRoundTripVariousValues(long original) {
            byte[] buffer = new byte[8];
            Bytes.fromLong(original, buffer, 0);
            long result = Bytes.toLong(buffer, 0);
            assertEquals(original, result);
        }
    }

    @Nested
    @DisplayName("Edge Case and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty array with offset 0")
        void testEmptyArrayOffset0() {
            byte[] data = {};
            int result = Bytes.toInt(data, 0);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle single element array")
        void testSingleElementArray() {
            byte[] data = {0x42};
            long result = Bytes.toLong(data, 0);
            assertEquals(0x4200000000000000L, result);
        }

        @Test
        @DisplayName("Should handle byte with high bit set")
        void testHighBitBytes() {
            byte[] data = {(byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83};
            int result = Bytes.toInt(data, 0);
            assertEquals((int) 0x80818283L, result);
        }

        @Test
        @DisplayName("Should handle alternating byte patterns")
        void testAlternatingPattern() {
            byte[] data = {0x55, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, (byte) 0xAA};
            long result = Bytes.toLong(data, 0);
            assertEquals(0x55AA55AA55AA55AAL, result);
        }

        @Test
        @DisplayName("Should correctly handle offset just before end of array")
        void testOffsetJustBeforeEnd() {
            byte[] data = {0x00, 0x00, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78};
            int result = Bytes.toInt(data, 7);
            assertEquals(0x78000000, result);
        }

        @Test
        @DisplayName("Should handle large array with small read")
        void testLargeArraySmallRead() {
            byte[] data = new byte[1024];
            data[512] = 0x12;
            data[513] = 0x34;
            short result = Bytes.toShort(data, 512);
            assertEquals((short) 0x1234, result);
        }

        @Test
        @DisplayName("Should preserve precision when converting between types and back")
        void testPrecisionPreservation() {
            byte[] data = {(byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89};
            long original = Bytes.toLong(data, 0);
            byte[] buffer = new byte[8];
            Bytes.fromLong(original, buffer, 0);
            
            for (int i = 0; i < 8; i++) {
                assertEquals(data[i], buffer[i]);
            }
        }
    }

    @Nested
    @DisplayName("fromNumber() Internal Helper Tests")
    class FromNumberTests {

        @Test
        @DisplayName("Should convert single byte correctly")
        void testFromNumberSingleByte() {
            byte[] target = new byte[1];
            Bytes.fromNumber(0xFF, target, 0, 1);
            assertEquals((byte) 0xFF, target[0]);
        }

        @Test
        @DisplayName("Should convert two bytes correctly")
        void testFromNumberTwoBytes() {
            byte[] target = new byte[2];
            Bytes.fromNumber(0x1234, target, 0, 2);
            assertEquals(0x12, target[0]);
            assertEquals(0x34, target[1]);
        }

        @Test
        @DisplayName("Should convert three bytes correctly")
        void testFromNumberThreeBytes() {
            byte[] target = new byte[3];
            Bytes.fromNumber(0x123456, target, 0, 3);
            assertEquals(0x12, target[0]);
            assertEquals(0x34, target[1]);
            assertEquals(0x56, target[2]);
        }

        @Test
        @DisplayName("Should convert with offset")
        void testFromNumberWithOffset() {
            byte[] target = new byte[6];
            Bytes.fromNumber(0x1234, target, 2, 2);
            assertEquals(0x12, target[2]);
            assertEquals(0x34, target[3]);
        }

        @Test
        @DisplayName("Should handle large values across multiple bytes")
        void testFromNumberLargeValue() {
            byte[] target = new byte[8];
            Bytes.fromNumber(0xFEDCBA9876543210L, target, 0, 8);
            assertEquals((byte) 0xFE, target[0]);
            assertEquals((byte) 0xDC, target[1]);
            assertEquals((byte) 0xBA, target[2]);
            assertEquals((byte) 0x98, target[3]);
            assertEquals(0x76, target[4]);
            assertEquals(0x54, target[5]);
            assertEquals(0x32, target[6]);
            assertEquals(0x10, target[7]);
        }
    }
}
