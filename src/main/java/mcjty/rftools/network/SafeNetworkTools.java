package mcjty.rftools.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端解码客户端数据用的读取工具：格式与 McJtyLib 的 {@link NetworkTools}、{@code TypedMapTools} 完全相同（两边的客户端照旧发包），
 * 但客户端声明的长度先按 {@link PacketLimits} 封顶、并且不能超过包里剩下的字节数，再分配内存。
 * 只在“服务端收包”的 {@code fromBytes} 里用；写包仍用 McJtyLib 的工具。
 */
public final class SafeNetworkTools {

    private SafeNetworkTools() {
    }

    @Nullable
    public static String readString(ByteBuf buf) {
        int s = buf.readInt();
        if (s == -1) {
            return null;
        }
        if (s == 0) {
            return "";
        }
        PacketLimits.checkDeclaredLength(s, buf.readableBytes(), PacketLimits.MAX_STRING_BYTES, 1);
        byte[] dst = new byte[s];
        buf.readBytes(dst);
        // 与 NetworkTools.readString 相同，用平台默认编码
        return new String(dst);
    }

    @Nullable
    public static String readStringUTF8(ByteBuf buf) {
        int s = buf.readInt();
        if (s == -1) {
            return null;
        }
        if (s == 0) {
            return "";
        }
        PacketLimits.checkDeclaredLength(s, buf.readableBytes(), PacketLimits.MAX_STRING_BYTES, 1);
        byte[] dst = new byte[s];
        buf.readBytes(dst);
        return new String(dst, StandardCharsets.UTF_8);
    }

    @Nonnull
    public static List<String> readStringList(ByteBuf buf) {
        int size = PacketLimits.checkDeclaredLength(buf.readInt(), buf.readableBytes(), PacketLimits.MAX_LIST_SIZE, 4);
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readStringUTF8(buf));
        }
        return list;
    }

    public static BlockPos readPos(ByteBuf buf) {
        return NetworkTools.readPos(buf);
    }

    @Nullable
    public static List<BlockPos> readPosList(ByteBuf buf) {
        int size = buf.readInt();
        if (size == -1) {
            return null;
        }
        PacketLimits.checkDeclaredLength(size, buf.readableBytes(), PacketLimits.MAX_LIST_SIZE, 8);
        List<BlockPos> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readPos(buf));
        }
        return list;
    }

    /** 物品的 NBT 由原版的 PacketBuffer 读（有 2 MB 的 NBT 上限），数量照 NetworkTools。 */
    public static ItemStack readItemStack(ByteBuf buf) {
        return NetworkTools.readItemStack(buf);
    }

    @Nonnull
    public static List<ItemStack> readItemStackList(ByteBuf buf) {
        int size = PacketLimits.checkDeclaredLength(buf.readInt(), buf.readableBytes(), PacketLimits.MAX_LIST_SIZE, 5);
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readItemStack(buf));
        }
        return list;
    }

    /** 枚举：下标越界时抛异常（原来的 values()[i] 也会抛，这里只是给出明确的信息）。 */
    public static <T extends Enum<T>> T readEnumByte(ByteBuf buf, T[] values) {
        int i = buf.readByte();
        if (!PacketLimits.validIndex(i, values.length)) {
            throw new IllegalArgumentException("bad enum index " + i);
        }
        return values[i];
    }

    /** 同 McJtyLib 的 TypedMapTools.readArguments（类型编号 0–9 照它的 ArgumentType），加上键数、长度上限。 */
    public static TypedMap readArguments(ByteBuf buf) {
        TypedMap.Builder args = TypedMap.builder();
        int size = PacketLimits.checkDeclaredLength(buf.readInt(), buf.readableBytes(), PacketLimits.MAX_MAP_SIZE, 5);
        for (int i = 0; i < size; i++) {
            String key = readString(buf);
            int type = buf.readByte();
            switch (type) {
                case 0:     // TYPE_STRING
                    args.put(new Key<>(key, Type.STRING), readString(buf));
                    break;
                case 1:     // TYPE_INTEGER
                    args.put(new Key<>(key, Type.INTEGER), buf.readInt());
                    break;
                case 2:     // TYPE_BLOCKPOS
                    args.put(new Key<>(key, Type.BLOCKPOS), buf.readBoolean() ? readPos(buf) : null);
                    break;
                case 3:     // TYPE_BOOLEAN
                    args.put(new Key<>(key, Type.BOOLEAN), buf.readBoolean());
                    break;
                case 4:     // TYPE_DOUBLE
                    args.put(new Key<>(key, Type.DOUBLE), buf.readDouble());
                    break;
                case 5:     // TYPE_STACK
                    args.put(new Key<>(key, Type.ITEMSTACK), buf.readBoolean() ? readItemStack(buf) : null);
                    break;
                case 6:     // TYPE_LONG
                    args.put(new Key<>(key, Type.LONG), buf.readLong());
                    break;
                case 7: {   // TYPE_STRING_LIST
                    int s = buf.readInt();
                    if (s == -1) {
                        args.put(new Key<>(key, Type.STRING_LIST), null);
                    } else {
                        PacketLimits.checkDeclaredLength(s, buf.readableBytes(), PacketLimits.MAX_LIST_SIZE, 4);
                        List<String> list = new ArrayList<>(s);
                        for (int j = 0; j < s; j++) {
                            list.add(readStringUTF8(buf));
                        }
                        args.put(new Key<>(key, Type.STRING_LIST), list);
                    }
                    break;
                }
                case 8: {   // TYPE_ITEMSTACK_LIST
                    int s = buf.readInt();
                    if (s == -1) {
                        args.put(new Key<>(key, Type.ITEMSTACK_LIST), null);
                    } else {
                        PacketLimits.checkDeclaredLength(s, buf.readableBytes(), PacketLimits.MAX_LIST_SIZE, 5);
                        List<ItemStack> list = new ArrayList<>(s);
                        for (int j = 0; j < s; j++) {
                            list.add(readItemStack(buf));
                        }
                        args.put(new Key<>(key, Type.ITEMSTACK_LIST), list);
                    }
                    break;
                }
                case 9: {   // TYPE_POS_LIST
                    int s = buf.readInt();
                    if (s == -1) {
                        args.put(new Key<>(key, Type.POS_LIST), null);
                    } else {
                        PacketLimits.checkDeclaredLength(s, buf.readableBytes(), PacketLimits.MAX_LIST_SIZE, 8);
                        List<BlockPos> list = new ArrayList<>(s);
                        for (int j = 0; j < s; j++) {
                            list.add(readPos(buf));
                        }
                        args.put(new Key<>(key, Type.POS_LIST), list);
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported type " + type + " for key '" + key + "'");
            }
        }
        return args.build();
    }
}
