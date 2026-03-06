package com.kntrel.mc.territotem.structure;

import com.kntrel.util.Vec3i;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TotemCandidatePersistentDataType Tests")
public class StructurePersistentDataTypeTest {

    private StructurePersistentDataType persistentDataType;
    private PersistentDataAdapterContext mockContext;

    @BeforeEach
    void setUp() {
        persistentDataType = StructurePersistentDataType.instance();
        mockContext = () -> null;
    }

    @Nested
    @DisplayName("Singleton Instance Tests")
    class SingletonTests {

        @Test
        @DisplayName("Should return same instance on multiple calls")
        void testSingletonInstance() {
            StructurePersistentDataType instance1 = StructurePersistentDataType.instance();
            StructurePersistentDataType instance2 = StructurePersistentDataType.instance();
            assertSame(instance1, instance2);
        }

        @Test
        @DisplayName("Should return non-null instance")
        void testInstanceNotNull() {
            assertNotNull(StructurePersistentDataType.instance());
        }
    }

    @Nested
    @DisplayName("Type Declaration Tests")
    class TypeDeclarationTests {

        @Test
        @DisplayName("getPrimitiveType() should return byte[].class")
        void testGetPrimitiveType() {
            assertEquals(byte[].class, persistentDataType.getPrimitiveType());
        }

        @Test
        @DisplayName("getComplexType() should return List class")
        void testGetComplexType() {
            Class<?> complexType = persistentDataType.getComplexType();
            assertTrue(List.class.isAssignableFrom(complexType));
        }

        @Test
        @DisplayName("getComplexType() should be consistent across multiple calls")
        void testGetComplexTypeConsistency() {
            assertEquals(persistentDataType.getComplexType(), persistentDataType.getComplexType());
        }
    }

    @Nested
    @DisplayName("toPrimitive() Single Candidate Tests")
    class ToPrimitiveSingleTests {

        @Test
        @DisplayName("Should convert single candidate with zero coordinates")
        void testToPrimitiveSingleCandidateZeroCoordinates() {
            List<StructureChunkData> candidates = new ArrayList<>();
            candidates.add(new StructureChunkData(new Vec3i(0, 0, 0), 0L));

            byte[] result = persistentDataType.toPrimitive(candidates, mockContext);

            assertEquals(12, result.length);
            assertEquals(0, result[0]); // x
            assertEquals(0, result[1]); // z
            assertEquals(0, result[2]); // y high byte
            assertEquals(0, result[3]); // y low byte
            assertEquals(0, result[4]); // blueprint id high byte
            assertEquals(0, result[11]); // blueprint id low byte
        }

