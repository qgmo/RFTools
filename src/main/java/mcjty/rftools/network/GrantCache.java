package mcjty.rftools.network;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 记住最近“允许”过的检查，在 {@code ttl} 个刻之内不必再问一次（领地插件拒绝时会给玩家发提示，拖动滑条之类的连续操作
 * 不必每个包都问）。只记允许，不记拒绝。时间由调用方传入（服务端的刻计数），不读真实时间。
 * 超过容量时丢掉最久没用的。只在主线程用。
 */
public final class GrantCache<K> {

    private final long ttl;
    private final LinkedHashMap<K, Long> expiry;

    public GrantCache(final int capacity, long ttl) {
        this.ttl = ttl;
        this.expiry = new LinkedHashMap<K, Long>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Long> eldest) {
                return size() > capacity;
            }
        };
    }

    public boolean isGranted(K key, long now) {
        Long until = expiry.get(key);
        if (until == null) {
            return false;
        }
        if (now >= until || now < until - ttl) {
            // 过期（或时钟倒退，例如换了一个服务端）
            expiry.remove(key);
            return false;
        }
        return true;
    }

    public void grant(K key, long now) {
        expiry.put(key, now + ttl);
    }

    public void clear() {
        expiry.clear();
    }

    public int size() {
        return expiry.size();
    }
}
