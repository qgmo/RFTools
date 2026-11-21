package mcjty.rftools.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrantCacheTest {

    @Test
    void grantExpiresAfterTtl() {
        GrantCache<String> cache = new GrantCache<>(10, 100);
        assertFalse(cache.isGranted("a", 0));
        cache.grant("a", 1000);
        assertTrue(cache.isGranted("a", 1000));
        assertTrue(cache.isGranted("a", 1099));
        assertFalse(cache.isGranted("a", 1100));
        // 过期后被删掉
        assertEquals(0, cache.size());
    }

    @Test
    void clockGoingBackwardsInvalidates() {
        GrantCache<String> cache = new GrantCache<>(10, 100);
        cache.grant("a", 5000);
        assertFalse(cache.isGranted("a", 10));
    }

    @Test
    void oldestEntriesAreDroppedWhenFull() {
        GrantCache<Integer> cache = new GrantCache<>(3, 100);
        for (int i = 0; i < 5; i++) {
            cache.grant(i, 0);
        }
        assertEquals(3, cache.size());
        assertFalse(cache.isGranted(0, 1));
        assertFalse(cache.isGranted(1, 1));
        assertTrue(cache.isGranted(4, 1));
    }

    @Test
    void clearRemovesEverything() {
        GrantCache<String> cache = new GrantCache<>(3, 100);
        cache.grant("a", 0);
        cache.clear();
        assertFalse(cache.isGranted("a", 1));
    }
}
