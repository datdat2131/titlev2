package vn.astrax.titles.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import vn.astrax.titles.AstraxTitlesPlugin;

/** Chèn danh hiệu vào chat: "[Title] Tên: nội dung". Tắt bằng chat.enabled=false nếu dùng plugin chat khác + PlaceholderAPI. */
public final class ChatListener implements Listener {
    private final AstraxTitlesPlugin plugin;
    public ChatListener(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("chat.enabled", false)) return;
        final String format = plugin.getConfig().getString("chat.format", "{title}{player}&7: &f{message}");
        e.renderer((source, displayName, message, viewer) -> {
            Component out = Component.empty(); StringBuilder lit = new StringBuilder(); int i = 0;
            while (i < format.length()) {
                int j = format.indexOf('{', i);
                if (j < 0) { lit.append(format, i, format.length()); break; }
                int k = format.indexOf('}', j);
                if (k < 0) { lit.append(format, i, format.length()); break; }
                lit.append(format, i, j);
                String token = format.substring(j + 1, k);
                Component rep = switch (token) { case "title" -> plugin.chatPrefix(source.getUniqueId()); case "player" -> displayName; case "message" -> message; default -> null; };
                if (rep == null) lit.append(format, j, k + 1);
                else { if (!lit.isEmpty()) { out = out.append(AstraxTitlesPlugin.AMP.deserialize(lit.toString())); lit.setLength(0); } out = out.append(rep); }
                i = k + 1;
            }
            if (!lit.isEmpty()) out = out.append(AstraxTitlesPlugin.AMP.deserialize(lit.toString()));
            return out;
        });
    }
}
