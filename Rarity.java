package vn.astrax.titles.model;

public enum Rarity {
    COMMON("Thường", "§f"), UNCOMMON("Khá", "§a"), RARE("Hiếm", "§b"), EPIC("Sử Thi", "§d"), LEGENDARY("Huyền Thoại", "§6"), MYTHIC("Thần Thoại", "§c");
    private final String display, color;
    Rarity(String d, String c) { display = d; color = c; }
    public String display() { return display; }
    public String color() { return color; }
    public boolean atLeast(Rarity o) { return ordinal() >= o.ordinal(); }
    public static Rarity parse(String s) {
        if (s == null) return COMMON;
        try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return COMMON; }
    }
}
