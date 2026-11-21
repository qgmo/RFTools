package mcjty.rftools.network;

import net.minecraft.util.math.BlockPos;

/**
 * 守卫与参数检查里用到的纯计算（不碰世界），方便单元测试。
 */
public final class GuardRules {

    private GuardRules() {
    }

    /** 点 (x, y, z) 与方块中心的距离是否不超过 {@code max}。 */
    public static boolean withinDistance(double x, double y, double z, BlockPos pos, double max) {
        double dx = x - (pos.getX() + 0.5);
        double dy = y - (pos.getY() + 0.5);
        double dz = z - (pos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= max * max;
    }

    /** {@code p} 是否在以 {@code center} 为中心、水平半径 {@code h}、垂直半径 {@code v} 的箱体内。 */
    public static boolean inBox(BlockPos center, BlockPos p, int h, int v) {
        return Math.abs(p.getX() - center.getX()) <= h
                && Math.abs(p.getZ() - center.getZ()) <= h
                && Math.abs(p.getY() - center.getY()) <= v;
    }

    /** 从 {@code last} 到 {@code now} 是否已经过了至少 {@code cooldown} 刻（时间由调用方给，例如世界刻数）。 */
    public static boolean cooledDown(long last, long now, long cooldown) {
        return now - last >= cooldown;
    }

    /** 两点距离的平方是否不超过 {@code max}²。 */
    public static boolean withinBlocks(BlockPos a, BlockPos b, double max) {
        return a.distanceSq(b) <= max * max;
    }
}
