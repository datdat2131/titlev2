package vn.astrax.titles.model;

import java.util.*;

/** Dữ liệu danh hiệu của 1 người chơi. Thuần Java, dễ test. */
public final class PlayerTitleData {
    private final UUID owner;
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Map<String, Long> progress = new HashMap<>();
    private String selected;
    private volatile boolean dirty;

    public PlayerTitleData(UUID owner) { this.owner = owner; }
    public UUID owner() { return owner; }
    public Set<String> unlocked() { return unlocked; }
    public Map<String, Long> progress() { return progress; }
    public String selected() { return selected; }
    public boolean dirty() { return dirty; }
    public void clearDirty() { dirty = false; }
    public void markDirty() { dirty = true; }

    public boolean has(String id) { return unlocked.contains(id.toLowerCase(Locale.ROOT)); }
    public boolean unlock(String id) { boolean added = unlocked.add(id.toLowerCase(Locale.ROOT)); if (added) dirty = true; return added; }
    public boolean lock(String id) {
        String k = id.toLowerCase(Locale.ROOT); boolean removed = unlocked.remove(k);
        if (k.equals(selected)) selected = null;
        if (removed) dirty = true; return removed;
    }
    /** Chỉ chọn được danh hiệu đã mở; null = bỏ chọn. */
    public boolean select(String id) {
        if (id == null) { boolean had = selected != null; selected = null; dirty = true; return had; }
        String k = id.toLowerCase(Locale.ROOT);
        if (!unlocked.contains(k)) return false;
        selected = k; dirty = true; return true;
    }
    public long progressOf(String id) { return progress.getOrDefault(id.toLowerCase(Locale.ROOT), 0L); }
    public long addProgress(String id, long amount) {
        if (amount <= 0) return progressOf(id);
        String k = id.toLowerCase(Locale.ROOT);
        long v = progress.merge(k, amount, (a, b) -> a + b < 0 ? Long.MAX_VALUE : a + b);
        dirty = true; return v;
    }
    public void setProgress(String id, long v) { progress.put(id.toLowerCase(Locale.ROOT), Math.max(0, v)); dirty = true; }

    public PlayerTitleData copy() {
        PlayerTitleData c = new PlayerTitleData(owner);
        c.unlocked.addAll(unlocked); c.progress.putAll(progress); c.selected = selected; return c;
    }
    /** Phục hồi từ DB/file (không đánh dấu dirty). */
    public void restore(Collection<String> unlockedIds, Map<String, Long> prog, String sel) {
        unlocked.clear(); progress.clear();
        for (String s : unlockedIds) unlocked.add(s.toLowerCase(Locale.ROOT));
        prog.forEach((k, v) -> progress.put(k.toLowerCase(Locale.ROOT), Math.max(0, v)));
        selected = sel == null || sel.isBlank() ? null : sel.toLowerCase(Locale.ROOT);
        if (selected != null && !unlocked.contains(selected)) selected = null;
        dirty = false;
    }
}
