package vn.astrax.titles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.astrax.titles.buff.BuffManager;
import vn.astrax.titles.commands.TitleAdminCommand;
import vn.astrax.titles.commands.TitleCommand;
import vn.astrax.titles.config.TitleLoader;
import vn.astrax.titles.gui.TitleGui;
import vn.astrax.titles.hook.PapiHook;
import vn.astrax.titles.hook.ElementerBridge;
import vn.astrax.titles.hook.NguyenToBridge;
import vn.astrax.titles.listener.BossListener;
import vn.astrax.titles.listener.ChatListener;
import vn.astrax.titles.listener.PassiveListener;
import vn.astrax.titles.listener.TravelTracker;
import vn.astrax.titles.listener.ProgressListener;
import vn.astrax.titles.model.*;
import vn.astrax.titles.storage.DataStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class AstraxTitlesPlugin extends JavaPlugin {
    public static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    private final TitleRegistry registry = new TitleRegistry();
    private final Map<UUID, PlayerTitleData> cache = new ConcurrentHashMap<>();
    private DataStore store; private BuffManager buffs; private TitleGui gui;
    private final ElementerBridge elementer = new ElementerBridge(getLogger());
    private final NguyenToBridge nguyento = new NguyenToBridge();
    private PassiveListener passives; private BossListener bosses; private TravelTracker travel;
    private int autosaveTask = -1, playtimeTask = -1, pollTask = -1;

    @Override public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(getDataFolder(), getLogger());
        buffs = new BuffManager(this); gui = new TitleGui(this);
        reloadAll();
        var pm = getServer().getPluginManager();
        passives = new PassiveListener(this); bosses = new BossListener(this); travel = new TravelTracker(this);
        pm.registerEvents(new ProgressListener(this), this); pm.registerEvents(passives, this); pm.registerEvents(bosses, this); pm.registerEvents(travel, this); pm.registerEvents(new ChatListener(this), this); pm.registerEvents(gui, this);
        var tc = new TitleCommand(this); var ac = new TitleAdminCommand(this);
        Objects.requireNonNull(getCommand("title")).setExecutor(tc); getCommand("title").setTabCompleter(tc);
        Objects.requireNonNull(getCommand("titleadmin")).setExecutor(ac); getCommand("titleadmin").setTabCompleter(ac);
        for (Player p : Bukkit.getOnlinePlayers()) loadBlocking(p);
        long every = Math.max(1, getConfig().getInt("autosave-minutes", 5)) * 1200L;
        autosaveTask = Bukkit.getScheduler().runTaskTimer(this, () -> { for (PlayerTitleData d : cache.values()) if (d.dirty()) store.saveAsync(d); }, every, every).getTaskId();
        playtimeTask = Bukkit.getScheduler().runTaskTimer(this, this::tickPlaytime, 1200L, 1200L).getTaskId();
        Bukkit.getScheduler().runTaskTimer(this, this::tickTravel, 40L, 40L);
        long poll = Math.max(1, getConfig().getInt("poll-seconds", 5)) * 20L; // kiểm tra nhiệm vụ mở khóa nguyên tố của plugin khác
        pollTask = Bukkit.getScheduler().runTaskTimer(this, () -> { for (Player p : Bukkit.getOnlinePlayers()) checkStates(p); bosses.purge(); }, 100L, poll).getTaskId();
        if (!elementer.available()) getLogger().info("Chưa thấy AstraxElementer: nhiệm vụ ELEMENTER_UNLOCK sẽ đạt khi plugin đó chạy.");
        if (!nguyento.available()) getLogger().info("Chưa thấy NguyenTo: nhiệm vụ NGUYENTO_UNLOCK sẽ đạt khi plugin đó chạy.");
        if (pm.isPluginEnabled("PlaceholderAPI")) { new PapiHook(this).register(); getLogger().info("Đã đăng ký PlaceholderAPI (%astraxtitles_*%)."); }
        getLogger().info("AstraxTitles bật: " + registry.size() + " danh hiệu.");
    }

    @Override public void onDisable() {
        if (autosaveTask != -1) Bukkit.getScheduler().cancelTask(autosaveTask);
        if (playtimeTask != -1) Bukkit.getScheduler().cancelTask(playtimeTask);
        if (pollTask != -1) Bukkit.getScheduler().cancelTask(pollTask);
        for (Player p : Bukkit.getOnlinePlayers()) { buffs.remove(p); Component base = tabBase.get(p.getUniqueId()); if (base != null) p.playerListName(base); } // trả tên Tab về như cũ
        for (PlayerTitleData d : cache.values()) store.saveAsync(d);
        cache.clear(); store.close();
    }

    public void reloadAll() {
        reloadConfig(); new TitleLoader(this).load(registry);
        for (TitleDef t : registry.all()) for (String a : t.buffs().keySet())
            if (!buffs.validAttribute(a)) getLogger().warning("titles.yml: '" + t.id() + "' dùng attribute không tồn tại: " + a);
        for (Player p : Bukkit.getOnlinePlayers()) { PlayerTitleData d = cache.get(p.getUniqueId()); if (d != null && d.selected() != null && registry.get(d.selected()) == null) d.select(null); buffs.apply(p); refreshTab(p); }
    }

    /** Playtime: +1 phút cho mỗi người chơi online. */
    private void tickTravel() { travel.tick(Bukkit.getOnlinePlayers()); for (Player p : Bukkit.getOnlinePlayers()) tabGuard(p); }
    private void tickPlaytime() {
        for (Player p : Bukkit.getOnlinePlayers()) progress(p, AchievementType.PLAYTIME_MINUTES, "MINUTE", 1);
    }

    public ElementerBridge elementer() { return elementer; }
    public NguyenToBridge nguyento() { return nguyento; }
    /** Nội tại của danh hiệu đang đeo (rỗng nếu không có). */
    public List<PassiveDef> passivesOf(UUID id) {
        PlayerTitleData d = cache.get(id); TitleDef t = d == null ? null : registry.get(d.selected());
        return t == null ? List.of() : t.passives();
    }
    /** Kiểm tra nhiệm vụ dạng trạng thái (mở khóa nguyên tố ở Elementer/NguyenTo, permission) và mở khóa danh hiệu đủ điều kiện. */
    public void checkStates(Player p) {
        PlayerTitleData d = cache.get(p.getUniqueId()); if (d == null) return;
        for (TitleDef t : registry.checkStates(d, (type, filter, def) -> switch (type) {
            case PERMISSION -> p.hasPermission("astraxtitles.title." + def.id());
            case ELEMENTER_UNLOCK -> elementer.hasUnlocked(p.getUniqueId(), filter);
            case NGUYENTO_UNLOCK -> nguyento.owns(p, filter);
            case PERMISSION_NODE -> Requirement.nodes(filter).stream().anyMatch(p::hasPermission);
            default -> false; })) onUnlocked(p, t);
    }
    /** Chuỗi hạ gục: tăng khi hạ gục, reset khi chết. Lưu trong dữ liệu người chơi (không mất khi restart). */
    public void streakKill(Player p, boolean playerKill) {
        PlayerTitleData d = cache.get(p.getUniqueId()); if (d == null) return;
        long any = d.progressOf("_streak~any") + 1; d.setProgress("_streak~any", any);
        for (TitleDef t : registry.progressMax(d, AchievementType.KILL_STREAK, "ANY", any)) onUnlocked(p, t);
        if (playerKill) {
            long pl = d.progressOf("_streak~player") + 1; d.setProgress("_streak~player", pl);
            for (TitleDef t : registry.progressMax(d, AchievementType.KILL_STREAK, "PLAYER", pl)) onUnlocked(p, t);
        }
    }
    public void resetStreak(Player p) {
        PlayerTitleData d = cache.get(p.getUniqueId()); if (d == null) return;
        if (d.progressOf("_streak~any") != 0) d.setProgress("_streak~any", 0);
        if (d.progressOf("_streak~player") != 0) d.setProgress("_streak~player", 0);
    }
    public TitleRegistry titles() { return registry; }
    public BuffManager buffs() { return buffs; }
    public DataStore store() { return store; }
    public TitleGui gui() { return gui; }
    public PlayerTitleData data(UUID id) { return cache.get(id); }
    public void putData(PlayerTitleData d) { cache.put(d.owner(), d); }
    public PlayerTitleData removeData(UUID id) { return cache.remove(id); }

    private void loadBlocking(Player p) {
        try { PlayerTitleData d = store.loadAsync(p.getUniqueId()).get(10, TimeUnit.SECONDS); cache.put(p.getUniqueId(), d == null ? new PlayerTitleData(p.getUniqueId()) : d); buffs.apply(p); }
        catch (Exception e) { getLogger().severe("Không tải được dữ liệu " + p.getName() + ": " + e); }
    }

    /** Ghi nhận tiến độ + thông báo mở khóa. Gọi trên main thread. */
    public void progress(Player p, AchievementType type, String key, long amount) {
        PlayerTitleData d = cache.get(p.getUniqueId()); if (d == null) return;
        for (TitleDef t : registry.progress(d, type, key, amount)) onUnlocked(p, t);
    }

    public void onUnlocked(Player p, TitleDef t) {
        Component name = AMP.deserialize(t.display());
        p.sendMessage(Component.text("§a✦ Mở khóa danh hiệu: ").append(name).append(Component.text(" §7(/title để đeo)")));
        p.showTitle(Title.title(Component.text("§6§lTHÀNH TỰU"), name));
        p.playSound(p.getLocation(), t.rarity().atLeast(Rarity.LEGENDARY) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        Rarity min = Rarity.parse(getConfig().getString("announce-min-rarity", "LEGENDARY"));
        if (t.announce() || t.rarity().atLeast(min))
            Bukkit.broadcast(Component.text("§e" + p.getName() + " §7vừa đạt danh hiệu ").append(name).append(Component.text("§7!")));
        if (d(p) != null && d(p).selected() == null && getConfig().getBoolean("auto-equip-first", true)) equip(p, t.id());
    }
    private PlayerTitleData d(Player p) { return cache.get(p.getUniqueId()); }

    public boolean equip(Player p, String id) {
        PlayerTitleData d = cache.get(p.getUniqueId()); if (d == null || !d.select(id)) return false;
        passives.reset(p); buffs.apply(p); refreshTab(p); return true;
    }
    public void unequip(Player p) { PlayerTitleData d = cache.get(p.getUniqueId()); if (d != null) { d.select(null); passives.reset(p); buffs.apply(p); refreshTab(p); } }

    /** Danh hiệu dạng tiền tố "danh hiệu | " (chat hoặc Tab kiểu BEFORE). Rỗng nếu không đeo. */
    public Component prefix(UUID id) { return build(id, "tab", "title-format", "%title% &8| &r"); }
    /** Danh hiệu dạng hậu tố " | danh hiệu" - dùng cho Tab: "[Rank] Tên | danh hiệu" và placeholder %astraxtitles_suffix%. */
    public Component suffix(UUID id) { return build(id, "tab", "suffix-format", " &8| %title%"); }
    /** Prefix dùng cho chat (chỉ khi chat.enabled=true). */
    public Component chatPrefix(UUID id) { return build(id, "chat", "title-format", "&8[%title%&8] "); }

    private Component build(UUID id, String section, String formatKey, String def) {
        PlayerTitleData d = cache.get(id); TitleDef t = d == null ? null : registry.get(d.selected());
        if (t == null) return Component.empty();
        String shown = getConfig().getBoolean(section + ".small-caps", true) ? SmallCaps.convert(t.display()) : t.display();
        return AMP.deserialize(getConfig().getString(section + "." + formatKey, def).replace("%title%", shown));
    }

    private final Map<UUID, Component> tabBase = new HashMap<>();
    private final Map<UUID, String> tabApplied = new HashMap<>();
    private static String plain(Component c) { return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c); }

    /**
     * Chế độ dựng sẵn (khi KHÔNG dùng plugin TAB): thêm danh hiệu vào tên trong Tab, mặc định phía SAU tên: "[Rank] Tên | danh hiệu"
     * (tab.position: BEFORE để đặt phía trước). Không ghi đè tên gốc: giữ nguyên phần plugin khác (rank) đã đặt. Nếu plugin khác đổi lại tên, lần kiểm tra kế tiếp sẽ chèn lại.
     */
    public void refreshTab(Player p) {
        if (!getConfig().getBoolean("tab.enabled", true)) return;
        UUID id = p.getUniqueId();
        Component cur = p.playerListName(); if (cur == null) cur = Component.text(p.getName());
        Component base = (tabApplied.containsKey(id) && plain(cur).equals(tabApplied.get(id)) && tabBase.containsKey(id)) ? tabBase.get(id) : cur; // còn nguyên của mình -> dùng lại tên gốc, tránh chèn đôi
        boolean before = "BEFORE".equalsIgnoreCase(getConfig().getString("tab.position", "AFTER"));
        Component result = before ? prefix(id).append(base) : base.append(suffix(id));
        tabBase.put(id, base); tabApplied.put(id, plain(result));
        p.playerListName(result);
    }
    /** Nếu plugin khác ghi đè tên trong Tab thì chèn lại danh hiệu. */
    private void tabGuard(Player p) {
        if (!getConfig().getBoolean("tab.enabled", true)) return;
        Component cur = p.playerListName(); String applied = tabApplied.get(p.getUniqueId());
        if (applied == null || cur == null || !applied.equals(plain(cur))) refreshTab(p);
    }
    public void forgetTab(UUID id) { tabBase.remove(id); tabApplied.remove(id); }
}
