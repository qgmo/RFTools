package mcjty.rftools.shapes;

import java.util.HashMap;
import java.util.Map;

/**
 * 每刻按玩家计的工作量预算：每刻开始时 {@link #reset}，每做一份工作 {@link #charge} 一次，用完的玩家这一刻不再做。
 * 一份工作的花费在开始前不知道是否“空”，所以不论结果如何都要扣（空形状也扣）。纯计算，便于单元测试。
 */
public final class PlayerWorkBudget<K> {

    private final Map<K, Long> spent = new HashMap<>();
    private long perTick;

    public PlayerWorkBudget(long perTick) {
        this.perTick = perTick;
    }

    public void reset(long perTick) {
        this.perTick = perTick;
        spent.clear();
    }

    public boolean hasBudget(K key) {
        Long s = spent.get(key);
        return s == null || s < perTick;
    }

    public void charge(K key, long cost) {
        spent.merge(key, Math.max(1, cost), Long::sum);
    }

    public long spent(K key) {
        Long s = spent.get(key);
        return s == null ? 0 : s;
    }
}
