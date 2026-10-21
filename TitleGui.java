package vn.astrax.titles.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.*;

import java.util.*;

/** GUI danh hiệu phân trang: đã mở = bấm để đeo/gỡ; chưa mở = hiện tiến độ. */
public final class TitleGui implements Listener {
    private static final int PER_PAGE = 45;
    private static final class Holder implements InventoryHolder {
        int page; final Map<Integer, String> slots = new HashMap<>(); Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }
    private final AstraxTitlesPlugin plugin;
    private final Map<UUID, Long> lastClick = new HashMap<>();
    public TitleGui(AstraxTitlesPlugin plugin) { this.plugin = plugin; }
    public void forget(UUID id) { lastClick.remove(id); }

    private static Component c(String s) { return AstraxTitlesPlugin.AMP.deserialize(s).decoration(TextDecoration.ITALIC, false); }
    private static Component raw(String s) { return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(s).decoration(TextDecoration.ITALIC, false); }
    private static String bar(double f, int n) { int k = (int) Math.round(Math.max(0, Math.min(1, f)) * n); return "§a" + "█".repeat(k) + "§8" + "█".repeat(n - k); }

    public void open(Player p, int page) {
        PlayerTitleData d = plugin.data(p.getUniqueId()); if (d == null) { p.sendMessage("§cDữ liệu chưa tải xong."); return; }
        List<TitleDef> list = new ArrayList<>();
        for (TitleDef t : plugin.titles().all()) if (!t.hidden() || d.has(t.id())) list.add(t);
        list.sort(Comparator.comparing((TitleDef t) -> !d.has(t.id())).thenComparing(t -> -t.rarity().ordinal())); // đã mở lên trước
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE); page = Math.max(0, Math.min(page, pages - 1));
        Holder h = new Holder(); h.page = page;
        h.inv = Bukkit.createInventory(h, 54, raw("§6✦ Danh hiệu §8(" + (page + 1) + "/" + pages + ") §7" + d.unlocked().size() + "/" + plugin.titles().size()));
        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < list.size(); i++) {
            TitleDef t = list.get(page * PER_PAGE + i); h.inv.setItem(i, icon(t, d)); h.slots.put(i, t.id());
        }
        h.inv.setItem(45, nav("§eTrang trước", page > 0)); h.inv.setItem(53, nav("§eTrang sau", page < pages - 1));
        ItemStack off = new ItemStack(Material.BARRIER); ItemMeta om = off.getItemMeta(); om.displayName(raw("§cGỡ danh hiệu")); off.setItemMeta(om); h.inv.setItem(49, off);
        p.openInventory(h.inv);
    }
    private ItemStack nav(String name, boolean on) {
        ItemStack it = new ItemStack(on ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE); ItemMeta m = it.getItemMeta(); m.displayName(raw(on ? name : " ")); it.setItemMeta(m); return it;
    }

    private ItemStack icon(TitleDef t, PlayerTitleData d) {
        boolean has = d.has(t.id()), eq = t.id().equalsIgnoreCase(d.selected());
        Material m = Material.matchMaterial(t.icon()); if (m == null || !m.isItem()) m = Material.NAME_TAG;
        ItemStack it = new ItemStack(has ? m : Material.GRAY_DYE); ItemMeta meta = it.getItemMeta();
        meta.displayName(c((has ? "" : "&8") + t.display()));
        List<Component> lore = new ArrayList<>();
        lore.add(raw(t.rarity().color() + t.rarity().display()));
        if (!t.description().isBlank()) lore.add(c("&7" + t.description()));
        if (!has && !t.requirements().isEmpty()) { // nhiệm vụ + tiến độ
            lore.add(Component.empty()); lore.add(raw("§bNhiệm vụ:"));
            for (int i = 0; i < t.requirements().size(); i++) {
                Requirement r = t.requirements().get(i); long cur = Math.min(d.progressOf(Requirement.reqKey(t.id(), i)), r.target()); boolean done = cur >= r.target();
                lore.add(raw((done ? "§a ✔ " : "§c ✘ ") + "§f" + r.describe() + (!done && r.type().counted() && r.target() > 1 ? " §7(" + cur + "/" + r.target() + ")" : "")));
            }
        } else if (!has) { lore.add(Component.empty()); lore.add(raw("§c✘ Nhận từ sự kiện/admin")); }
        if (!t.buffs().isEmpty()) { lore.add(Component.empty()); lore.add(raw("§6Buff khi đeo:")); t.buffs().forEach((k, v) -> lore.add(raw("§e • " + pretty(k) + " §f" + v.describe()))); }
        if (!t.passives().isEmpty()) { lore.add(Component.empty()); lore.add(raw("§dNội tại:")); for (PassiveDef p : t.passives()) lore.add(raw("§d • §7" + p.describe())); }
        lore.add(Component.empty());
        if (has) lore.add(raw(eq ? "§a✔ Đang đeo §7(bấm để gỡ)" : "§eBấm để đeo"));
        meta.lore(lore);
        if (eq) { meta.addEnchant(Enchantment.UNBREAKING, 1, true); meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); }
        it.setItemMeta(meta); return it;
    }
    private static String pretty(String attr) {
        return switch (attr) { case "max_health" -> "Máu tối đa"; case "attack_damage" -> "Sát thương"; case "movement_speed" -> "Tốc độ chạy"; case "armor" -> "Giáp";
            case "armor_toughness" -> "Độ cứng giáp"; case "luck" -> "May mắn"; case "attack_speed" -> "Tốc độ tấn công"; case "knockback_resistance" -> "Kháng đẩy lùi";
            case "block_break_speed" -> "Tốc độ đào"; case "jump_strength" -> "Lực nhảy"; default -> attr; };
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() < 0 || e.getRawSlot() >= 54) return;
        long now = System.currentTimeMillis(); if (now - lastClick.getOrDefault(p.getUniqueId(), 0L) < 200) return; lastClick.put(p.getUniqueId(), now); // chống spam
        int s = e.getRawSlot();
        if (s == 45 && h.page > 0) { open(p, h.page - 1); return; }
        if (s == 53) { open(p, h.page + 1); return; }
        if (s == 49) { plugin.unequip(p); p.sendMessage("§7Đã gỡ danh hiệu."); open(p, h.page); return; }
        String id = h.slots.get(s); if (id == null) return;
        PlayerTitleData d = plugin.data(p.getUniqueId()); TitleDef t = plugin.titles().get(id); if (d == null || t == null) return;
        if (!d.has(id)) { p.sendMessage("§cBạn chưa mở khóa danh hiệu này."); return; }
        if (id.equalsIgnoreCase(d.selected())) { plugin.unequip(p); p.sendMessage("§7Đã gỡ danh hiệu."); }
        else if (plugin.equip(p, id)) p.sendMessage(Component.text("§aĐã đeo: ").append(AstraxTitlesPlugin.AMP.deserialize(t.display())));
        open(p, h.page);
    }
    @EventHandler public void onDrag(InventoryDragEvent e) { if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true); }
}
