package vn.astrax.titles.model;

import java.util.Locale;

/**
 * Một nội tại. value/value2 tính theo %; chance 0-100 (mặc định 100); threshold = % máu kích hoạt;
 * duration/cooldown tính bằng giây; filter tùy loại (nguyên tố, loại sát thương, hiệu ứng hạt).
 */
public record PassiveDef(PassiveType type, String name, double value, double value2, double chance, double threshold,
                         int duration, int cooldown, String filter) {
    static String n(double d) { return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.1f", d); }

    private String elementName(String f) {
        if (f == null) return "?";
        String u = f.toUpperCase(Locale.ROOT);
        return switch (u) { case "DARK" -> "Bóng Tối"; case "LIGHT" -> "Ánh Sáng"; case "FIRE" -> "Lửa"; case "ICE" -> "Băng"; case "LIGHTNING" -> "Sét";
            case "EARTH" -> "Đất"; case "WIND" -> "Gió"; case "DRAGON" -> "Rồng"; default -> f; };
    }

    /** Mô tả tiếng Việt hiển thị trong GUI. */
    public String describe() {
        String c = chance >= 100 ? "" : n(chance) + "% cơ hội ";
        String body = switch (type) {
            case OUTGOING_DAMAGE_PERCENT -> "+" + n(value) + "% sát thương gây ra";
            case DAMAGE_VS_BURNING_PERCENT -> "+" + n(value) + "% sát thương lên mục tiêu đang cháy";
            case BOSS_DAMAGE_PERCENT -> "+" + n(value) + "% sát thương khi đánh Boss";
            case IGNITE_ON_HIT -> c + "đòn đánh thiêu đốt mục tiêu " + duration + "s";
            case SLOW_ON_HIT -> c + "đòn đánh gây lạnh (làm chậm) " + duration + "s";
            case WEAKEN_ON_HIT -> c + "đòn đánh gây Suy Yếu " + duration + "s";
            case LIFESTEAL_PERCENT -> "Hút máu " + n(value) + "% sát thương gây ra";
            case LIFESTEAL_ON_HIT -> c + "hút " + n(value) + "% sát thương thành máu";
            case LIGHTNING_ON_HIT -> c + "đòn đánh giáng sét (+" + n(value) + "% sát thương)";
            case BONUS_DAMAGE_ON_HIT -> c + "gây thêm " + n(value) + "% sát thương" + (duration > 0 ? " và thiêu đốt " + duration + "s" : "");
            case DODGE -> n(chance) + "% né tránh đòn đánh";
            case DAMAGE_REDUCTION_PERCENT -> filter != null && filter.equalsIgnoreCase("FIRE") ? "Kháng lửa: giảm " + n(value) + "% sát thương từ lửa"
                    : "Giảm " + n(value) + "% sát thương nhận" + (filter == null ? "" : " (" + filter + ")");
            case DAMAGE_REDUCTION_FROM_ELEMENT -> "Giảm " + n(value) + "% sát thương nhận từ " + elementName(filter);
            case HEAL_RECEIVED_PERCENT -> "+" + n(value) + "% lượng máu hồi nhận được";
            case LOW_HP_BOOST -> "Khi HP dưới " + n(threshold) + "%: +" + n(value) + "% sát thương" + (value2 > 0 ? ", +" + n(value2) + "% phòng thủ" : "")
                    + " trong " + duration + "s (hồi " + cooldown + "s)";
            case LOW_HP_HEAL -> "Khi HP dưới " + n(threshold) + "%: hồi " + n(value) + "% máu tối đa" + (value2 > 0 ? ", +" + n(value2) + "% tốc độ " + duration + "s" : "")
                    + " (hồi " + cooldown + "s)";
        };
        return (name == null || name.isBlank()) ? body : name + ": " + body;
    }
}
