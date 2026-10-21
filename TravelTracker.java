package vn.astrax.titles.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.AchievementType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nhiệm vụ TRAVEL_BLOCKS: lấy mẫu vị trí mỗi 2 giây (không dùng PlayerMoveEvent nên nhẹ), cộng quãng đường ngang.
 * Bỏ qua dịch chuyển (teleport/respawn/đổi thế giới), người chơi Creative/Spectator và bước nhảy quá lớn (travel.max-sample-blocks).
 */
public final class TravelTracker implements Listener {
    private final AstraxTitlesPlugin plugin;
    private final Map<UUID, Location> last = new HashMap<>();
    private final Map<UUID, Double> remainder = new HashMap<>();
    public TravelTracker(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    public void tick(Iterable<? extends Player> players) {
        double max = plugin.getConfig().getDouble("travel.max-sample-blocks", 150);
        boolean creative = plugin.getConfig().getBoolean("travel.count-creative", false);
        for (Player p : players) {
            UUID id = p.getUniqueId();
            GameMode gm = p.getGameMode();
            if (p.isDead() || gm == GameMode.SPECTATOR || (gm == GameMode.CREATIVE && !creative)) { last.remove(id); continue; }
            Location now = p.getLocation(), prev = last.put(id, now.clone());
            if (prev == null || prev.getWorld() != now.getWorld()) continue;
            double dx = now.getX() - prev.getX(), dz = now.getZ() - prev.getZ(), d = Math.sqrt(dx * dx + dz * dz);
            if (d <= 0.05 || d > max) continue;
            double total = remainder.getOrDefault(id, 0.0) + d; long whole = (long) total;
            remainder.put(id, total - whole);
            if (whole > 0) plugin.progress(p, AchievementType.TRAVEL_BLOCKS, "BLOCK", whole);
        }
    }
    @EventHandler public void onTeleport(PlayerTeleportEvent e) { last.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { last.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { last.remove(e.getPlayer().getUniqueId()); remainder.remove(e.getPlayer().getUniqueId()); }
}
