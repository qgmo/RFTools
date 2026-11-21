package mcjty.rftools.blocks.teleporter;

import mcjty.lib.varia.GlobalCoordinate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class TeleportDestinationsTest {

    private static GlobalCoordinate gc(int x, int dim) {
        return new GlobalCoordinate(new BlockPos(x, 64, 0), dim);
    }

    @Test
    void cleanupInvalidRemovesIdMappingsToo() {
        TeleportDestinations destinations = new TeleportDestinations("test");
        GlobalCoordinate a = gc(1, 0);
        GlobalCoordinate b = gc(2, -1);
        int idA = destinations.getNewId(a);
        int idB = destinations.getNewId(b);
        destinations.addDestination(a);
        destinations.addDestination(b);

        destinations.cleanupInvalid(key -> key.getDimension() == -1);

        assertNotNull(destinations.getDestination(a));
        assertEquals(a, destinations.getCoordinateForId(idA));
        assertNull(destinations.getDestination(b));
        // 原来这里还能查到坐标，充能传送器按编号找到坐标却找不到接收器，会空指针崩服
        assertNull(destinations.getCoordinateForId(idB));
        assertNull(destinations.getIdForCoordinate(b));
        assertTrue(destinations.isConsistent());
    }

    @Test
    void accessCopyIsSavedAndLoaded() {
        TeleportDestinations destinations = new TeleportDestinations("test");
        GlobalCoordinate a = gc(1, 0);
        GlobalCoordinate b = gc(2, 0);
        destinations.getNewId(a);
        destinations.getNewId(b);
        destinations.addDestination(a).setAccess(true, Arrays.asList("alice"));
        destinations.addDestination(b);

        NBTTagCompound tag = destinations.writeToNBT(new NBTTagCompound());
        TeleportDestinations loaded = new TeleportDestinations("test");
        loaded.readFromNBT(tag);

        TeleportDestination da = loaded.getDestination(a);
        assertTrue(da.isAccessKnown());
        assertEquals(Boolean.TRUE, da.checkCachedAccess("alice"));
        assertEquals(Boolean.FALSE, da.checkCachedAccess("mallory"));
        // 旧存档里没有副本的：未知
        assertNull(loaded.getDestination(b).checkCachedAccess("alice"));
        assertTrue(loaded.isConsistent());
    }

    @Test
    void publicReceiverAllowsEveryone() {
        TeleportDestination d = new TeleportDestination(new BlockPos(0, 0, 0), 0);
        d.setAccess(false, Collections.emptyList());
        assertEquals(Boolean.TRUE, d.checkCachedAccess("anyone"));
    }
}
