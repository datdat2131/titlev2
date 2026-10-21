package vn.astrax.titles.listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.PassiveDef;
import vn.astrax.titles.model.PassiveType;

import java.util.*;

/**
 * Xử lý nội tại của danh hiệu đang đeo. Một handler duy nhất cho EntityDamageEvent (cả người đánh lẫn người bị đánh)
 * nên thứ tự xử lý luôn đúng; extra damage chạy ở tick sau và có cờ chống đệ quy.
 */
public final class PassiveListener implements Listener {
    private record Boost(long until, double dmgPct, double defPct) {}

    private final AstraxTitlesPlugin plugin;
    private final Random rnd = new Random();
    private final Map<UUID, Boost> boosts = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> speedTasks = new HashMap<>();
    private final NamespacedKey speedKey;
    private boolean inBonus;

    public PassiveListener(AstraxTitlesPlugin plugin) { this.plugin = plugin; this.speedKey = new NamespacedKey(plugin, "title_lowhp_speed"); }

    private boolean roll(double chance) { return chance >= 100 || rnd.nextDouble() * 100 < chance; }
    private static double maxHp(LivingEntity e) { AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH); return a == null ? 20 : a.getValue(); }
    private void heal(Player p, double amount) { if (amount > 0 && !p.isDead()) p.setHealth(Math.min(maxHp(p), p.getHealth() + amount)); } // setHealth không gọi sự kiện hồi máu -> không nhân hai lần

    private boolean isBoss(Entity e) { return e instanceof Boss || plugin.elementer().bossId(e) != null; }

    /** Nguyên tố của kẻ tấn công: nguyên tố Elementer đang dùng (người chơi) hoặc nguyên tố của boss Elementer. */
    private String attackerElement(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof Entity s) d = s;
        if (d instanceof Player p) return plugin.elementer().activeElement(p.getUniqueId());
        String boss = plugin.elementer().bossId(d);
        return boss == null ? null : plugin.getConfig().getString("boss-elements." + boss);
    }

    private static boolean causeMatches(String filter, EntityDamageEvent.DamageCause cause) {
        if (filter == null || filter.isBlank()) return true;
        if (filter.equalsIgnoreCase("FIRE")) return cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
        for (String s : filter.split(",")) if (s.trim().equalsIgnoreCase(cause.name())) return true;
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (inBonus) return;
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player a
                && (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)
                && e.getEntity() instanceof LivingEntity target && !target.equals(a)) attacker(e, a, target);
        if (e.getEntity() instanceof Player v && !e.isCancelled()) defender(e, v);
    }

    // ---------------------------------------------------------------- người đánh
    private void attacker(EntityDamageEvent e, Player a, LivingEntity target) {
        List<PassiveDef> list = plugin.passivesOf(a.getUniqueId());
        Boost b = boosts.get(a.getUniqueId()); long now = System.currentTimeMillis();
        if (list.isEmpty() && (b == null || b.until < now)) return;
        double mult = 1;
        for (PassiveDef p : list) switch (p.type()) {
            case OUTGOING_DAMAGE_PERCENT -> mult += p.value() / 100;
            case DAMAGE_VS_BURNING_PERCENT -> { if (target.getFireTicks() > 0) mult += p.value() / 100; }
            case BOSS_DAMAGE_PERCENT -> { if (isBoss(target)) mult += p.value() / 100; }
            default -> { }
        }
        if (b != null && b.until >= now) mult += b.dmgPct / 100;
        if (mult != 1) e.setDamage(e.getDamage() * mult);
        final double dealt = e.getFinalDamage();
        for (PassiveDef p : list) switch (p.type()) {
            case LIFESTEAL_PERCENT -> heal(a, dealt * p.value() / 100);
            case LIFESTEAL_ON_HIT -> { if (roll(p.chance())) heal(a, dealt * p.value() / 100); }
            case IGNITE_ON_HIT -> { if (roll(p.chance())) target.setFireTicks(Math.max(target.getFireTicks(), p.duration() * 20)); }
            case SLOW_ON_HIT -> { if (roll(p.chance())) { target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, p.duration() * 20, Math.max(0, (int) p.value()), true, true, true));
                target.setFreezeTicks(Math.min(target.getMaxFreezeTicks(), 100)); target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 12, .3, .5, .3, .02); } }
            case WEAKEN_ON_HIT -> { if (roll(p.chance())) { target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, p.duration() * 20, 0, true, true, true));
                target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 10, .3, .5, .3, .02); } }
            case LIGHTNING_ON_HIT -> { if (roll(p.chance())) { target.getWorld().strikeLightningEffect(target.getLocation()); extra(a, target, dealt * p.value() / 100, "LIGHTNING", 0); } }
            case BONUS_DAMAGE_ON_HIT -> { if (roll(p.chance())) extra(a, target, dealt * p.value() / 100, p.filter(), p.duration()); }
            default -> { }
        }
    }

    /** Gây thêm sát thương ở tick sau (tránh đệ quy sự kiện và miễn thương). */
    private void extra(Player a, LivingEntity t, double dmg, String fx, int igniteSec) {
        if (dmg <= 0) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!t.isValid() || t.isDead() || !a.isOnline()) return;
            inBonus = true;
            try { t.setNoDamageTicks(0); t.damage(dmg, a); if (igniteSec > 0) t.setFireTicks(Math.max(t.getFireTicks(), igniteSec * 20)); }
            finally { inBonus = false; }
            Particle part = fx == null ? Particle.CRIT : switch (fx.toUpperCase(Locale.ROOT)) { case "FIRE" -> Particle.FLAME; case "LIGHT" -> Particle.END_ROD;
                case "DARK" -> Particle.SMOKE; case "LIGHTNING" -> Particle.ELECTRIC_SPARK; default -> Particle.CRIT; };
            t.getWorld().spawnParticle(part, t.getLocation().add(0, 1, 0), 14, .3, .5, .3, .05);
        });
    }

    // ---------------------------------------------------------------- người bị đánh
    private void defender(EntityDamageEvent e, Player v) {
        List<PassiveDef> list = plugin.passivesOf(v.getUniqueId());
        Boost b = boosts.get(v.getUniqueId()); long now = System.currentTimeMillis();
        if (list.isEmpty() && (b == null || b.until < now)) return;
        boolean attack = e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK || e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE;
        double reduction = 0;
        for (PassiveDef p : list) switch (p.type()) {
            case DODGE -> { if (attack && roll(p.chance())) { e.setCancelled(true); v.sendActionBar(AstraxTitlesPlugin.AMP.deserialize("&b✦ Né tránh!"));
                v.getWorld().spawnParticle(Particle.CLOUD, v.getLocation().add(0, 1, 0), 10, .3, .5, .3, .03); return; } }
            case DAMAGE_REDUCTION_PERCENT -> { if (causeMatches(p.filter(), e.getCause())) reduction += p.value(); }
            case DAMAGE_REDUCTION_FROM_ELEMENT -> { if (e instanceof EntityDamageByEntityEvent by && p.filter() != null && p.filter().equalsIgnoreCase(attackerElement(by))) reduction += p.value(); }
            default -> { }
        }
        if (b != null && b.until >= now) reduction += b.defPct;
        reduction = Math.min(reduction, plugin.getConfig().getDouble("max-damage-reduction", 80));
        if (reduction > 0) e.setDamage(e.getDamage() * (1 - reduction / 100));
        // nội tại kích hoạt khi máu thấp
        double after = v.getHealth() - e.getFinalDamage();
        if (after <= 0) return;
        for (int i = 0; i < list.size(); i++) {
            PassiveDef p = list.get(i);
            if ((p.type() == PassiveType.LOW_HP_BOOST || p.type() == PassiveType.LOW_HP_HEAL) && after <= maxHp(v) * p.threshold() / 100) trigger(v, p, i);
        }
    }

    private void trigger(Player v, PassiveDef p, int idx) {
        long now = System.currentTimeMillis();
        String key = p.type() + "#" + idx;
        Map<String, Long> cd = cooldowns.computeIfAbsent(v.getUniqueId(), k -> new HashMap<>());
        if (cd.getOrDefault(key, 0L) > now) return;
        cd.put(key, now + p.cooldown() * 1000L);
        final String nm = p.name() == null || p.name().isBlank() ? "Nội tại" : p.name();
        if (p.type() == PassiveType.LOW_HP_BOOST) {
            boosts.put(v.getUniqueId(), new Boost(now + p.duration() * 1000L, p.value(), p.value2()));
            v.sendMessage(AstraxTitlesPlugin.AMP.deserialize("&c⚡ " + nm + " &7kích hoạt (" + p.duration() + "s)!"));
            v.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, v.getLocation().add(0, 2, 0), 6, .4, .3, .4, 0);
        } else Bukkit.getScheduler().runTask(plugin, () -> { // hồi máu + tăng tốc ở tick sau
            if (!v.isOnline() || v.isDead()) return;
            heal(v, maxHp(v) * p.value() / 100);
            if (p.value2() > 0 && p.duration() > 0) speed(v, p.value2() / 100, p.duration());
            v.sendMessage(AstraxTitlesPlugin.AMP.deserialize("&e✦ " + nm + " &7kích hoạt!"));
            v.getWorld().spawnParticle(Particle.END_ROD, v.getLocation().add(0, 1, 0), 20, .4, .6, .4, .03);
        });
    }

    private void speed(Player v, double amount, int seconds) {
        AttributeInstance inst = v.getAttribute(Attribute.MOVEMENT_SPEED); if (inst == null) return;
        clearSpeed(v);
        inst.addTransientModifier(new AttributeModifier(speedKey, amount, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
        speedTasks.put(v.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> clearSpeed(v), seconds * 20L));
    }
    private void clearSpeed(Player v) {
        BukkitTask t = speedTasks.remove(v.getUniqueId()); if (t != null) t.cancel();
        AttributeInstance inst = v.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst != null) for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) if (m.getKey().equals(speedKey)) inst.removeModifier(m);
    }

    @EventHandler(ignoreCancelled = true) public void onHeal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        double pct = 0;
        for (PassiveDef d : plugin.passivesOf(p.getUniqueId())) if (d.type() == PassiveType.HEAL_RECEIVED_PERCENT) pct += d.value();
        if (pct != 0) e.setAmount(e.getAmount() * (1 + pct / 100));
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { UUID id = e.getPlayer().getUniqueId(); clearSpeed(e.getPlayer()); boosts.remove(id); cooldowns.remove(id); }
    /** Gọi khi đổi/gỡ danh hiệu: bỏ trạng thái tạm của danh hiệu cũ. */
    public void reset(Player p) { clearSpeed(p); boosts.remove(p.getUniqueId()); cooldowns.remove(p.getUniqueId()); }
}
