package vn.astrax.titles.model;

import java.text.Normalizer;

/** Đổi chữ thường sang kiểu in hoa nhỏ (ᴀʙᴄ). Bỏ dấu tiếng Việt vì font small-caps không có chữ có dấu. Giữ nguyên mã màu &x. */
public final class SmallCaps {
    private SmallCaps() {}
    private static final String SRC = "abcdefghijklmnopqrstuvwxyz";
    private static final String DST = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    public static String convert(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) { out.append(c).append(s.charAt(++i)); continue; } // mã màu
            out.append(letter(c));
        }
        return out.toString();
    }
    private static String letter(char c) {
        if (c == 'đ' || c == 'Đ') return "ᴅ";
        String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
        char b = Character.toLowerCase(base.charAt(0));
        int idx = SRC.indexOf(b);
        return idx >= 0 ? String.valueOf(DST.charAt(idx)) : String.valueOf(c);
    }
}
