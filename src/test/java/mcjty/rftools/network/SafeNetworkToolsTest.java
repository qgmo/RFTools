package mcjty.rftools.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.network.TypedMapTools;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 服务端的有上限解码与 McJtyLib 的编码格式一致，并且拒绝客户端声明的超长长度。 */
class SafeNetworkToolsTest {

    @Test
    void readsWhatMcJtyLibWrites() {
        Key<String> name = new Key<>("name", Type.STRING);
        Key<Integer> count = new Key<>("count", Type.INTEGER);
        Key<Boolean> flag = new Key<>("flag", Type.BOOLEAN);
        Key<BlockPos> pos = new Key<>("pos", Type.BLOCKPOS);
        Key<List<String>> list = new Key<>("list", Type.STRING_LIST);
        Key<Long> energy = new Key<>("energy", Type.LONG);
        TypedMap map = TypedMap.builder()
                .put(name, "receiver")
                .put(count, 42)
                .put(flag, true)
                .put(pos, new BlockPos(1, 64, -3))
                .put(list, Arrays.asList("a", "b"))
                .put(energy, 123456789L)
                .build();
        ByteBuf buf = Unpooled.buffer();
        NetworkTools.writeString(buf, "cmd");
        TypedMapTools.writeArguments(buf, map);

        assertEquals("cmd", SafeNetworkTools.readString(buf));
        TypedMap read = SafeNetworkTools.readArguments(buf);
        assertEquals("receiver", read.get(name));
        assertEquals(Integer.valueOf(42), read.get(count));
        assertEquals(Boolean.TRUE, read.get(flag));
        assertEquals(new BlockPos(1, 64, -3), read.get(pos));
        assertEquals(Arrays.asList("a", "b"), read.get(list));
        assertEquals(Long.valueOf(123456789L), read.get(energy));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void nullAndEmptyStrings() {
        ByteBuf buf = Unpooled.buffer();
        NetworkTools.writeString(buf, null);
        NetworkTools.writeString(buf, "");
        assertNull(SafeNetworkTools.readString(buf));
        assertEquals("", SafeNetworkTools.readString(buf));
    }

    @Test
    void hugeDeclaredStringIsRejectedBeforeAllocation() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(2_000_000_000);
        buf.writeBytes(new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readString(buf));
    }

    @Test
    void stringLongerThanRemainingBytesIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(100);
        buf.writeBytes(new byte[10]);
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readString(buf));
    }

    @Test
    void negativeStringLengthIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(-7);
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readString(buf));
    }

    @Test
    void hugeTypedMapIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1_000_000);
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readArguments(buf));
    }

    @Test
    void hugeListInsideTypedMapIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        NetworkTools.writeString(buf, "list");
        buf.writeByte(9);               // POS_LIST
        buf.writeInt(500_000_000);      // 声明的项数
        buf.writeLong(0);
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readArguments(buf));
    }

    @Test
    void unknownArgumentTypeIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        NetworkTools.writeString(buf, "x");
        buf.writeByte(42);
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readArguments(buf));
    }

    enum Three { A, B, C }

    @Test
    void enumIndexOutOfRangeIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(2);
        buf.writeByte(3);
        buf.writeByte(-1);
        assertEquals(Three.C, SafeNetworkTools.readEnumByte(buf, Three.values()));
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readEnumByte(buf, Three.values()));
        assertThrows(IllegalArgumentException.class, () -> SafeNetworkTools.readEnumByte(buf, Three.values()));
    }
}
