package mcjty.rftools.items.builder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShapeCardNbtSanitizerTest {

    private final ShapeCardNbtSanitizer sanitizer = new ShapeCardNbtSanitizer(s -> s.equals("Box") || s.equals("Sphere"), 512, 2048);

    @Test
    void acceptsGuiKeysAndClamps() {
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(sanitizer.apply(tag, "dimX", 100000));
        assertTrue(sanitizer.apply(tag, "dimY", 100000));
        assertTrue(sanitizer.apply(tag, "dimZ", -5));
        assertTrue(sanitizer.apply(tag, "offsetX", -999999));
        assertTrue(sanitizer.apply(tag, "offsetY", 12));
        assertTrue(sanitizer.apply(tag, "solid", true));
        assertTrue(sanitizer.apply(tag, "voidstone", true));
        assertTrue(sanitizer.apply(tag, "shapenew", "Sphere"));
        assertEquals(512, tag.getInteger("dimX"));
        assertEquals(256, tag.getInteger("dimY"));
        assertEquals(0, tag.getInteger("dimZ"));
        assertEquals(-2048, tag.getInteger("offsetX"));
        assertEquals(12, tag.getInteger("offsetY"));
        assertEquals("Sphere", tag.getString("shapenew"));
    }

    @Test
    void rejectsKeysOutsideTheShapeCardGui() {
        NBTTagCompound tag = new NBTTagCompound();
        // 审查里列出的利用方式：任意物品满电、无法破坏、远程存储编号、安全频道、注射器、传送目标
        assertFalse(sanitizer.apply(tag, "charge", 1e9));
        assertFalse(sanitizer.apply(tag, "Unbreakable", true));
        assertFalse(sanitizer.apply(tag, "Energy", Integer.MAX_VALUE));
        assertFalse(sanitizer.apply(tag, "id", 3));
        assertFalse(sanitizer.apply(tag, "channel", 3));
        assertFalse(sanitizer.apply(tag, "mobId", "minecraft:wither"));
        assertFalse(sanitizer.apply(tag, "target", 1));
        assertTrue(tag.getKeySet().isEmpty());
    }

    @Test
    void rejectsWrongTypesAndUnknownShapes() {
        NBTTagCompound tag = new NBTTagCompound();
        assertFalse(sanitizer.apply(tag, "dimX", "12"));
        assertFalse(sanitizer.apply(tag, "solid", 1));
        assertFalse(sanitizer.apply(tag, "shapenew", "NoSuchShape"));
        assertFalse(sanitizer.apply(tag, "shapenew", 5));
        assertTrue(tag.getKeySet().isEmpty());
    }

    @Test
    void mergeKeepsServerDataAndIgnoresInjectedKeys() {
        NBTTagCompound current = new NBTTagCompound();
        current.setInteger("dimX", 10);
        current.setTag("children", new NBTTagList());
        current.setInteger("scanid", 77);

        NBTTagCompound fromClient = new NBTTagCompound();
        fromClient.setInteger("dimX", 20);
        fromClient.setInteger("offsetZ", 999999);
        fromClient.setInteger("scanid", 1);
        fromClient.setInteger("channel", 5);
        fromClient.setString("padding", "xxxxxxxxxxxxxxxxxxxx");

        NBTTagCompound merged = sanitizer.merge(current, fromClient);
        assertEquals(20, merged.getInteger("dimX"));
        assertEquals(2048, merged.getInteger("offsetZ"));
        assertEquals(77, merged.getInteger("scanid"));
        assertTrue(merged.hasKey("children"));
        assertFalse(merged.hasKey("channel"));
        assertFalse(merged.hasKey("padding"));
        // 原来的 NBT 不被改动
        assertEquals(10, current.getInteger("dimX"));
    }
}
