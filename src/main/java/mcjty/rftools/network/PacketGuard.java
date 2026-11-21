package mcjty.rftools.network;

import mcjty.lib.container.ContainerFactory;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.varia.GlobalCoordinate;
import mcjty.lib.varia.Logging;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import mcjty.rftools.protection.Protection;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * 封包守卫（仅服务端主线程）：客户端发来的、要操作某台机器的包，先经这里检查，通过了才交给机器处理。
 * <ol>
 *     <li>区块已加载（绝不因为封包去加载或生成区块）；</li>
 *     <li>玩家正开着这台机器的界面（服务端的 {@code openContainer} 对应这个位置，包括用平板远程打开的存储扫描器），
 *     或者人在 {@link #MAX_DISTANCE} 格之内且在同一个世界；</li>
 *     <li>机器的安全卡 / 拥有者权限（与右键打开界面时的检查相同，但不发提示）；</li>
 *     <li>领地保护（{@link Protection}，像右键这个方块一样问一次）。允许的结果记 {@link #GRANT_TTL} 刻。</li>
 * </ol>
 * 只读的显示数据（屏幕内容、HUD）用 {@link #viewable}，只检查 1 和距离。
 * 守卫只管“能不能碰这台机器”；每个包、每条命令的参数（坐标范围、下标、数量、长度）仍由各自的处理代码检查。
 */
public final class PacketGuard {

    /** 不开界面、直接在世界里操作（点屏幕等）时，玩家与机器方块中心的最大距离（格）。 */
    public static final double MAX_DISTANCE = 10.0;
    /** 领地检查允许后记住多少刻。 */
    public static final long GRANT_TTL = 100;

    /** 服务端打开的界面 → 它对应的机器位置（由 GuiProxy 在打开时登记）。 */
    private static final Map<Container, GlobalCoordinate> OPEN_GUIS = new WeakHashMap<>();
    private static final GrantCache<GrantKey> GRANTS = new GrantCache<>(4096, GRANT_TTL);
    private static final GrantCache<UUID> WARNED = new GrantCache<>(1024, 200);

    /** 正在处理的封包是谁发的（只在 {@link #runAs} 期间有值），给拿不到玩家对象的老接口（executeWithResult）用。 */
    @Nullable
    private static EntityPlayerMP current = null;

    private PacketGuard() {
    }

    /** GuiProxy 在服务端打开一台机器的界面时调用。 */
    public static void rememberGui(@Nullable Object container, int dimension, BlockPos pos) {
        if (container instanceof Container && pos != null) {
            OPEN_GUIS.put((Container) container, new GlobalCoordinate(pos, dimension));
        }
    }

    /**
     * 完整的守卫：通过时返回这个位置的 TileEntity，否则返回 null。
     */
    @Nullable
    public static TileEntity machine(@Nullable EntityPlayerMP player, @Nullable World world, @Nullable BlockPos pos) {
        if (player == null || world == null || pos == null || world.isRemote) {
            return null;
        }
        if (!world.isBlockLoaded(pos)) {
            reject(player, "unloaded", world, pos);
            return null;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return null;
        }
        if (!hasGuiOpen(player, world, pos, te) && !inReach(player, world, pos, MAX_DISTANCE)) {
            reject(player, "not at machine", world, pos);
            return null;
        }
        if (GenericRFToolsBlock.isAccessDenied(world, player, te)) {
            reject(player, "security", world, pos);
            return null;
        }
        if (!protectionAllows(player, world, pos)) {
            reject(player, "protection", world, pos);
            return null;
        }
        return te;
    }

    /** 同 {@link #machine}，并且要求 TileEntity 是 {@code type}。 */
    @Nullable
    public static <T> T machine(@Nullable EntityPlayerMP player, @Nullable World world, @Nullable BlockPos pos, Class<T> type) {
        TileEntity te = machine(player, world, pos);
        return type.isInstance(te) ? type.cast(te) : null;
    }

    /**
     * 只读的显示数据：区块已加载、同一个世界、在 {@code maxDistance} 格内。
     */
    @Nullable
    public static TileEntity viewable(@Nullable EntityPlayerMP player, @Nullable World world, @Nullable BlockPos pos, double maxDistance) {
        if (player == null || world == null || pos == null || world.isRemote) {
            return null;
        }
        if (!world.isBlockLoaded(pos) || !inReach(player, world, pos, maxDistance)) {
            return null;
        }
        return world.getTileEntity(pos);
    }

    /** 玩家开着的界面是不是这台机器的。 */
    public static boolean hasGuiOpen(EntityPlayerMP player, World world, BlockPos pos, @Nullable TileEntity te) {
        Container container = player.openContainer;
        if (container == null || container == player.inventoryContainer) {
            return false;
        }
        GlobalCoordinate gc = OPEN_GUIS.get(container);
        if (gc != null && gc.getDimension() == world.provider.getDimension() && gc.getCoordinate().equals(pos)) {
            return true;
        }
        if (te != null && container instanceof GenericContainer) {
            return ((GenericContainer) container).getInventory(ContainerFactory.CONTAINER_CONTAINER) == te;
        }
        return false;
    }

    public static boolean inReach(EntityPlayerMP player, World world, BlockPos pos, double maxDistance) {
        if (player.world != world) {
            return false;
        }
        return GuardRules.withinDistance(player.posX, player.posY + player.getEyeHeight(), player.posZ, pos, maxDistance);
    }

    /** 领地保护：允许过的在 {@link #GRANT_TTL} 刻内不再问。 */
    public static boolean protectionAllows(EntityPlayerMP player, World world, BlockPos pos) {
        GrantKey key = new GrantKey(player.getUniqueID(), world.provider.getDimension(), pos.toLong());
        long now = now();
        if (GRANTS.isGranted(key, now)) {
            return true;
        }
        if (Protection.canUse(player, world, pos)) {
            GRANTS.grant(key, now);
            return true;
        }
        return false;
    }

    /** 以这名玩家的身份执行（供 {@link #currentPlayer()} 取用）。 */
    public static <T> T runAs(EntityPlayerMP player, Supplier<T> action) {
        EntityPlayerMP outer = current;
        current = player;
        try {
            return action.get();
        } finally {
            current = outer;
        }
    }

    public static void runAs(EntityPlayerMP player, Runnable action) {
        runAs(player, () -> {
            action.run();
            return null;
        });
    }

    /** 正在处理的封包的发送者；不在封包处理里时为 null。 */
    @Nullable
    public static EntityPlayerMP currentPlayer() {
        return current;
    }

    /** 服务端停下时清掉。 */
    public static void clear() {
        OPEN_GUIS.clear();
        GRANTS.clear();
        WARNED.clear();
        current = null;
    }

    static void reject(EntityPlayerMP player, String reason, World world, BlockPos pos) {
        long now = now();
        if (!WARNED.isGranted(player.getUniqueID(), now)) {
            WARNED.grant(player.getUniqueID(), now);
            Logging.log("RFTools: ignored a request from " + player.getName() + " for " + pos + " in dimension "
                    + world.provider.getDimension() + " (" + reason + ")");
        }
    }

    private static long now() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server == null ? 0 : server.getTickCounter();
    }

    private static final class GrantKey {
        private final UUID player;
        private final int dimension;
        private final long pos;

        GrantKey(UUID player, int dimension, long pos) {
            this.player = player;
            this.dimension = dimension;
            this.pos = pos;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GrantKey)) {
                return false;
            }
            GrantKey that = (GrantKey) o;
            return dimension == that.dimension && pos == that.pos && player.equals(that.player);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player, dimension, pos);
        }
    }
}
