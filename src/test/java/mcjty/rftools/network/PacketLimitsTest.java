package mcjty.rftools.network;

import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PacketLimitsTest {

    @Test
    void declaredLengthWithinLimitsIsAccepted() {
        assertEquals(10, PacketLimits.checkDeclaredLength(10, 10, 100, 1));
        assertEquals(0, PacketLimits.checkDeclaredLength(0, 0, 100, 8));
    }

    @Test
    void negativeLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkDeclaredLength(-5, 100, 100, 1));
    }

    @Test
    void lengthAboveMaximumIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkDeclaredLength(2_000_000_000, Integer.MAX_VALUE, PacketLimits.MAX_STRING_BYTES, 1));
    }

    @Test
    void lengthAboveRemainingBytesIsRejected() {
        // 声明 100 项、每项至少 8 字节，但包里只剩 799 字节
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkDeclaredLength(100, 799, 1024, 8));
        assertEquals(100, PacketLimits.checkDeclaredLength(100, 800, 1024, 8));
    }

    @Test
    void entrySizeProductDoesNotOverflow() {
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkDeclaredLength(1000, 1000, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void truncateCutsLongStringsOnly() {
        assertEquals("abc", PacketLimits.truncate("abcdef", 3));
        assertEquals("ab", PacketLimits.truncate("ab", 3));
        assertNull(PacketLimits.truncate(null, 3));
    }

    @Test
    void clampAndIndex() {
        assertEquals(20, PacketLimits.clamp(100000, 0, 20));
        assertEquals(0, PacketLimits.clamp(-3, 0, 20));
        assertEquals(7, PacketLimits.clamp(7, 0, 20));
        assertTrue(PacketLimits.validIndex(0, 1));
        assertFalse(PacketLimits.validIndex(1, 1));
        assertFalse(PacketLimits.validIndex(-1, 5));
    }

    @Test
    void stringsWithinChecksStringsAndStringLists() {
        char[] longChars = new char[300];
        Arrays.fill(longChars, 'x');
        String longString = new String(longChars);
        TypedMap ok = TypedMap.builder().put(new Key<>("name", Type.STRING), "short").put(new Key<>("n", Type.INTEGER), 5).build();
        TypedMap bad = TypedMap.builder().put(new Key<>("name", Type.STRING), longString).build();
        TypedMap badList = TypedMap.builder().put(new Key<>("list", Type.STRING_LIST), Arrays.asList("a", longString)).build();
        assertTrue(PacketLimits.stringsWithin(ok, 256));
        assertFalse(PacketLimits.stringsWithin(bad, 256));
        assertFalse(PacketLimits.stringsWithin(badList, 256));
        assertTrue(PacketLimits.stringsWithin(null, 256));
    }
}
