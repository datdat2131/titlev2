package vn.astrax.titles.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.*;

import java.util.concurrent.TimeUnit;

public final class ProgressListener implements Listener {
    private final AstraxTitlesPlugin plugin;
    private final PairCooldown pairs = new PairCooldown();
    public ProgressListener(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        try {
            PlayerTitleData d = plugin.store().loadAsync(e.getUniqueId()).get(10, TimeUnit.SECONDS); // xếp sau các lần ghi đang chờ
            plugin.putData(d == null ? new PlayerTitleData(e.getUniqueId()) : d);
        } catch (Exception ex) {
            plugin.getLogger().severe("Tải dữ liệu thất bại cho " + e.getName() + ": " + ex);
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, net.kyori.adventure.text.Component.text("Dữ liệu danh hiệu đang lỗi, thử lại sau ít phút."));
        }
    }
    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerTitleData d = plugin.data(p.getUniqueId()); if (d == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) plugin.checkStates(p); }, 60L); // Elementer nạp dữ liệu bất đồng bộ nên chờ ~3s
        plugin.buffs().apply(p); plugin.refreshTab(p);
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        plugin.buffs().remove(e.getPlayer());
        PlayerTitleData d = plugin.removeData(e.getPlayer().getUniqueId());
        if (d != null) plugin.store().saveAsync(d);
        plugin.gui().forget(e.getPlayer().getUniqueId()); pairs.forget(e.getPlayer().getUniqueId()); plugin.forgetTab(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { Player p = e.getPlayer(); Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) plugin.buffs().apply(p); }); }

    @EventHandler(ignoreCancelled = true) public void onBreak(BlockBreakEvent e) { plugin.progress(e.getPlayer(), AchievementType.BREAK_BLOCK, e.getBlock().getType().name(), 1); }
    @EventHandler public void onFish(PlayerFishEvent e) { if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) plugin.progress(e.getPlayer(), AchievementType.FISH, "FISH", 1); }
    @EventHandler public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity(); Player killer = dead.getKiller();
        if (killer == null || killer.equals(dead)) return;
        if (dead instanceof Player victim) { // chống farm: cùng một nạn nhân chỉ tính lại sau anti-farm.same-victim-seconds
            long cd = plugin.getConfig().getLong("anti-farm.same-victim-seconds", 120) * 1000L;
            if (!pairs.tryAcquire(killer.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis(), cd)) return;
            plugin.progress(killer, AchievementType.KILL_PLAYER, "PLAYER", 1);
            plugin.streakKill(killer, true);
        } else { plugin.progress(killer, AchievementType.KILL_MOB, dead.getType().name(), 1); plugin.streakKill(killer, false); }
    }
    @EventHandler public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent e) { plugin.resetStreak(e.getEntity()); }
}
