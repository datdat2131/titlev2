package vn.astrax.titles.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.*;

/** %astraxtitles_suffix% (cho tabsuffix), %astraxtitles_prefix%, %astraxtitles_title%, %astraxtitles_id%, %astraxtitles_count% - đã có màu § */
public final class PapiHook extends PlaceholderExpansion {
    private final AstraxTitlesPlugin plugin;
    public PapiHook(AstraxTitlesPlugin plugin) { this.plugin = plugin; }
    @Override public String getIdentifier() { return "astraxtitles"; }
    @Override public String getAuthor() { return "Astrax"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        PlayerTitleData d = plugin.data(player.getUniqueId()); if (d == null) return "";
        TitleDef t = plugin.titles().get(d.selected());
        LegacyComponentSerializer sec = LegacyComponentSerializer.legacySection();
        return switch (params.toLowerCase()) {
            case "suffix" -> sec.serialize(plugin.suffix(player.getUniqueId()));   // " | ✦ ᴛᴀɴ ᴛʜᴜ" (rỗng nếu không đeo) - dùng cho tabsuffix
            case "prefix" -> sec.serialize(plugin.prefix(player.getUniqueId()));   // "✦ ᴛᴀɴ ᴛʜᴜ | "
            case "title" -> t == null ? "" : sec.serialize(AstraxTitlesPlugin.AMP.deserialize(t.display()));
            case "id" -> t == null ? "" : t.id();
            case "count" -> String.valueOf(d.unlocked().size());
            default -> null;
        };
    }
}
