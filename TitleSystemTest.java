package vn.astrax.titles;

import org.junit.jupiter.api.Test;
import vn.astrax.titles.config.TitleMerger;
import vn.astrax.titles.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TitleSystemTest {
    static Requirement count(AchievementType t, long target, String filter) { return new Requirement(t, filter, target, null); }
    static Requirement state(AchievementType t, String filter) { return new Requirement(t, filter, 1, null); }
    static TitleDef def(String id, Rarity r, Requirement... reqs) { return new TitleDef(id, "&a" + id, "", r, "NAME_TAG", List.of(reqs), Map.of("max_health", new BuffValue(2, false)), List.of(), false, false); }
    static TitleRegistry reg() {
        TitleRegistry r = new TitleRegistry();
        r.register(def("zombie", Rarity.COMMON, count(AchievementType.KILL_MOB, 3, "ZOMBIE")));
        r.register(def("anymob", Rarity.RARE, count(AchievementType.KILL_MOB, 5, null)));
        r.register(def("pvp", Rarity.RARE, count(AchievementType.KILL_PLAYER, 2, null)));
        r.register(def("hoa", Rarity.RARE, state(AchievementType.ELEMENTER_UNLOCK, "FIRE"), state(AchievementType.NGUYENTO_UNLOCK, "lua")));
        r.register(def("dark", Rarity.MYTHIC, state(AchievementType.ELEMENTER_UNLOCK, "DARK"), state(AchievementType.NGUYENTO_UNLOCK, "bong_toi"), count(AchievementType.KILL_PLAYER, 3, null)));
        r.register(def("dragon", Rarity.LEGENDARY, count(AchievementType.BOSS_KILL, 1, "dragon_lord"), state(AchievementType.ELEMENTER_UNLOCK, "DRAGON")));
        r.register(def("vip", Rarity.EPIC, state(AchievementType.PERMISSION, null)));
        r.register(def("manual", Rarity.MYTHIC));
        r.register(def("streak", Rarity.LEGENDARY, count(AchievementType.KILL_STREAK, 3, "PLAYER")));
        r.register(def("rank", Rarity.EPIC, new Requirement(AchievementType.PERMISSION_NODE, "group.vip3", 1, null)));
        r.register(def("walker", Rarity.RARE, count(AchievementType.TRAVEL_BLOCKS, 100, null)));
        r.register(def("bosses", Rarity.EPIC, count(AchievementType.BOSS_KILL, 3, null)));
        r.register(def("miner", Rarity.RARE, count(AchievementType.BREAK_BLOCK, 3, null), count(AchievementType.KILL_PLAYER, 2, null)));
        return r;
    }
    static StateProbe probe(Set<String> has) { return (t, f, d) -> has.contains(t + ":" + f) || has.contains(t + ":" + d.id()); }

    @Test void buffValueParsing() {
        assertEquals(4.0, BuffValue.parse("4").amount()); BuffValue p = BuffValue.parse("5%"); assertTrue(p.percent()); assertEquals(0.05, p.amount(), 1e-9);
        assertEquals("+5%", p.describe()); assertEquals("+0.15", BuffValue.parse("0.15").describe());
        assertThrows(IllegalArgumentException.class, () -> BuffValue.parse("abc"));
    }
    @Test void typeClassification() {
        assertTrue(AchievementType.BOSS_KILL.counted()); assertTrue(AchievementType.ELEMENTER_UNLOCK.state()); assertTrue(AchievementType.NGUYENTO_UNLOCK.state());
        assertFalse(AchievementType.MANUAL.counted()); assertFalse(AchievementType.MANUAL.state()); assertNull(AchievementType.parse("nope"));
    }
    @Test void requirementDescriptionsNameTheRightPlugin() {
        assertTrue(state(AchievementType.ELEMENTER_UNLOCK, "FIRE").describe().contains("Elementer")); assertTrue(state(AchievementType.ELEMENTER_UNLOCK, "FIRE").describe().contains("Hỏa"));
        assertTrue(state(AchievementType.NGUYENTO_UNLOCK, "lua").describe().contains("NguyenTo")); assertTrue(state(AchievementType.NGUYENTO_UNLOCK, "lua").describe().contains("Lửa"));
        assertEquals("Hạ gục 10 người chơi", count(AchievementType.KILL_PLAYER, 10, null).describe()); assertEquals("Online đủ 15 phút", count(AchievementType.PLAYTIME_MINUTES, 15, null).describe());
        assertEquals("tuỳ chỉnh", new Requirement(AchievementType.FISH, null, 5, "tuỳ chỉnh").describe());
    }
    @Test void countedProgressUnlocksOnceAndHonoursFilter() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        for (int i = 0; i < 10; i++) r.progress(d, AchievementType.KILL_MOB, "SPIDER", 1);
        assertEquals(0, d.progressOf(Requirement.reqKey("zombie", 0))); assertTrue(d.has("anymob")); assertFalse(d.has("zombie"));
        int n = 0; for (int i = 0; i < 5; i++) n += r.progress(d, AchievementType.KILL_MOB, "ZOMBIE", 1).size();
        assertEquals(1, n); assertTrue(d.has("zombie"));
    }
    @Test void stateRequirementsAllNeeded() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertTrue(r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:FIRE"))).isEmpty()); assertFalse(d.has("hoa")); // chỉ có Elementer
        var got = r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:FIRE", "NGUYENTO_UNLOCK:lua")));
        assertEquals(1, got.size()); assertEquals("hoa", got.get(0).id()); assertTrue(d.has("hoa"));
        assertTrue(r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:FIRE", "NGUYENTO_UNLOCK:lua"))).isEmpty()); // không thông báo lại
    }
    @Test void stateIsRememberedEvenIfLostLater() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:FIRE"))); // đạt 1 nửa, ghi nhớ
        assertEquals(1, r.checkStates(d, probe(Set.of("NGUYENTO_UNLOCK:lua"))).size()); // nửa còn lại đạt sau
    }
    @Test void mixedStateAndCountedRequirements() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        for (int i = 0; i < 3; i++) assertTrue(r.progress(d, AchievementType.KILL_PLAYER, "PLAYER", 1).stream().noneMatch(t -> t.id().equals("dark")));
        assertTrue(d.has("pvp")); assertFalse(d.has("dark")); // đủ kill nhưng chưa mở khóa nguyên tố
        assertEquals(1, r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:DARK", "NGUYENTO_UNLOCK:bong_toi"))).size()); assertTrue(d.has("dark"));
    }
    @Test void mixedOrderStatesFirstThenKills() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:DARK", "NGUYENTO_UNLOCK:bong_toi"))); assertFalse(d.has("dark"));
        r.progress(d, AchievementType.KILL_PLAYER, "PLAYER", 2); assertFalse(d.has("dark"));
        assertTrue(r.progress(d, AchievementType.KILL_PLAYER, "PLAYER", 1).stream().anyMatch(t -> t.id().equals("dark")));
    }
    @Test void bossKillPlusElementer() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        r.progress(d, AchievementType.BOSS_KILL, "fire_lord", 1); assertEquals(0, d.progressOf(Requirement.reqKey("dragon", 0))); // sai boss
        r.progress(d, AchievementType.BOSS_KILL, "DRAGON_LORD", 1); assertFalse(d.has("dragon")); // chưa có nguyên tố Rồng
        assertEquals(1, r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:DRAGON"))).size());
    }
    @Test void permissionAndManual() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertEquals(1, r.checkStates(d, probe(Set.of("PERMISSION:vip"))).size()); assertTrue(d.has("vip"));
        r.checkStates(d, probe(Set.of("ELEMENTER_UNLOCK:FIRE", "NGUYENTO_UNLOCK:lua"))); assertFalse(d.has("manual")); // thủ công không tự mở
        assertTrue(r.get("manual").manual());
    }
    @Test void selectOnlyUnlocked() {
        PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertFalse(d.select("vip")); d.unlock("vip"); assertTrue(d.select("VIP")); assertTrue(d.lock("vip")); assertNull(d.selected());
    }
    @Test void restoreAndCopy() {
        PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        d.restore(List.of("A"), Map.of("a~0", 5L), "C"); assertNull(d.selected()); assertFalse(d.dirty());
        PlayerTitleData c = d.copy(); d.addProgress("a~0", 10); assertEquals(5, c.progressOf("a~0"));
        d.addProgress("big", Long.MAX_VALUE); d.addProgress("big", 5); assertEquals(Long.MAX_VALUE, d.progressOf("big"));
    }
    @Test void pairCooldownStopsKillFarming() {
        PairCooldown pc = new PairCooldown(); UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(pc.tryAcquire(a, b, 0, 120_000)); assertFalse(pc.tryAcquire(a, b, 60_000, 120_000)); assertTrue(pc.tryAcquire(a, c, 60_000, 120_000)); assertTrue(pc.tryAcquire(a, b, 130_000, 120_000));
        assertFalse(pc.tryAcquire(a, a, 0, 0)); pc.forget(a); assertEquals(0, pc.size());
    }
    @Test void passiveDescriptions() {
        PassiveDef d = new PassiveDef(PassiveType.LOW_HP_BOOST, "Long Hồn", 20, 10, 100, 30, 5, 30, null);
        assertTrue(d.describe().contains("30%")); assertTrue(d.describe().contains("+20% sát thương")); assertTrue(d.describe().contains("+10% phòng thủ")); assertTrue(d.describe().startsWith("Long Hồn:"));
        assertTrue(new PassiveDef(PassiveType.DAMAGE_REDUCTION_PERCENT, null, 50, 0, 100, 0, 0, 0, "FIRE").describe().contains("Kháng lửa"));
        assertTrue(new PassiveDef(PassiveType.DODGE, null, 0, 0, 20, 0, 0, 0, null).describe().contains("20% né tránh"));
        assertTrue(new PassiveDef(PassiveType.DAMAGE_REDUCTION_FROM_ELEMENT, null, 15, 0, 100, 0, 0, 0, "DARK").describe().contains("Bóng Tối"));
        assertEquals("ᴛᴀɴ ᴛʜᴜ", SmallCaps.convert("Tân Thủ")); assertEquals("&6ᴍɪɴᴇʀ", SmallCaps.convert("&6Miner"));
    }
    @Test void killStreakKeepsBestAndNeverGoesDown() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertTrue(r.progressMax(d, AchievementType.KILL_STREAK, "PLAYER", 2).isEmpty()); assertTrue(r.progressMax(d, AchievementType.KILL_STREAK, "PLAYER", 1).isEmpty());
        assertEquals(2, d.progressOf(Requirement.reqKey("streak", 0))); // không tụt khi giá trị nhỏ hơn
        assertTrue(r.progressMax(d, AchievementType.KILL_STREAK, "ANY", 50).isEmpty()); // filter PLAYER không nhận chuỗi ANY
        assertEquals(1, r.progressMax(d, AchievementType.KILL_STREAK, "PLAYER", 3).size()); assertTrue(d.has("streak"));
    }
    @Test void rankPermissionNode() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertTrue(r.checkStates(d, (t, f, def) -> t == AchievementType.PERMISSION_NODE && "group.vip2".equals(f)).isEmpty());
        assertEquals(1, r.checkStates(d, (t, f, def) -> t == AchievementType.PERMISSION_NODE && "group.vip3".equals(f)).size()); assertTrue(d.has("rank"));
        assertTrue(Requirement.class != null && new Requirement(AchievementType.PERMISSION_NODE, "group.vip3", 1, null).describe().contains("group.vip3"));
    }
    @Test void travelAndAnyBossCount() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertTrue(r.progress(d, AchievementType.TRAVEL_BLOCKS, "BLOCK", 99).isEmpty()); assertEquals(1, r.progress(d, AchievementType.TRAVEL_BLOCKS, "BLOCK", 1).size());
        r.progress(d, AchievementType.BOSS_KILL, "ender_dragon", 1); r.progress(d, AchievementType.BOSS_KILL, "wither", 1); assertFalse(d.has("bosses"));
        assertEquals(1, r.progress(d, AchievementType.BOSS_KILL, "dragon_lord", 1).stream().filter(t -> t.id().equals("bosses")).count());
    }
    @Test void miningPlusKillsNeedBoth() {
        TitleRegistry r = reg(); PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        r.progress(d, AchievementType.BREAK_BLOCK, "STONE", 5); assertFalse(d.has("miner"));
        r.progress(d, AchievementType.KILL_PLAYER, "PLAYER", 2); assertTrue(d.has("miner"));
        assertTrue(Requirement.reqKey("X", 1).equals("x~1"));
    }
    @Test void anyOfPermissionNodes() {
        assertEquals(List.of("group.vip3", "group.vip4", "group.vipmax"), Requirement.nodes("group.vip3, group.vip4|group.vipmax"));
        assertTrue(Requirement.nodes(null).isEmpty()); assertTrue(Requirement.nodes(" , ").isEmpty());
        assertTrue(new Requirement(AchievementType.PERMISSION_NODE, "group.vip3,group.vip4", 1, null).describe().contains("group.vip3 hoặc group.vip4"));
    }
    @Test void donateTotalsAccumulateAcrossTiers() {
        TitleRegistry r = new TitleRegistry();
        r.register(def("rich", Rarity.MYTHIC, count(AchievementType.DONATE_TOTAL, 500_000, null)));
        r.register(def("tycoon", Rarity.MYTHIC, count(AchievementType.DONATE_TOTAL, 1_000_000, null)));
        PlayerTitleData d = new PlayerTitleData(UUID.randomUUID());
        assertTrue(r.progress(d, AchievementType.DONATE_TOTAL, "VND", 200_000).isEmpty());
        var got = r.progress(d, AchievementType.DONATE_TOTAL, "VND", 300_000); assertEquals(1, got.size()); assertEquals("rich", got.get(0).id());
        assertFalse(d.has("tycoon"));
        assertEquals("tycoon", r.progress(d, AchievementType.DONATE_TOTAL, "VND", 500_000).get(0).id()); // 1.000.000 đạt tier 2
        assertEquals("Nạp tổng cộng 500.000đ", count(AchievementType.DONATE_TOTAL, 500_000, null).describe()); assertEquals("1.000.000", Requirement.money(1_000_000));
    }
    @Test void rankFollowsDeclarationOrder() {
        TitleRegistry r = new TitleRegistry();
        r.register(def("weak", Rarity.COMMON)); r.register(def("mid", Rarity.RARE)); r.register(def("top", Rarity.MYTHIC));
        assertEquals(1, r.rankOf("weak")); assertEquals(3, r.rankOf("TOP")); assertEquals(-1, r.rankOf("none"));
        assertEquals(List.of("weak", "mid", "top"), r.all().stream().map(TitleDef::id).toList());
    }
    @Test void offlineNotifyFlagRoundTrip() {
        PlayerTitleData d = new PlayerTitleData(UUID.randomUUID()); d.setProgress("_notify~rich", 1); assertEquals(1, d.progressOf("_notify~rich"));
        d.removeProgress("_notify~rich"); assertEquals(0, d.progressOf("_notify~rich"));
    }
    @Test void mobDamagePassiveDescription() {
        assertTrue(new PassiveDef(PassiveType.DAMAGE_VS_MOB_PERCENT, "Săn Quái", 10, 0, 100, 0, 0, 0, null).describe().contains("+10% sát thương lên quái vật"));
        assertTrue(new PassiveDef(PassiveType.DAMAGE_VS_MOB_PERCENT, null, 20, 0, 100, 0, 0, 0, "WITHER_SKELETON").describe().contains("Wither Skeleton"));
    }
    static final String DEF = "# header\ntitles:\n  # ==== G1\n  a:\n    display: \"A\"\n    buffs: { x: 1 }\n\n  b:\n    display: \"B\"\n    # ghi chú trong khối\n    rarity: RARE\n\n  # ==== G2\n  c:\n    display: \"C\"\n\n  d:\n    display: \"D\"\n";
    static final String USER = "# my header\ntitles:\n  a:\n    display: \"MY A\"   # tui sửa\n    buffs: { x: 99 }\n\n  custom:\n    display: \"Của tui\"\n\n  c:\n    display: \"My C\"\n";
    static List<String> ids(String yaml) { List<String> o = new ArrayList<>(); for (String l : yaml.split("\n")) if (l.matches("^  [a-z_]+:\\s*$")) o.add(l.trim().replace(":", "")); return o; }

    @Test void updateConfigAddsOnlyMissingTitlesInRankPosition() {
        var r = TitleMerger.merge(USER, DEF, Set.of());
        assertEquals(List.of("b", "d"), r.added());
        assertEquals(List.of("a", "b", "custom", "c", "d"), ids(r.merged())); // b ngay sau a (theo bản mặc định), d sau c
        assertTrue(r.merged().contains("display: \"MY A\"   # tui sửa")); assertTrue(r.merged().contains("buffs: { x: 99 }")); // dữ liệu cũ giữ nguyên
        assertFalse(r.merged().contains("buffs: { x: 1 }")); assertTrue(r.merged().contains("# ghi chú trong khối")); assertTrue(r.merged().startsWith("# my header"));
    }
    @Test void updateConfigIsIdempotentAndRespectsIgnoreList() {
        String once = TitleMerger.merge(USER, DEF, Set.of()).merged();
        assertTrue(TitleMerger.merge(once, DEF, Set.of()).added().isEmpty()); assertEquals(once, TitleMerger.merge(once, DEF, Set.of()).merged());
        var r = TitleMerger.merge(USER, DEF, Set.of("b")); assertEquals(List.of("d"), r.added()); assertFalse(ids(r.merged()).contains("b"));
    }
    @Test void updateConfigInsertsNewFirstTitleAtTop() {
        String user = "titles:\n  b:\n    display: \"B\"\n";
        var r = TitleMerger.merge(user, DEF, Set.of());
        assertEquals("a", ids(r.merged()).get(0)); assertEquals(List.of("a", "b", "c", "d"), ids(r.merged())); assertEquals(List.of("a", "c", "d"), r.added());
    }
    @Test void updateConfigRejectsBrokenFiles() {
        assertThrows(IllegalArgumentException.class, () -> TitleMerger.merge("khong co muc nao\n", DEF, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> TitleMerger.merge(USER, "khong co\n", Set.of()));
    }
}
