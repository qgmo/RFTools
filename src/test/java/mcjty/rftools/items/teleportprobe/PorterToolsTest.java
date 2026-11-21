package mcjty.rftools.items.teleportprobe;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PorterToolsTest {

    @Test
    void onlyStoredTargetsCanBeSelected() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("target0", 12);
        tag.setInteger("target7", 30);
        assertTrue(PorterTools.isStoredTarget(tag, 12));
        assertTrue(PorterTools.isStoredTarget(tag, 30));
        // 任意接收器编号（例如别人的私有接收器）不行
        assertFalse(PorterTools.isStoredTarget(tag, 13));
        // target8 超出 MAXTARGETS，不算
        tag.setInteger("target8", 99);
        assertFalse(PorterTools.isStoredTarget(tag, 99));
        assertFalse(PorterTools.isStoredTarget(new NBTTagCompound(), 0));
    }
}
