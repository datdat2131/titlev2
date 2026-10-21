package vn.astrax.titles.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.astrax.titles.model.PlayerTitleData;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lưu mỗi người chơi 1 file data/<uuid>.yml. Ghi bất đồng bộ trên 1 thread (tuần tự),
 * ghi ra file tạm rồi move (atomic) nên crash giữa chừng không làm hỏng file.
 */
public final class DataStore implements AutoCloseable {
    private final File dir; private final Logger log;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "AstraxTitles-IO"));

    public DataStore(File dataFolder, Logger log) { this.dir = new File(dataFolder, "data"); this.log = log; dir.mkdirs(); }

    private File file(UUID id) { return new File(dir, id + ".yml"); }

    /** Đọc đồng bộ. Trả về null nếu chưa có file. Ném IOException nếu file hỏng (KHÔNG tạo dữ liệu rỗng đè lên). */
    public PlayerTitleData load(UUID id) throws IOException {
        File f = file(id);
        if (!f.exists()) return null;
        YamlConfiguration y = new YamlConfiguration();
        try { y.loadFromString(Files.readString(f.toPath(), StandardCharsets.UTF_8)); }
        catch (InvalidConfigurationException e) {
            Path bad = Path.of(f.getPath() + ".corrupt-" + System.currentTimeMillis());
            try { Files.copy(f.toPath(), bad); } catch (IOException ignored) { }
            throw new IOException("File dữ liệu hỏng (đã sao lưu " + bad.getFileName() + ")", e);
        }
        PlayerTitleData d = new PlayerTitleData(id);
        Map<String, Long> prog = new HashMap<>();
        ConfigurationSection ps = y.getConfigurationSection("progress");
        if (ps != null) for (String k : ps.getKeys(false)) prog.put(k, ps.getLong(k));
        d.restore(y.getStringList("unlocked"), prog, y.getString("selected"));
        return d;
    }

    private String serialize(PlayerTitleData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("unlocked", new ArrayList<>(d.unlocked()));
        y.set("selected", d.selected());
        d.progress().forEach((k, v) -> y.set("progress." + k, v));
        return y.saveToString();
    }

    private void write(UUID id, String content) throws IOException {
        Path tmp = Path.of(file(id).getPath() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try { Files.move(tmp, file(id).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file(id).toPath(), StandardCopyOption.REPLACE_EXISTING); }
    }

    /** Snapshot chuỗi ngay trên thread gọi (main), ghi file ở thread IO. */
    public CompletableFuture<Void> saveAsync(PlayerTitleData d) {
        final String content = serialize(d); final UUID id = d.owner(); d.clearDirty();
        return CompletableFuture.runAsync(() -> {
            try { write(id, content); } catch (IOException e) { d.markDirty(); log.log(Level.SEVERE, "Lưu thất bại " + id, e); }
        }, io);
    }
    public CompletableFuture<PlayerTitleData> loadAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> { try { return load(id); } catch (IOException e) { throw new CompletionException(e); } }, io);
    }
    /** Sửa dữ liệu người chơi offline (đọc-sửa-ghi tuần tự). */
    public CompletableFuture<Boolean> modifyOffline(UUID id, Consumer<PlayerTitleData> change) {
        return CompletableFuture.supplyAsync(() -> {
            try { PlayerTitleData d = load(id); if (d == null) return false; change.accept(d); write(id, serialize(d)); return true; }
            catch (IOException e) { log.log(Level.SEVERE, "modifyOffline " + id, e); return false; }
        }, io);
    }
    @Override public void close() {
        io.shutdown();
        try { if (!io.awaitTermination(20, TimeUnit.SECONDS)) log.warning("Hết thời gian chờ ghi dữ liệu!"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
