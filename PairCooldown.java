package vn.astrax.titles.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Chống farm kill: cùng một nạn nhân chỉ tính lại sau cooldown. */
public final class PairCooldown {
    private final Map<UUID, Map<UUID, Long>> map = new HashMap<>();
    public boolean tryAcquire(UUID killer, UUID victim, long nowMs, long cooldownMs) {
        if (killer.equals(victim)) return false;
        Map<UUID, Long> m = map.computeIfAbsent(killer, k -> new HashMap<>());
        Long last = m.get(victim);
        if (last != null && nowMs - last < cooldownMs) return false;
        m.put(victim, nowMs); return true;
    }
    public void forget(UUID killer) { map.remove(killer); }
    public int size() { return map.size(); }
}
