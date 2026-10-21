package vn.astrax.titles.model;

/** Kiểm tra trạng thái bên ngoài (đã mở khóa nguyên tố ở plugin khác, có permission...). */
@FunctionalInterface
public interface StateProbe {
    boolean has(AchievementType type, String filter, TitleDef def);
}
