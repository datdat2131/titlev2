package vn.astrax.titles.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import vn.astrax.titles.model.BuffValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

/** Sửa titles.yml ngay trong game (tên, mô tả, độ hiếm, icon, buff). Luôn sao lưu titles.yml.bak trước khi ghi. */
public final class TitleEditor {
    public static final Set<String> FIELDS = Set.of("display", "description", "rarity", "icon");
    private final JavaPlugin plugin;
    public TitleEditor(JavaPlugin plugin) { this.plugin = plugin; }

    private File file() { return new File(plugin.getDataFolder(), "titles.yml"); }

    private interface Change { void apply(YamlConfiguration y, String base) throws IllegalArgumentException; }

    private String edit(String id, Change c) {
        File f = file();
        YamlConfiguration y = new YamlConfiguration();
        try { y.load(f); } catch (Exception e) { return "Không đọc được titles.yml: " + e.getMessage(); }
        String key = null;
        var root = y.getConfigurationSection("titles");
        if (root != null) for (String k : root.getKeys(false)) if (k.equalsIgnoreCase(id)) key = k;
        if (key == null) return "Không có danh hiệu '" + id + "'.";
        try { c.apply(y, "titles." + key); } catch (IllegalArgumentException e) { return e.getMessage(); }
        try {
            Files.copy(f.toPath(), new File(plugin.getDataFolder(), "titles.yml.bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
            y.save(f);
        } catch (IOException e) { return "Không ghi được titles.yml: " + e.getMessage(); }
        return null; // null = thành công
    }

    /** field: display | description | rarity | icon. Trả về null nếu OK, ngược lại là thông báo lỗi. */
    public String setField(String id, String field, String value) {
        String f = field.toLowerCase(Locale.ROOT);
        if (!FIELDS.contains(f)) return "Trường hợp lệ: " + String.join(", ", FIELDS);
        return edit(id, (y, base) -> y.set(base + "." + f, f.equals("rarity") ? value.toUpperCase(Locale.ROOT) : value));
    }
    /** value: số ("4") hoặc phần trăm ("5%"). */
    public String setBuff(String id, String attr, String value) {
        String a = attr.toLowerCase(Locale.ROOT).replace("generic.", "");
        return edit(id, (y, base) -> {
            try { BuffValue.parse(value); } catch (RuntimeException e) { throw new IllegalArgumentException("Giá trị buff không hợp lệ: " + value); }
            String raw = value.trim(); Object v = raw.endsWith("%") ? raw : (Object) Double.valueOf(raw);
            y.set(base + ".buffs." + a, v);
        });
    }
    public String removeBuff(String id, String attr) {
        String a = attr.toLowerCase(Locale.ROOT).replace("generic.", "");
        return edit(id, (y, base) -> { if (!y.contains(base + ".buffs." + a)) throw new IllegalArgumentException("Danh hiệu không có buff " + a); y.set(base + ".buffs." + a, null); });
    }
}
