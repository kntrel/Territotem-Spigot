package com.kntrel.mc.territotem.totem.deeds;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DeedsPersistentDataType Tests")
class DeedsPersistentDataTypeTest {

    private DeedsPersistentDataType persistentDataType;
    private PersistentDataAdapterContext mockContext;

    @BeforeEach
    void setUp() {
        this.persistentDataType = DeedsPersistentDataType.instance();
        this.mockContext = () -> null;
    }

    @Test
    @DisplayName("Singleton factory returns same instance")
    void singletonFactoryReturnsSameInstance() {
        assertSame(DeedsPersistentDataType.instance(), DeedsPersistentDataType.instance());
    }

    @Test
    @DisplayName("Type declarations are correct")
    void typeDeclarationsAreCorrect() {
        assertEquals(byte[].class, this.persistentDataType.getPrimitiveType());
        assertEquals(DeedsPersistentData.class, this.persistentDataType.getComplexType());
    }

    @Test
    @DisplayName("Round-trip preserves namespace region id and version")
    void roundTripPreservesNamespaceRegionIdAndVersion() {
        DeedsPersistentData original = new DeedsPersistentData("territotem", 987654321L, 3);

        byte[] serialized = this.persistentDataType.toPrimitive(original, this.mockContext);
        DeedsPersistentData restored = this.persistentDataType.fromPrimitive(serialized, this.mockContext);

        assertEquals(original, restored);
    }

    @Test
    @DisplayName("Trailing bytes are ignored during deserialization")
    void trailingBytesAreIgnoredDuringDeserialization() {
        DeedsPersistentData original = new DeedsPersistentData("claims", 41L, 7);

        byte[] serialized = this.persistentDataType.toPrimitive(original, this.mockContext);
        byte[] padded = new byte[serialized.length + 3];
        System.arraycopy(serialized, 0, padded, 0, serialized.length);

        DeedsPersistentData restored = this.persistentDataType.fromPrimitive(padded, this.mockContext);

        assertEquals(original, restored);
        assertArrayEquals(serialized, java.util.Arrays.copyOf(padded, serialized.length));
    }

    @Test
    @DisplayName("Short payloads are rejected")
    void shortPayloadsAreRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.persistentDataType.fromPrimitive(new byte[15], this.mockContext)
        );

        assertEquals("Not enough bytes to deserialize deeds data", exception.getMessage());
    }
}
