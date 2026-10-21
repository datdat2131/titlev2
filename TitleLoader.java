package vn.astrax.titles.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import vn.astrax.titles.model.*;

import java.io.File;
import java.util.*;

/** Đọc titles.yml. Mục lỗi bị bỏ qua kèm cảnh báo, không làm sập plugin. */
public final class TitleLoader {
    private final JavaPlugin plugin;
    public TitleLoader(JavaPlugin plugin) { this.plugin = plugin; }

    public void load(TitleRegistry reg) {
        reg.clear();
        File f = new File(plugin.getDataFolder(), "titles.yml");
        if (!f.exists()) plugin.saveResource("titles.yml", false);
        ConfigurationSection root = YamlConfiguration.loadConfiguration(f).getConfigurationSection("titles");
        if (root == null) { plugin.getLogger().warning("titles.yml không có mục 'titles'."); return; }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                List<Requirement> reqs = new ArrayList<>();
                for (Map<?, ?> m : s.getMapList("requirements")) reqs.add(requirement(m));
                // cú pháp rút gọn: type/target/filter ngay trong title (tương thích bản cũ)
                if (reqs.isEmpty() && s.contains("type") && AchievementType.parse(s.getString("type")) != AchievementType.MANUAL) {
                    AchievementType t = AchievementType.parse(s.getString("type"));
                    if (t == null) throw new IllegalArgumentException("type không hợp lệ: " + s.getString("type"));
                    reqs.add(new Requirement(t, s.getString("filter"), s.getLong("target", 1), s.getString("label")));
                }
                Map<String, BuffValue> buffs = new LinkedHashMap<>();
                ConfigurationSection bs = s.getConfigurationSection("buffs");
                if (bs != null) for (String k : bs.getKeys(false)) {
                    try { buffs.put(normalize(k), BuffValue.parse(bs.get(k))); }
                    catch (RuntimeException e) { plugin.getLogger().warning("titles.yml: " + id + " buff " + k + " lỗi: " + e.getMessage()); }
                }
                List<PassiveDef> passives = new ArrayList<>();
                for (Map<?, ?> m : s.getMapList("passives")) {
                    try { passives.add(passive(m)); } catch (RuntimeException e) { plugin.getLogger().warning("titles.yml: " + id + " nội tại lỗi: " + e.getMessage()); }
                }
                reg.register(new TitleDef(id, s.getString("display", id), s.getString("description", ""), Rarity.parse(s.getString("rarity")),
                        s.getString("icon", "NAME_TAG"), reqs, buffs, passives, s.getBoolean("announce", false), s.getBoolean("hidden", false)));
            } catch (RuntimeException e) { plugin.getLogger().warning("titles.yml: bỏ qua '" + id + "': " + e.getMessage()); }
        }
    }

    private static Requirement requirement(Map<?, ?> m) {
        AchievementType t = AchievementType.parse(str(m.get("type")));
        if (t == null || t == AchievementType.MANUAL) throw new IllegalArgumentException("requirement type không hợp lệ: " + m.get("type"));
        return new Requirement(t, str(m.get("filter")), (long) num(m.get("target"), 1), str(m.get("label")));
    }
    private static PassiveDef passive(Map<?, ?> m) {
        PassiveType t = PassiveType.parse(str(m.get("type")));
        if (t == null) throw new IllegalArgumentException("passive type không hợp lệ: " + m.get("type"));
        return new PassiveDef(t, str(m.get("name")), num(m.get("value"), 0), num(m.get("value2"), 0), Math.max(0, Math.min(100, num(m.get("chance"), 100))),
                num(m.get("threshold"), 30), (int) num(m.get("duration"), 0), (int) num(m.get("cooldown"), 0), str(m.get("filter")));
    }
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static double num(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        if (o == null) return def;
        try { return Double.parseDouble(String.valueOf(o).trim()); } catch (NumberFormatException e) { return def; }
    }
    /** "generic.max_health" / "MAX_HEALTH" / "max_health" -> "max_health". */
    static String normalize(String k) {
        String s = k.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("generic.")) s = s.substring(8);
        if (s.startsWith("minecraft:")) s = s.substring(10);
        return s;
    }
}
