package mcjty.rftools.network;

import mcjty.lib.typed.Type;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * “改手上物品 NBT”一类的包：只允许界面实际会写的键，类型必须对，字符串有长度上限（超过的截断）。
 */
public final class ItemNbtWhitelist {

    private final Map<String, Type<?>> keys;
    private final Map<String, Integer> maxLengths;

    private ItemNbtWhitelist(Map<String, Type<?>> keys, Map<String, Integer> maxLengths) {
        this.keys = Collections.unmodifiableMap(keys);
        this.maxLengths = Collections.unmodifiableMap(maxLengths);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 写入一个值；键不在白名单里或类型不对时不写，返回 false。 */
    public boolean apply(NBTTagCompound tag, String key, Type<?> type, Object value) {
        Type<?> expected = keys.get(key);
        if (expected == null || !expected.equals(type) || value == null) {
            return false;
        }
        if (Type.STRING.equals(type) && value instanceof String) {
            tag.setString(key, PacketLimits.truncate((String) value, maxLengths.getOrDefault(key, PacketLimits.MAX_NBT_STRING_LENGTH)));
        } else if (Type.BOOLEAN.equals(type) && value instanceof Boolean) {
            tag.setBoolean(key, (Boolean) value);
        } else if (Type.INTEGER.equals(type) && value instanceof Integer) {
            tag.setInteger(key, (Integer) value);
        } else {
            return false;
        }
        return true;
    }

    public boolean allows(String key) {
        return keys.containsKey(key);
    }

    public static final class Builder {
        private final Map<String, Type<?>> keys = new HashMap<>();
        private final Map<String, Integer> maxLengths = new HashMap<>();

        public Builder string(String key, int maxLength) {
            keys.put(key, Type.STRING);
            maxLengths.put(key, maxLength);
            return this;
        }

        public Builder bool(String key) {
            keys.put(key, Type.BOOLEAN);
            return this;
        }

        public ItemNbtWhitelist build() {
            return new ItemNbtWhitelist(new HashMap<>(keys), new HashMap<>(maxLengths));
        }
    }
}
