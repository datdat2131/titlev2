package vn.astrax.titles.model;

import java.util.Map;

/** Một nhiệm vụ của danh hiệu. filter: mục tiêu (ZOMBIE, DIAMOND_ORE, FIRE, lua, dragon_lord...). */
public record Requirement(AchievementType type, String filter, long target, String label) {
    private static final Map<String, String> ELEMENTER = Map.of("FIRE", "Hỏa", "ICE", "Băng", "LIGHTNING", "Lôi", "EARTH", "Thổ",
            "WIND", "Phong", "LIGHT", "Quang", "DARK", "Bóng Tối", "DRAGON", "Rồng");
    private static final Map<String, String> NGUYENTO = Map.of("lua", "Lửa", "nuoc", "Nước", "dat", "Đất", "gio", "Gió",
            "set", "Sét", "bang", "Băng", "bong_toi", "Bóng Tối", "anh_sang", "Ánh Sáng");

    public Requirement { target = type != null && type.counted() ? Math.max(1, target) : 1; }

    /** Danh sách quyền cho PERMISSION_NODE: "group.vip3,group.vip4" hoặc "a|b" = chỉ cần MỘT trong số đó. */
    public static java.util.List<String> nodes(String filter) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (filter == null) return out;
        for (String s : filter.split("[,|]")) if (!s.isBlank()) out.add(s.trim());
        return out;
    }
    public boolean matches(String key) { return filter == null || filter.isBlank() || filter.equalsIgnoreCase(key); }
    public static String reqKey(String titleId, int index) { return titleId.toLowerCase() + "~" + index; }

    public static String elementerName(String f) { return f == null ? "?" : ELEMENTER.getOrDefault(f.toUpperCase(java.util.Locale.ROOT), f); }
    public static String nguyentoName(String f) { return f == null ? "?" : NGUYENTO.getOrDefault(f.toLowerCase(java.util.Locale.ROOT), f); }

    /** Mô tả hiển thị trong GUI; có thể ghi đè bằng "label" trong titles.yml. */
    public String describe() {
        if (label != null && !label.isBlank()) return label;
        return switch (type) {
            case PLAYTIME_MINUTES -> "Online đủ " + target + " phút";
            case KILL_PLAYER -> "Hạ gục " + target + " người chơi";
            case KILL_MOB -> "Tiêu diệt " + target + " " + (filter == null ? "quái vật" : filter);
            case BREAK_BLOCK -> "Đào " + target + " " + (filter == null ? "block" : filter);
            case FISH -> "Câu " + target + " con cá";
            case BOSS_KILL -> "Tiêu diệt boss " + (filter == null ? "bất kỳ" : filter) + (target > 1 ? " x" + target : "");
            case ELEMENTER_UNLOCK -> "Mở khóa nguyên tố " + elementerName(filter) + " (plugin Elementer)";
            case NGUYENTO_UNLOCK -> "Mở khóa nguyên tố " + nguyentoName(filter) + " (plugin NguyenTo)";
            case TRAVEL_BLOCKS -> "Di chuyển " + target + " block";
            case KILL_STREAK -> "Đạt chuỗi hạ gục " + target + ("ANY".equalsIgnoreCase(filter) ? " (tính cả quái)" : " người chơi liên tiếp") + ", không chết";
            case PERMISSION_NODE -> "Đạt rank/quyền " + (filter == null ? "?" : String.join(" hoặc ", nodes(filter)));
            case PERMISSION -> "Nhận từ quyền đặc biệt";
            case MANUAL -> "Do admin cấp";
        };
    }
}
