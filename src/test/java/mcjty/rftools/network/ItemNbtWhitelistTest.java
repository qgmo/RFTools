package mcjty.rftools.network;

import mcjty.lib.typed.Type;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ItemNbtWhitelistTest {

    private final ItemNbtWhitelist tablet = ItemNbtWhitelist.builder()
            .string("sortMode", 32).string("viewMode", 32).string("filter", 256).bool("groupMode")
            .build();

    @Test
    void acceptsGuiKeys() {
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(tablet.apply(tag, "sortMode", Type.STRING, "name"));
        assertTrue(tablet.apply(tag, "groupMode", Type.BOOLEAN, true));
        assertEquals("name", tag.getString("sortMode"));
        assertTrue(tag.getBoolean("groupMode"));
    }

    @Test
    void rejectsOtherKeys() {
        NBTTagCompound tag = new NBTTagCompound();
        // 以前能写的：远程存储编号、扫描器坐标、能量、扩容
        assertFalse(tablet.apply(tag, "id", Type.INTEGER, 7));
        assertFalse(tablet.apply(tag, "monitorx", Type.INTEGER, 100));
        assertFalse(tablet.apply(tag, "Energy", Type.INTEGER, Integer.MAX_VALUE));
        assertFalse(tablet.apply(tag, "childDamage", Type.INTEGER, 2));
        assertTrue(tag.getKeySet().isEmpty());
    }

    @Test
    void rejectsWrongType() {
        NBTTagCompound tag = new NBTTagCompound();
        assertFalse(tablet.apply(tag, "groupMode", Type.STRING, "true"));
        assertFalse(tablet.apply(tag, "sortMode", Type.INTEGER, 1));
        assertTrue(tag.getKeySet().isEmpty());
    }

    @Test
    void truncatesLongStrings() {
        char[] chars = new char[1000];
        Arrays.fill(chars, 'a');
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(tablet.apply(tag, "filter", Type.STRING, new String(chars)));
        assertEquals(256, tag.getString("filter").length());
        assertTrue(tablet.apply(tag, "sortMode", Type.STRING, new String(chars)));
        assertEquals(32, tag.getString("sortMode").length());
    }
}
