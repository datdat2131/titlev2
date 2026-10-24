package vn.astrax.titles.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * /titleadmin updateconfig: thêm danh hiệu mới và mục config mới của bản plugin mới vào file đang dùng.
 * KHÔNG ghi đè giá trị cũ, luôn sao lưu vào plugins/AstraxTitles/backups/ trước khi ghi.
 */
public final class ConfigUpdater {
    public record Report(List<String> newTitles, List<String> newKeys) { public boolean empty() { return newTitles.isEmpty() && newKeys.isEmpty(); } }
    /** Các mục dạng danh sách tự do: người dùng có thể xóa bớt dòng nên chỉ thêm khi cả mục chưa tồn tại. */
    private static final Set<String> USER_MAPS = Set.of("boss-elements");
    private final JavaPlugin plugin;
    public ConfigUpdater(JavaPlugin plugin) { this.plugin = plugin; }

    private String bundled(String name) throws IOException {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) throw new IOException("Không thấy " + name + " trong plugin");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private Set<String> ignored() { Set<String> s = new HashSet<>(); for (String x : plugin.getConfig().getStringList("update.ignore-titles")) s.add(x.toLowerCase(Locale.ROOT)); return s; }
    private File file(String n) { return new File(plugin.getDataFolder(), n); }
    private String read(File f) throws IOException { return Files.readString(f.toPath(), StandardCharsets.UTF_8); }

    private YamlConfiguration bundledConfig() throws IOException {
        YamlConfiguration def = new YamlConfiguration();
        try { def.loadFromString(bundled("config.yml")); } catch (InvalidConfigurationException e) { throw new IOException(e.getMessage(), e); }
        return def;
    }
    private List<String> missingKeys() throws IOException {
        YamlConfiguration def = bundledConfig(), user = YamlConfiguration.loadConfiguration(file("config.yml"));
        List<String> out = new ArrayList<>();
        for (String k : def.getKeys(true)) {
            if (def.isConfigurationSection(k) || user.contains(k, true)) continue;
            boolean skip = false; for (String m : USER_MAPS) if (k.startsWith(m + ".") && user.isConfigurationSection(m)) skip = true;
            if (!skip) out.add(k);
        }
        return out;
    }

    /** Xem trước, không ghi gì. */
    public Report check() throws IOException {
        File tf = file("titles.yml");
        List<String> nt = tf.exists() ? TitleMerger.merge(read(tf), bundled("titles.yml"), ignored()).added() : List.of();
        return new Report(nt, missingKeys());
    }

    public Report apply() throws IOException {
        Report r = check();
        File dir = plugin.getDataFolder();
        if (!r.empty()) {
            File bak = new File(dir, "backups/" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())); bak.mkdirs();
            for (String n : List.of("titles.yml", "config.yml")) { File f = file(n); if (f.exists()) Files.copy(f.toPath(), new File(bak, n).toPath(), StandardCopyOption.REPLACE_EXISTING); }
        }
        if (!r.newTitles().isEmpty()) writeAtomic(file("titles.yml"), TitleMerger.merge(read(file("titles.yml")), bundled("titles.yml"), ignored()).merged());
        if (!r.newKeys().isEmpty()) {
            YamlConfiguration def = bundledConfig(), user = YamlConfiguration.loadConfiguration(file("config.yml"));
            for (String k : r.newKeys()) { user.set(k, def.get(k)); try { user.setComments(k, def.getComments(k)); } catch (Throwable ignore) { } }
            writeAtomic(file("config.yml"), user.saveToString());
        }
        // bản mặc định mới nhất để bạn đối chiếu/sao chép tay nếu muốn dùng giá trị mới cho danh hiệu cũ
        writeAtomic(file("titles.default.yml"), bundled("titles.yml")); writeAtomic(file("config.default.yml"), bundled("config.yml"));
        return r;
    }

    private void writeAtomic(File f, String content) throws IOException {
        Path tmp = Path.of(f.getPath() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try { Files.move(tmp, f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, f.toPath(), StandardCopyOption.REPLACE_EXISTING); }
    }
}
