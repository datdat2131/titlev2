package vn.astrax.titles.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.*;

import java.util.*;
import java.util.function.Consumer;

public final class TitleAdminCommand implements CommandExecutor, TabCompleter {
    private final AstraxTitlesPlugin plugin;
    public TitleAdminCommand(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("astraxtitles.admin")) { s.sendMessage("§cBạn không có quyền."); return true; }
        if (a.length == 0) { s.sendMessage("§e/titleadmin §7reload | list | give <p> <id> | take <p> <id> | progress <p> <id> <nhiệm vụ#> <số> | check <p> | set <id> <trường> <giá trị> | buff <id> <attr> <giá trị|remove>"); return true; }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> { plugin.reloadAll(); s.sendMessage("§aĐã tải lại: " + plugin.titles().size() + " danh hiệu."); }
            case "list" -> s.sendMessage("§7Danh hiệu: §f" + String.join(", ", plugin.titles().all().stream().map(TitleDef::id).toList()));
            case "give" -> { if (a.length < 3) return usage(s, "give <player> <id>"); TitleDef t = need(s, a[2]); if (t == null) return true;
                edit(s, a[1], d -> d.unlock(t.id()), p -> plugin.onUnlocked(p, t), "Đã cấp " + t.id()); }
            case "take" -> { if (a.length < 3) return usage(s, "take <player> <id>"); TitleDef t = need(s, a[2]); if (t == null) return true;
                edit(s, a[1], d -> d.lock(t.id()), p -> { plugin.buffs().apply(p); plugin.refreshTab(p); }, "Đã thu hồi " + t.id()); }
            case "progress" -> { if (a.length < 5) return usage(s, "progress <player> <id> <nhiệm vụ số 1,2..> <số>"); TitleDef t = need(s, a[2]); if (t == null) return true;
                int idx; long n; try { idx = Integer.parseInt(a[3]) - 1; n = Long.parseLong(a[4]); } catch (NumberFormatException e) { s.sendMessage("§cSố không hợp lệ."); return true; }
                if (idx < 0 || idx >= t.requirements().size()) { s.sendMessage("§cDanh hiệu này có " + t.requirements().size() + " nhiệm vụ."); return true; }
                edit(s, a[1], d -> { d.setProgress(Requirement.reqKey(t.id(), idx), n); if (plugin.titles().complete(d, t)) d.unlock(t.id()); }, p -> plugin.checkStates(p), "Đã đặt tiến độ " + t.id() + " #" + (idx + 1) + " = " + n); }
            case "set" -> { if (a.length < 4) return usage(s, "set <id> <display|description|rarity|icon> <giá trị...>");
                String err = new vn.astrax.titles.config.TitleEditor(plugin).setField(a[1], a[2], String.join(" ", Arrays.copyOfRange(a, 3, a.length)));
                if (err != null) s.sendMessage("§c" + err); else { plugin.reloadAll(); s.sendMessage("§aĐã cập nhật " + a[1] + " (" + a[2] + "). Bản sao lưu: titles.yml.bak"); } }
            case "buff" -> { if (a.length < 4) return usage(s, "buff <id> <attribute> <giá trị|remove>   vd: buff do_sat attack_damage 2.5  |  buff tan_thu movement_speed 10%");
                var ed = new vn.astrax.titles.config.TitleEditor(plugin);
                if (!a[3].equalsIgnoreCase("remove") && !plugin.buffs().validAttribute(a[2].toLowerCase().replace("generic.", ""))) { s.sendMessage("§cAttribute không tồn tại: " + a[2]); return true; }
                String err = a[3].equalsIgnoreCase("remove") ? ed.removeBuff(a[1], a[2]) : ed.setBuff(a[1], a[2], a[3]);
                if (err != null) s.sendMessage("§c" + err); else { plugin.reloadAll(); s.sendMessage("§aĐã cập nhật buff " + a[2] + " của " + a[1] + ". Bản sao lưu: titles.yml.bak"); } }
            case "check" -> { if (a.length < 2) return usage(s, "check <player>"); Player p = Bukkit.getPlayerExact(a[1]); if (p == null) s.sendMessage("§cNgười chơi không online."); else { plugin.checkStates(p);
                s.sendMessage("§7Elementer: §f" + (plugin.elementer().available() ? "đã kết nối" : "không có/không đọc được") + " §7| NguyenTo: §f" + (plugin.nguyento().available() ? "đã kết nối" : "không có")); } }
            default -> s.sendMessage("§cLệnh không hợp lệ.");
        }
        return true;
    }
    private boolean usage(CommandSender s, String u) { s.sendMessage("§eDùng: /titleadmin " + u); return true; }
    private TitleDef need(CommandSender s, String id) { TitleDef t = plugin.titles().get(id); if (t == null) s.sendMessage("§cKhông có danh hiệu '" + id + "'."); return t; }

    /** Online: sửa cache; offline: đọc-sửa-ghi trên thread IO. */
    private void edit(CommandSender s, String name, Consumer<PlayerTitleData> change, Consumer<Player> afterOnline, String ok) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) { PlayerTitleData d = plugin.data(online.getUniqueId()); if (d == null) { s.sendMessage("§cDữ liệu chưa tải."); return; }
            change.accept(d); d.markDirty(); plugin.store().saveAsync(d); if (afterOnline != null) afterOnline.accept(online); s.sendMessage("§a" + ok + " cho " + name); return; }
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
        if (off == null) { s.sendMessage("§cKhông tìm thấy người chơi."); return; }
        plugin.store().modifyOffline(off.getUniqueId(), change).thenAccept(r -> Bukkit.getScheduler().runTask(plugin, () -> s.sendMessage(r ? "§a" + ok + " cho " + name + " §7(offline)" : "§cNgười chơi chưa có dữ liệu.")));
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("reload", "list", "give", "take", "progress", "check", "set", "buff");
        if (a.length == 2 && a[0].matches("(?i)give|take|progress|check")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (a.length == 2 && a[0].matches("(?i)set|buff")) return plugin.titles().all().stream().map(TitleDef::id).toList();
        if (a.length == 3 && a[0].equalsIgnoreCase("set")) return List.of("display", "description", "rarity", "icon");
        if (a.length == 3 && a[0].equalsIgnoreCase("buff")) return List.of("max_health", "attack_damage", "attack_speed", "movement_speed", "armor", "block_break_speed", "luck");
        if (a.length == 3) return plugin.titles().all().stream().map(TitleDef::id).toList();
        return List.of();
    }
}
