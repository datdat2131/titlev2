package vn.astrax.titles.model;

import java.util.Locale;

/**
 * Loại nhiệm vụ.
 * Đếm tiến độ: KILL_MOB, KILL_PLAYER, BREAK_BLOCK, FISH, PLAYTIME_MINUTES, BOSS_KILL, TRAVEL_BLOCKS (di chuyển), KILL_STREAK (chuỗi hạ gục, reset khi chết).
 * Trạng thái (kiểm tra định kỳ): ELEMENTER_UNLOCK (plugin AstraxElementer), NGUYENTO_UNLOCK (plugin NguyenTo), PERMISSION, PERMISSION_NODE (có quyền/nhóm bất kỳ, vd group.vip3 của LuckPerms).
 * MANUAL: không có nhiệm vụ, admin cấp tay.
 */
public enum AchievementType {
    KILL_MOB, KILL_PLAYER, BREAK_BLOCK, FISH, PLAYTIME_MINUTES, BOSS_KILL, TRAVEL_BLOCKS, KILL_STREAK,
    ELEMENTER_UNLOCK, NGUYENTO_UNLOCK, PERMISSION, PERMISSION_NODE, MANUAL;

    public static AchievementType parse(String s) {
        if (s == null) return null;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }
    public boolean counted() { return this == KILL_MOB || this == KILL_PLAYER || this == BREAK_BLOCK || this == FISH || this == PLAYTIME_MINUTES || this == BOSS_KILL || this == TRAVEL_BLOCKS || this == KILL_STREAK; }
    public boolean state() { return this == ELEMENTER_UNLOCK || this == NGUYENTO_UNLOCK || this == PERMISSION || this == PERMISSION_NODE; }
}
