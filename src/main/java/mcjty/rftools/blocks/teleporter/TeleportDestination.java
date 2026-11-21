package mcjty.rftools.blocks.teleporter;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TeleportDestination {
    private final BlockPos coordinate;
    private final int dimension;
    private String name = "";

    // 接收器访问设置的副本（只在服务端用，不进网络包），让列表、设目标时不必为了查权限去加载接收器所在的区块。
    // accessKnown 为 false 表示还没从接收器同步过（旧存档）。
    private boolean accessKnown = false;
    private boolean privateAccess = false;
    private final Set<String> allowedPlayers = new HashSet<>();

    public TeleportDestination(ByteBuf buf) {
        int cx = buf.readInt();
        int cy = buf.readInt();
        int cz = buf.readInt();
        if (cx == -1 && cy == -1 && cz == -1) {
            coordinate = null;
        } else {
            coordinate = new BlockPos(cx, cy, cz);
        }
        dimension = buf.readInt();
        setName(NetworkTools.readString(buf));
    }

    public TeleportDestination(BlockPos coordinate, int dimension) {
        this.coordinate = coordinate;
        this.dimension = dimension;
    }

    public boolean isValid() {
        return coordinate != null;
    }

    public void toBytes(ByteBuf buf) {
        if (coordinate == null) {
            buf.writeInt(-1);
            buf.writeInt(-1);
            buf.writeInt(-1);
        } else {
            buf.writeInt(coordinate.getX());
            buf.writeInt(coordinate.getY());
            buf.writeInt(coordinate.getZ());
        }
        buf.writeInt(dimension);
        NetworkTools.writeString(buf, getName());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = "";
        } else {
            this.name = name;
        }
    }

    public void setAccess(boolean privateAccess, Collection<String> allowedPlayers) {
        this.accessKnown = true;
        this.privateAccess = privateAccess;
        this.allowedPlayers.clear();
        this.allowedPlayers.addAll(allowedPlayers);
    }

    public boolean isAccessKnown() {
        return accessKnown;
    }

    public boolean isPrivateAccess() {
        return privateAccess;
    }

    public Set<String> getAllowedPlayers() {
        return Collections.unmodifiableSet(allowedPlayers);
    }

    /** 按副本判断这名玩家能不能用这个接收器（副本未知时返回 null）。 */
    public Boolean checkCachedAccess(String player) {
        if (!accessKnown) {
            return null;
        }
        return !privateAccess || allowedPlayers.contains(player);
    }

    public BlockPos getCoordinate() {
        return coordinate;
    }

    public int getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TeleportDestination that = (TeleportDestination) o;

        if (dimension != that.dimension) {
            return false;
        }
        if (coordinate != null ? !coordinate.equals(that.coordinate) : that.coordinate != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = coordinate != null ? coordinate.hashCode() : 0;
        result = 31 * result + dimension;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
