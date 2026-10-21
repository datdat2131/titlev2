package vn.astrax.titles.model;

/** Giá trị buff: "4" = cộng thẳng 4; "5%" = cộng 5% (ADD_SCALAR 0.05). */
public record BuffValue(double amount, boolean percent) {
    public static BuffValue parse(Object raw) {
        if (raw == null) throw new IllegalArgumentException("giá trị buff rỗng");
        String s = String.valueOf(raw).trim();
        boolean pct = s.endsWith("%");
        if (pct) s = s.substring(0, s.length() - 1).trim();
        double v = Double.parseDouble(s);
        return new BuffValue(pct ? v / 100.0 : v, pct);
    }
    public String describe() {
        String sign = amount >= 0 ? "+" : "";
        return percent ? sign + trim(amount * 100) + "%" : sign + trim(amount);
    }
    private static String trim(double d) { return d == Math.rint(d) ? String.valueOf((long) d) : String.format(java.util.Locale.US, "%.2f", d); }
}
