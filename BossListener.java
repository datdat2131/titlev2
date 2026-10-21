package vn.astrax.titles.listener;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.AchievementType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nhiệm vụ BOSS_KILL: ghi sát thương mỗi người chơi lên boss (AstraxElementer qua khóa PDC boss_id, hoặc boss vanilla Ender Dragon/Wither),
 * khi boss chết thì mọi người gây >= boss-credit-percent % máu tối đa của boss đều được tính 1 lần hạ boss
 * (giống ngưỡng nhận thưởng 5% của Elementer, nên không chỉ người đánh đòn cuối).
 */
public final class BossListener implements Listener {
    private final AstraxTitlesPlugin plugin;
    private final Map<UUID, Map<UUID, Double>> damage = new HashMap<>();
    public BossListener(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity boss) || bossId(boss) == null) return;
        Player p = e.getDamager() instanceof Player pl ? pl : (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player ps ? ps : null);
        if (p == null) return;
        damage.computeIfAbsent(boss.getUniqueId(), k -> new HashMap<>()).merge(p.getUniqueId(), e.getFinalDamage(), Double::sum);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        String id = bossId(e.getEntity()); if (id == null) return;
        Map<UUID, Double> map = damage.remove(e.getEntity().getUniqueId());
        AttributeInstance mh = e.getEntity().getAttribute(Attribute.MAX_HEALTH);
        double need = (mh == null ? 100 : mh.getValue()) * plugin.getConfig().getDouble("boss-credit-percent", 5) / 100.0;
        Player killer = e.getEntity().getKiller();
        Map<UUID, Boolean> credited = new HashMap<>();
        if (killer != null) credited.put(killer.getUniqueId(), true); // người đánh đòn cuối luôn được tính
        if (map != null) map.forEach((u, d) -> { if (d >= need) credited.put(u, true); });
        for (UUID u : credited.keySet()) { Player p = Bukkit.getPlayer(u); if (p != null && p.isOnline()) plugin.progress(p, AchievementType.BOSS_KILL, id, 1); }
    }

    /** Id boss: boss AstraxElementer (dragon_lord...) hoặc boss vanilla (ender_dragon, wither) -> tên loại entity thường. */
    private String bossId(Entity e) {
        String id = plugin.elementer().bossId(e);
        if (id != null) return id;
        return e instanceof Boss ? e.getType().name().toLowerCase(java.util.Locale.ROOT) : null;
    }

    /** Dọn dữ liệu boss đã biến mất mà không chết (despawn). */
    public void purge() { damage.keySet().removeIf(id -> Bukkit.getEntity(id) == null); }
}