        @Test
        @DisplayName("Should convert single candidate with positive coordinates")
        void testToPrimitiveSingleCandidatePositiveCoordinates() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(10, 64, 15), 12345L));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(12, result.length);
            assertEquals(10, result[0]); // x
            assertEquals(15, result[1]); // z
            // y = 64 = 0x0040
            assertEquals(0x00, result[2]); // y high byte
            assertEquals(0x40, result[3]); // y low byte
        }

        @Test
        @DisplayName("Should convert single candidate with negative coordinates")
        void testToPrimitiveSingleCandidateNegativeCoordinates() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(-50, -100, -75), 999L));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(12, result.length);
            assertEquals((byte) -50, result[0]); // x
            assertEquals((byte) -75, result[1]); // z
        }

        @Test
        @DisplayName("Should convert single candidate with large blueprint ID")
        void testToPrimitiveSingleCandidateLargeBlueprintId() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(0, 0, 0), Long.MAX_VALUE));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(12, result.length);
            // Verify that the blueprint ID (8 bytes) is correctly stored
            assertEquals(0x7F, result[4]);
            for (int i = 5; i < 12; i++) {
                assertEquals((byte) 0xFF, result[i]);
            }
        }

        @Test
        @DisplayName("Should convert single candidate with minimum blueprint ID")
        void testToPrimitiveSingleCandidateMinBlueprintId() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(5, 10, 15), Long.MIN_VALUE));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(12, result.length);
            assertEquals((byte) 0x80, result[4]);
            for (int i = 5; i < 12; i++) {
                assertEquals(0x00, result[i]);
            }
        }

        @Test
        @DisplayName("Should convert single candidate with boundary coordinate values")
        void testToPrimitiveSingleCandidateBoundaryCoordinates() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(Byte.MAX_VALUE, Short.MAX_VALUE, Byte.MIN_VALUE), 1L));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(12, result.length);
            assertEquals(Byte.MAX_VALUE, result[0]); // x
            assertEquals(Byte.MIN_VALUE, result[1]); // z
        }
    }

    @Nested
    @DisplayName("toPrimitive() Multiple Candidates Tests")
    class ToPrimitiveMultipleTests {

        @Test
        @DisplayName("Should convert two candidates correctly")
        void testToPrimitiveMultipleCandidates() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(1, 100, 2), 111L));
            data.add(new StructureChunkData(new Vec3i(3, 200, 4), 222L));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(24, result.length);
            // First candidate at offset 0-11
            assertEquals(1, result[0]);
            assertEquals(2, result[1]);
            // Second candidate at offset 12-23
            assertEquals(3, result[12]);
            assertEquals(4, result[13]);
        }

        @Test
        @DisplayName("Should convert three candidates correctly")
        void testToPrimitiveThreeCandidates() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(1, 1, 1), 1L));
            data.add(new StructureChunkData(new Vec3i(2, 2, 2), 2L));
            data.add(new StructureChunkData(new Vec3i(3, 3, 3), 3L));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(36, result.length);
            assertEquals(1, result[0]);
            assertEquals(2, result[12]);
            assertEquals(3, result[24]);
        }

        @Test
        @DisplayName("Should handle large list of candidates")
        void testToPrimitiveLargeList() {
            List<StructureChunkData> data = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                data.add(new StructureChunkData(new Vec3i(i, i * 2, i * 3), (long) i));
            }

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(1200, result.length); // 100 * 12
        }

        @Test
        @DisplayName("Should preserve order of candidates in byte array")
        void testToPrimitivePreservesOrder() {
            List<StructureChunkData> data = new ArrayList<>();
            data.add(new StructureChunkData(new Vec3i(10, 100, 110), 1000L));
            data.add(new StructureChunkData(new Vec3i(20, 200, 220), 2000L));
            data.add(new StructureChunkData(new Vec3i(30, 300, 330), 3000L));

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(10, result[0]);
            assertEquals(20, result[12]);
            assertEquals(30, result[24]);
        }
    }

    @Nested
    @DisplayName("toPrimitive() Empty List Tests")
    class ToPrimitiveEmptyTests {

        @Test
        @DisplayName("Should handle empty candidate list")
        void testToPrimitiveEmptyList() {
            List<StructureChunkData> data = new ArrayList<>();

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("Should return non-null for empty list")
        void testToPrimitiveEmptyListNotNull() {
            List<StructureChunkData> data = new ArrayList<>();

            byte[] result = persistentDataType.toPrimitive(data, mockContext);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("fromPrimitive() Single Candidate Tests")
    class FromPrimitiveSingleTests {

        @Test
        @DisplayName("Should deserialize single candidate with zero coordinates")
        void testFromPrimitiveSingleCandidateZeroCoordinates() {
            byte[] data = new byte[12];

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1, result.size());
            assertEquals(0, result.getFirst().offset().x());
            assertEquals(0, result.getFirst().offset().y());
            assertEquals(0, result.getFirst().offset().z());
            assertEquals(0L, result.getFirst().blueprintId());
        }

        @Test
        @DisplayName("Should deserialize single candidate with positive coordinates")
        void testFromPrimitiveSingleCandidatePositiveCoordinates() {
            byte[] data = new byte[12];
            data[0] = 10;   // x
            data[1] = 20;   // z
            data[2] = 0x00; // y high byte
            data[3] = 0x40; // y low byte (64)

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1, result.size());
            assertEquals(10, result.getFirst().offset().x());
            assertEquals(64, result.getFirst().offset().y());
            assertEquals(20, result.getFirst().offset().z());
        }

        @Test
        @DisplayName("Should deserialize single candidate with negative coordinates")
        void testFromPrimitiveSingleCandidateNegativeCoordinates() {
            byte[] data = new byte[12];
            data[0] = (byte) -50;   // x
            data[1] = (byte) -75;   // z
            data[2] = (byte) 0xFF;  // y high byte
            data[3] = (byte) 0x9C;  // y low byte (-100)

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1, result.size());
            assertEquals(-50, result.getFirst().offset().x());
            assertEquals(-75, result.getFirst().offset().z());
        }

        @Test
        @DisplayName("Should deserialize candidate with large blueprint ID")
        void testFromPrimitiveSingleCandidateLargeBlueprintId() {
            byte[] data = new byte[12];
            data[4] = 0x7F;
            for (int i = 5; i < 12; i++) {
                data[i] = (byte) 0xFF;
            }

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1, result.size());
            assertEquals(Long.MAX_VALUE, result.get(0).blueprintId());
        }

        @Test
        @DisplayName("Should deserialize candidate with minimum blueprint ID")
        void testFromPrimitiveSingleCandidateMinBlueprintId() {
            byte[] data = new byte[12];
            data[4] = (byte) 0x80;
            for (int i = 5; i < 12; i++) {
                data[i] = 0x00;
            }

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1, result.size());
            assertEquals(Long.MIN_VALUE, result.get(0).blueprintId());
        }
    }

    @Nested
    @DisplayName("fromPrimitive() Multiple Candidates Tests")
    class FromPrimitiveMultipleTests {

        @Test
        @DisplayName("Should deserialize two candidates correctly")
        void testFromPrimitiveMultipleCandidates() {
            byte[] data = new byte[24];
            // First candidate
            data[0] = 1;
            data[1] = 2;
            data[2] = 0x00;
            data[3] = 0x64; // 100
            // Second candidate
            data[12] = 3;
            data[13] = 4;
            data[14] = 0x00;
            data[15] = (byte) 0xC8; // 200

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).offset().x());
            assertEquals(2, result.get(0).offset().z());
            assertEquals(3, result.get(1).offset().x());
            assertEquals(4, result.get(1).offset().z());
        }

        @Test
        @DisplayName("Should deserialize three candidates correctly")
        void testFromPrimitiveThreeCandidates() {
            byte[] data = new byte[36];
            for (int i = 0; i < 3; i++) {
                int offset = i * 12;
                data[offset] = (byte) (i + 1);
            }

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(3, result.size());
            assertEquals(1, result.get(0).offset().x());
            assertEquals(2, result.get(1).offset().x());
            assertEquals(3, result.get(2).offset().x());
        }

        @Test
        @DisplayName("Should deserialize large number of candidates")
        void testFromPrimitiveLargeList() {
            byte[] data = new byte[1200]; // 100 candidates * 12 bytes each

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(100, result.size());
        }
    }

    @Nested
    @DisplayName("fromPrimitive() Empty Data Tests")
    class FromPrimitiveEmptyTests {

        @Test
        @DisplayName("Should handle empty byte array")
        void testFromPrimitiveEmptyArray() {
            byte[] data = new byte[0];

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should return empty list for empty byte array")
        void testFromPrimitiveEmptyArrayReturnsEmptyList() {
            byte[] data = new byte[0];

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("fromPrimitive() Partial Data Tests")
    class FromPrimitivePartialTests {

        @Test
        @DisplayName("Should ignore incomplete candidate at end of array")
        void testFromPrimitivePartialCandidate() {
            byte[] data = new byte[15]; // 1 full candidate (12 bytes) + 3 partial bytes

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should handle array with multiple full and partial candidate")
        void testFromPrimitiveMultipleFullPartialCandidates() {
            byte[] data = new byte[27]; // 2 full candidates (24 bytes) + 3 partial bytes

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("Round-trip Conversion Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should successfully convert single candidate to bytes and back")
        void testRoundTripSingleCandidate() {
            StructureChunkData original = new StructureChunkData(new Vec3i(10, 64, 20), 12345L);
            List<StructureChunkData> originalList = List.of(original);

            byte[] serialized = persistentDataType.toPrimitive(originalList, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(1, deserialized.size());
            assertEquals(original.offset().x(), deserialized.get(0).offset().x());
            assertEquals(original.offset().y(), deserialized.get(0).offset().y());
            assertEquals(original.offset().z(), deserialized.get(0).offset().z());
            assertEquals(original.blueprintId(), deserialized.get(0).blueprintId());
        }

        @Test
        @DisplayName("Should successfully round-trip multiple candidates")
        void testRoundTripMultipleCandidates() {
            List<StructureChunkData> original = new ArrayList<>();
            original.add(new StructureChunkData(new Vec3i(1, 100, 2), 111L));
            original.add(new StructureChunkData(new Vec3i(3, 200, 4), 222L));
            original.add(new StructureChunkData(new Vec3i(5, 300, 6), 333L));

            byte[] serialized = persistentDataType.toPrimitive(original, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(original.size(), deserialized.size());
            for (int i = 0; i < original.size(); i++) {
                assertEquals(original.get(i).offset().x(), deserialized.get(i).offset().x());
                assertEquals(original.get(i).offset().y(), deserialized.get(i).offset().y());
                assertEquals(original.get(i).offset().z(), deserialized.get(i).offset().z());
                assertEquals(original.get(i).blueprintId(), deserialized.get(i).blueprintId());
            }
        }

        @Test
        @DisplayName("Should preserve all extreme values in round-trip")
        void testRoundTripExtremeValues() {
            List<StructureChunkData> original = new ArrayList<>();
            original.add(new StructureChunkData(new Vec3i(Byte.MAX_VALUE, Short.MAX_VALUE, Byte.MAX_VALUE), Long.MAX_VALUE));
            original.add(new StructureChunkData(new Vec3i(Byte.MIN_VALUE, Short.MIN_VALUE, Byte.MIN_VALUE), Long.MIN_VALUE));
            original.add(new StructureChunkData(new Vec3i(0, 0, 0), 0L));

            byte[] serialized = persistentDataType.toPrimitive(original, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(original.size(), deserialized.size());
            for (int i = 0; i < original.size(); i++) {
                assertEquals(original.get(i).offset().x(), deserialized.get(i).offset().x());
                assertEquals(original.get(i).offset().y(), deserialized.get(i).offset().y());
                assertEquals(original.get(i).offset().z(), deserialized.get(i).offset().z());
                assertEquals(original.get(i).blueprintId(), deserialized.get(i).blueprintId());
            }
        }

        @Test
        @DisplayName("Should handle round-trip with empty list")
        void testRoundTripEmptyList() {
            List<StructureChunkData> original = new ArrayList<>();

            byte[] serialized = persistentDataType.toPrimitive(original, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(0, deserialized.size());
        }

        @Test
        @DisplayName("Should handle round-trip with large list")
        void testRoundTripLargeList() {
            List<StructureChunkData> original = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                original.add(new StructureChunkData(new Vec3i(i, i * 10, i * 2), (long) i * 1000));
            }

            byte[] serialized = persistentDataType.toPrimitive(original, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(original.size(), deserialized.size());
            for (int i = 0; i < original.size(); i++) {
                assertEquals(original.get(i).offset().x(), deserialized.get(i).offset().x());
                assertEquals(original.get(i).offset().y(), deserialized.get(i).offset().y());
                assertEquals(original.get(i).offset().z(), deserialized.get(i).offset().z());
                assertEquals(original.get(i).blueprintId(), deserialized.get(i).blueprintId());
            }
        }
    }

    @Nested
    @DisplayName("Byte Layout Verification Tests")
    class ByteLayoutTests {

        @Test
        @DisplayName("Should place x coordinate in correct byte position")
        void testByteLayoutXCoordinate() {
            byte[] data = new byte[12];
            data[0] = 42;

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(42, result.get(0).offset().x());
        }

        @Test
        @DisplayName("Should place z coordinate in correct byte position")
        void testByteLayoutZCoordinate() {
            byte[] data = new byte[12];
            data[1] = 55;

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(55, result.get(0).offset().z());
        }

        @Test
        @DisplayName("Should place y coordinate in correct byte positions")
        void testByteLayoutYCoordinate() {
            byte[] data = new byte[12];
            data[2] = 0x00;
            data[3] = 0x50; // 80 in decimal

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(80, result.get(0).offset().y());
        }

        @Test
        @DisplayName("Should place blueprint ID in correct byte positions")
        void testByteLayoutBlueprintId() {
            byte[] data = new byte[12];
            // Set blueprint ID to specific value
            data[4] = 0x00;
            data[5] = 0x00;
            data[6] = 0x00;
            data[7] = 0x00;
            data[8] = 0x00;
            data[9] = 0x00;
            data[10] = 0x04;
            data[11] = (byte) 0xD2; // 1234 in decimal

            List<StructureChunkData> result = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(1234L, result.get(0).blueprintId());
        }

        @Test
        @DisplayName("Should correctly encode all 12 bytes for single candidate")
        void testByteLayoutFullCandidate() {
            List<StructureChunkData> candidates = List.of(
                new StructureChunkData(new Vec3i(1, 1000, 2), 999999L)
            );

            byte[] serialized = persistentDataType.toPrimitive(candidates, mockContext);

            assertEquals(12, serialized.length);
            assertEquals(1, serialized[0]); // x
            assertEquals(2, serialized[1]); // z
            // Verify bytes 2-3 contain y value
            // Verify bytes 4-11 contain blueprint ID
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should not corrupt data when serializing multiple times")
        void testSerializeMultipleTimes() {
            StructureChunkData candidate = new StructureChunkData(new Vec3i(10, 20, 30), 40L);
            List<StructureChunkData> candidates = List.of(candidate);

            byte[] result1 = persistentDataType.toPrimitive(candidates, mockContext);
            byte[] result2 = persistentDataType.toPrimitive(candidates, mockContext);

            assertArrayEquals(result1, result2);
        }

        @Test
        @DisplayName("Should not corrupt data when deserializing multiple times")
        void testDeserializeMultipleTimes() {
            byte[] data = new byte[12];
            data[0] = 11;
            data[1] = 22;
            data[2] = 0x00;
            data[3] = 0x33;

            List<StructureChunkData> result1 = persistentDataType.fromPrimitive(data, mockContext);
            List<StructureChunkData> result2 = persistentDataType.fromPrimitive(data, mockContext);

            assertEquals(result1.get(0).offset().x(), result2.get(0).offset().x());
            assertEquals(result1.get(0).offset().z(), result2.get(0).offset().z());
        }

        @Test
        @DisplayName("Should preserve data when candidates are added in different order")
        void testOrderPreservation() {
            List<StructureChunkData> data1 = new ArrayList<>();
            data1.add(new StructureChunkData(new Vec3i(1, 1, 1), 1L));
            data1.add(new StructureChunkData(new Vec3i(2, 2, 2), 2L));

            List<StructureChunkData> data2 = new ArrayList<>();
            data2.add(new StructureChunkData(new Vec3i(2, 2, 2), 2L));
            data2.add(new StructureChunkData(new Vec3i(1, 1, 1), 1L));

            byte[] serialized1 = persistentDataType.toPrimitive(data1, mockContext);
            byte[] serialized2 = persistentDataType.toPrimitive(data2, mockContext);

            assertNotEquals(serialized1[0], serialized2[0]);
        }
    }

    @Nested
    @DisplayName("Special Coordinate Tests")
    class SpecialCoordinateTests {

        @ParameterizedTest
        @ValueSource(ints = {-128, -1, 0, 1, 127})
        @DisplayName("Should handle boundary byte values for x coordinate")
        void testBoundaryXCoordinates(int x) {
            List<StructureChunkData> data = List.of(
                new StructureChunkData(new Vec3i(x, 0, 0), 0L)
            );

            byte[] serialized = persistentDataType.toPrimitive(data, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(x, deserialized.get(0).offset().x());
        }

        @ParameterizedTest
        @ValueSource(ints = {-128, -1, 0, 1, 127})
        @DisplayName("Should handle boundary byte values for z coordinate")
        void testBoundaryZCoordinates(int z) {
            List<StructureChunkData> data = List.of(
                new StructureChunkData(new Vec3i(0, 0, z), 0L)
            );

            byte[] serialized = persistentDataType.toPrimitive(data, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(z, deserialized.get(0).offset().z());
        }

        @Test
        @DisplayName("Should handle coordinate with mixed signs")
        void testMixedSignCoordinates() {
            List<StructureChunkData> data = List.of(
                new StructureChunkData(new Vec3i(-50, 100, 50), 12345L)
            );

            byte[] serialized = persistentDataType.toPrimitive(data, mockContext);
            List<StructureChunkData> deserialized = persistentDataType.fromPrimitive(serialized, mockContext);

            assertEquals(-50, deserialized.get(0).offset().x());
            assertEquals(100, deserialized.get(0).offset().y());
            assertEquals(50, deserialized.get(0).offset().z());
        }
    }
}
