package vn.astrax.titles.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.*;

import java.util.*;

public final class TitleCommand implements CommandExecutor, TabCompleter {
    private final AstraxTitlesPlugin plugin;
    public TitleCommand(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Chỉ người chơi dùng được."); return true; }
        PlayerTitleData d = plugin.data(p.getUniqueId());
        if (d == null) { p.sendMessage("§cDữ liệu chưa tải xong."); return true; }
        String sub = a.length == 0 ? "menu" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu", "gui" -> plugin.gui().open(p, 0);
            case "off", "none" -> { plugin.unequip(p); p.sendMessage("§7Đã gỡ danh hiệu."); }
            case "equip", "set" -> {
                if (a.length < 2) { p.sendMessage("§eDùng: /title equip <id>"); return true; }
                TitleDef t = plugin.titles().get(a[1]);
                if (t == null || !plugin.equip(p, a[1])) { p.sendMessage("§cBạn chưa sở hữu danh hiệu đó."); return true; }
                p.sendMessage(Component.text("§aĐã đeo: ").append(AstraxTitlesPlugin.AMP.deserialize(t.display())));
            }
            case "list" -> {
                p.sendMessage("§6Danh hiệu đã mở (" + d.unlocked().size() + "/" + plugin.titles().size() + "):");
                for (String id : d.unlocked()) { TitleDef t = plugin.titles().get(id); if (t != null) p.sendMessage(Component.text("§7 - " + id + " ").append(AstraxTitlesPlugin.AMP.deserialize(t.display()))); }
            }
            default -> p.sendMessage("§e/title §7[menu | equip <id> | off | list]");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("menu", "equip", "off", "list");
        if (a.length == 2 && a[0].equalsIgnoreCase("equip") && s instanceof Player p) { PlayerTitleData d = plugin.data(p.getUniqueId()); return d == null ? List.of() : new ArrayList<>(d.unlocked()); }
        return List.of();
    }
}
