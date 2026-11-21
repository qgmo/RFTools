package mcjty.rftools.network;

/**
 * 客户端发来的数据的上限与检查。只做纯计算，方便单元测试。
 * 超过上限时抛 {@link IllegalArgumentException}：解码在网络线程，异常会让这个包作废（发包人被断开），不会进到主线程。
 */
public final class PacketLimits {

    /** 一个字符串最多多少字节（客户端发往服务端的自定义包本身最大 32767 字节）。 */
    public static final int MAX_STRING_BYTES = 32767;
    /** 列表最多多少项。 */
    public static final int MAX_LIST_SIZE = 1024;
    /** TypedMap 最多多少个键。 */
    public static final int MAX_MAP_SIZE = 64;
    /** 名字类字符串（接收器、发射器、频道等）最多多少个字符。 */
    public static final int MAX_NAME_LENGTH = 64;
    /** 物品 NBT 里客户端能写的普通字符串最多多少个字符。 */
    public static final int MAX_NBT_STRING_LENGTH = 256;

    private PacketLimits() {
    }

    /**
     * 检查客户端声明的长度：不能为负，不能超过上限，也不能超过包里实际剩下的字节数（每项至少占 {@code minBytesPerEntry} 字节）。
     * 通过时原样返回。
     */
    public static int checkDeclaredLength(int declared, int readableBytes, int max, int minBytesPerEntry) {
        if (declared < 0) {
            throw new IllegalArgumentException("negative length " + declared);
        }
        if (declared > max) {
            throw new IllegalArgumentException("length " + declared + " exceeds " + max);
        }
        long needed = (long) declared * Math.max(0, minBytesPerEntry);
        if (needed > readableBytes) {
            throw new IllegalArgumentException("length " + declared + " exceeds remaining " + readableBytes + " bytes");
        }
        return declared;
    }

    /** 把字符串截到最多 {@code max} 个字符；null 原样返回。 */
    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    /** 一般命令参数里的字符串最多多少个字符（名字类另有 {@link #MAX_NAME_LENGTH}）。 */
    public static final int MAX_PARAM_STRING_LENGTH = 256;

    /** 参数里所有字符串（含字符串列表）是否都不超过 {@code max} 个字符。 */
    public static boolean stringsWithin(mcjty.lib.typed.TypedMap params, int max) {
        if (params == null) {
            return true;
        }
        for (mcjty.lib.typed.Key<?> key : params.getKeys()) {
            Object v = params.get(key);
            if (v instanceof String && ((String) v).length() > max) {
                return false;
            }
            if (v instanceof java.util.List) {
                for (Object o : (java.util.List<?>) v) {
                    if (o instanceof String && ((String) o).length() > max) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 下标是否在 [0, size) 内。 */
    public static boolean validIndex(int index, int size) {
        return index >= 0 && index < size;
    }

    /** 把整数夹到 [min, max]。 */
    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
