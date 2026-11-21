package mcjty.rftools.network;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardRulesTest {

    @Test
    void distanceIsMeasuredToBlockCenter() {
        BlockPos pos = new BlockPos(0, 64, 0);
        assertTrue(GuardRules.withinDistance(0.5, 64.5, 10.5, pos, 10.0));
        assertFalse(GuardRules.withinDistance(0.5, 64.5, 10.6, pos, 10.0));
        assertFalse(GuardRules.withinDistance(5000, 64, 0, pos, PacketGuard.MAX_DISTANCE));
    }

    @Test
    void boxUsesSeparateHorizontalAndVerticalRange() {
        BlockPos center = new BlockPos(100, 64, 100);
        assertTrue(GuardRules.inBox(center, new BlockPos(110, 69, 90), 10, 5));
        assertFalse(GuardRules.inBox(center, new BlockPos(111, 64, 100), 10, 5));
        assertFalse(GuardRules.inBox(center, new BlockPos(100, 70, 100), 10, 5));
        assertFalse(GuardRules.inBox(center, new BlockPos(100, 64, 1_000_000), 10, 5));
        // 监视器：只允许相邻的 3×3×3
        assertTrue(GuardRules.inBox(center, new BlockPos(101, 63, 99), 1, 1));
        assertFalse(GuardRules.inBox(center, new BlockPos(102, 64, 100), 1, 1));
    }

    @Test
    void cooldown() {
        assertFalse(GuardRules.cooledDown(100, 139, 40));
        assertTrue(GuardRules.cooledDown(100, 140, 40));
        assertTrue(GuardRules.cooledDown(Long.MIN_VALUE / 2, 0, 40));
    }
}
