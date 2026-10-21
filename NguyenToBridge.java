package vn.astrax.titles.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/**
 * Đọc nguyên tố đã sở hữu của plugin NguyenTo (plugin riêng, khác AstraxElementer).
 * NguyenTo lưu trong PersistentDataContainer của người chơi: khóa "nguyento:owned" = "lua,nuoc,bong_toi"...
 * Không cần reflection, không phụ thuộc lớp của NguyenTo.
 */
public final class NguyenToBridge {
    private static final NamespacedKey OWNED = NamespacedKey.fromString("nguyento:owned");
    public boolean available() { var p = Bukkit.getPluginManager().getPlugin("NguyenTo"); return p != null && p.isEnabled(); }

    public boolean owns(Player p, String id) {
        if (id == null || OWNED == null) return false;
        String raw = p.getPersistentDataContainer().get(OWNED, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return false;
        String want = norm(id);
        for (String part : raw.split(",")) if (norm(part).equals(want)) return true;
        return false;
    }
    private static String norm(String s) { return s.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", ""); }
}
