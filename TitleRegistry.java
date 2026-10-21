package vn.astrax.titles.model;

import java.util.*;

/** Danh sách danh hiệu + chỉ mục theo loại nhiệm vụ. Thuần logic. */
public final class TitleRegistry {
    private final Map<String, TitleDef> byId = new LinkedHashMap<>();
    private final Map<AchievementType, List<TitleDef>> byType = new EnumMap<>(AchievementType.class);
    private final List<TitleDef> withState = new ArrayList<>();

    public void clear() { byId.clear(); byType.clear(); withState.clear(); }
    public void register(TitleDef d) {
        byId.put(d.id().toLowerCase(Locale.ROOT), d);
        Set<AchievementType> seen = EnumSet.noneOf(AchievementType.class);
        boolean state = false;
        for (Requirement r : d.requirements()) { if (seen.add(r.type())) byType.computeIfAbsent(r.type(), k -> new ArrayList<>()).add(d); state |= r.type().state(); }
        if (state) withState.add(d);
    }
    public TitleDef get(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<TitleDef> all() { return Collections.unmodifiableCollection(byId.values()); }
    public List<TitleDef> ofType(AchievementType t) { return byType.getOrDefault(t, List.of()); }
    public List<TitleDef> withState() { return withState; }
    public int size() { return byId.size(); }

    /** Tất cả nhiệm vụ đã đạt chưa. */
    public boolean complete(PlayerTitleData d, TitleDef def) {
        if (def.requirements().isEmpty()) return false;
        for (int i = 0; i < def.requirements().size(); i++) {
            Requirement r = def.requirements().get(i);
            if (d.progressOf(Requirement.reqKey(def.id(), i)) < r.target()) return false;
        }
        return true;
    }

    /** Cộng tiến độ cho sự kiện đếm (type,key). Trả về danh hiệu vừa mở khóa. */
    public List<TitleDef> progress(PlayerTitleData d, AchievementType type, String key, long amount) {
        if (!type.counted()) return List.of();
        List<TitleDef> out = null;
        for (TitleDef def : ofType(type)) {
            if (d.has(def.id())) continue;
            boolean touched = false;
            for (int i = 0; i < def.requirements().size(); i++) {
                Requirement r = def.requirements().get(i);
                if (r.type() == type && r.matches(key)) { d.addProgress(Requirement.reqKey(def.id(), i), amount); touched = true; }
            }
            if (touched && complete(d, def) && d.unlock(def.id())) { if (out == null) out = new ArrayList<>(); out.add(def); }
        }
        return out == null ? List.of() : out;
    }

    /** Kiểm tra các nhiệm vụ dạng trạng thái (mở khóa nguyên tố ở plugin khác, permission). Ghi nhớ lại khi đã đạt. */
    public List<TitleDef> checkStates(PlayerTitleData d, StateProbe probe) {
        List<TitleDef> out = null;
        for (TitleDef def : withState) {
            if (d.has(def.id())) continue;
            for (int i = 0; i < def.requirements().size(); i++) {
                Requirement r = def.requirements().get(i);
                String k = Requirement.reqKey(def.id(), i);
                if (r.type().state() && d.progressOf(k) < 1 && probe.has(r.type(), r.filter(), def)) d.setProgress(k, 1);
            }
            if (complete(d, def) && d.unlock(def.id())) { if (out == null) out = new ArrayList<>(); out.add(def); }
        }
        return out == null ? List.of() : out;
    }

    /** Đặt tiến độ = max(hiện tại, value). Dùng cho chuỗi hạ gục (giữ kỷ lục cao nhất). */
    public List<TitleDef> progressMax(PlayerTitleData d, AchievementType type, String key, long value) {
        List<TitleDef> out = null;
        for (TitleDef def : ofType(type)) {
            if (d.has(def.id())) continue;
            boolean touched = false;
            for (int i = 0; i < def.requirements().size(); i++) {
                Requirement r = def.requirements().get(i);
                if (r.type() != type || !r.matches(key)) continue;
                String k = Requirement.reqKey(def.id(), i);
                if (d.progressOf(k) < value) d.setProgress(k, value);
                touched = true;
            }
            if (touched && complete(d, def) && d.unlock(def.id())) { if (out == null) out = new ArrayList<>(); out.add(def); }
        }
        return out == null ? List.of() : out;
    }
}
