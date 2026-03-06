package com.kntrel.mc.territotem.structure;

import com.kntrel.util.Vec3i;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StructurePersistentDataType Tests")
class StructurePersistentDataTypeTest {

    private static final int CANDIDATE_BYTES = 28;

    private StructurePersistentDataType persistentDataType;
    private PersistentDataAdapterContext mockContext;

    @BeforeEach
    void setUp() {
        this.persistentDataType = StructurePersistentDataType.instance();
        this.mockContext = () -> null;
    }

    @Test
    @DisplayName("Singleton factory returns same instance")
    void singletonFactoryReturnsSameInstance() {
        assertSame(StructurePersistentDataType.instance(), StructurePersistentDataType.instance());
    }

    @Test
    @DisplayName("Type declarations are correct")
    void typeDeclarationsAreCorrect() {
        assertEquals(byte[].class, this.persistentDataType.getPrimitiveType());
        assertTrue(List.class.isAssignableFrom(this.persistentDataType.getComplexType()));
    }

    @Test
    @DisplayName("Serialize single candidate into 28-byte record")
    void serializeSingleCandidateInto28Bytes() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        List<StructureChunkData> data = List.of(
                new StructureChunkData(new Vec3i(10, 64, 20), 12345L, id)
        );

        byte[] serialized = this.persistentDataType.toPrimitive(data, this.mockContext);

        assertEquals(CANDIDATE_BYTES, serialized.length);
        assertEquals((byte) 10, serialized[0]);
        assertEquals((byte) 20, serialized[1]);
    }

    @Test
    @DisplayName("Serialize multiple candidates with fixed 28-byte stride")
    void serializeMultipleCandidatesWithFixedStride() {
        List<StructureChunkData> data = List.of(
                new StructureChunkData(new Vec3i(1, 100, 2), 11L, UUID.randomUUID()),
                new StructureChunkData(new Vec3i(3, 200, 4), 22L, UUID.randomUUID())
        );

        byte[] serialized = this.persistentDataType.toPrimitive(data, this.mockContext);

        assertEquals(CANDIDATE_BYTES * 2, serialized.length);
        assertEquals((byte) 1, serialized[0]);
        assertEquals((byte) 3, serialized[CANDIDATE_BYTES]);
    }

    @Test
    @DisplayName("Deserialize single 28-byte candidate")
    void deserializeSingleCandidate() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        StructureChunkData original = new StructureChunkData(new Vec3i(-7, 255, 9), Long.MAX_VALUE, id);

        byte[] serialized = this.persistentDataType.toPrimitive(List.of(original), this.mockContext);
        List<StructureChunkData> restored = this.persistentDataType.fromPrimitive(serialized, this.mockContext);

        assertEquals(1, restored.size());
        assertEquals(original.offset().x(), restored.get(0).offset().x());
        assertEquals(original.offset().y(), restored.get(0).offset().y());
        assertEquals(original.offset().z(), restored.get(0).offset().z());
        assertEquals(original.blueprintId(), restored.get(0).blueprintId());
        assertEquals(original.structureId(), restored.get(0).structureId());
    }

    @Test
    @DisplayName("Round-trip preserves unassigned UUID sentinel")
    void roundTripPreservesUnassignedUuidSentinel() {
        StructureChunkData original = new StructureChunkData(new Vec3i(0, 0, 0), 0L);

        byte[] serialized = this.persistentDataType.toPrimitive(List.of(original), this.mockContext);
        List<StructureChunkData> restored = this.persistentDataType.fromPrimitive(serialized, this.mockContext);

        assertEquals(1, restored.size());
        assertEquals(StructureChunkData.UNASSIGNED_ID, restored.get(0).structureId());
    }

    @Test
    @DisplayName("fromPrimitive ignores trailing partial bytes")
    void fromPrimitiveIgnoresTrailingPartialBytes() {
        byte[] oneAndHalf = new byte[CANDIDATE_BYTES + 10];

        List<StructureChunkData> restored = this.persistentDataType.fromPrimitive(oneAndHalf, this.mockContext);

        assertEquals(1, restored.size());
    }

    @Test
    @DisplayName("Empty inputs are handled")
    void emptyInputsAreHandled() {
        byte[] serialized = this.persistentDataType.toPrimitive(List.of(), this.mockContext);
        List<StructureChunkData> restored = this.persistentDataType.fromPrimitive(new byte[0], this.mockContext);

        assertNotNull(serialized);
        assertEquals(0, serialized.length);
        assertNotNull(restored);
        assertTrue(restored.isEmpty());
    }
}
