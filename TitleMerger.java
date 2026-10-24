package vn.astrax.titles.config;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Gộp danh hiệu MỚI từ bản mặc định (trong JAR) vào titles.yml của server mà KHÔNG đụng tới phần cũ:
 * danh hiệu đã có giữ nguyên 100% (kể cả bạn đã sửa tên/buff), chú thích và danh hiệu tự thêm của bạn cũng giữ nguyên.
 * Danh hiệu mới được chèn đúng thứ hạng (ngay sau danh hiệu đứng trước nó trong bản mặc định).
 * Thuần Java (không phụ thuộc Bukkit) nên kiểm thử được.
 */
public final class TitleMerger {
    public record Result(String merged, List<String> added) {}
    private record Block(String id, int start, int contentEnd) {}
    private static final Pattern HEADER = Pattern.compile("^  ([A-Za-z0-9_\\-]+):\\s*(#.*)?$");
    private static final Pattern TITLES = Pattern.compile("^titles:\\s*(#.*)?$");
    private static final Pattern TRAILER = Pattern.compile("^ {0,2}#.*");

    private TitleMerger() {}

    private static List<String> lines(String s) { return new ArrayList<>(Arrays.asList(s.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1))); }

    private static int titlesLine(List<String> l) { for (int i = 0; i < l.size(); i++) if (TITLES.matcher(l.get(i)).matches()) return i; return -1; }

    private static List<Block> blocks(List<String> l, int tl) {
        List<Integer> heads = new ArrayList<>(); int stop = l.size();
        for (int i = tl + 1; i < l.size(); i++) {
            String s = l.get(i);
            if (!s.isBlank() && !s.startsWith(" ") && !s.startsWith("#")) { stop = i; break; } // sang mục cấp 0 khác
            if (HEADER.matcher(s).matches()) heads.add(i);
        }
        List<Block> out = new ArrayList<>();
        for (int k = 0; k < heads.size(); k++) {
            int start = heads.get(k), next = k + 1 < heads.size() ? heads.get(k + 1) : stop, end = next;
            while (end > start + 1 && (l.get(end - 1).isBlank() || TRAILER.matcher(l.get(end - 1)).matches())) end--; // bỏ dòng trống / chú thích nhóm phía sau
            var m = HEADER.matcher(l.get(start)); m.matches();
            out.add(new Block(m.group(1), start, end));
        }
        return out;
    }

    /** ignore: id (viết thường) không được thêm lại, dùng cho danh hiệu bạn cố ý xóa. */
    public static Result merge(String user, String def, Set<String> ignore) {
        List<String> out = lines(user), d = lines(def);
        int dtl = titlesLine(d);
        if (dtl < 0) throw new IllegalArgumentException("Bản mặc định thiếu mục 'titles:'");
        if (titlesLine(out) < 0) throw new IllegalArgumentException("titles.yml thiếu mục 'titles:'");
        List<Block> db = blocks(d, dtl);
        List<String> added = new ArrayList<>();
        for (int i = 0; i < db.size(); i++) {
            Block nb = db.get(i); String key = nb.id().toLowerCase(Locale.ROOT);
            if (ignore.contains(key)) continue;
            List<Block> cur = blocks(out, titlesLine(out));
            boolean has = false; for (Block b : cur) if (b.id().equalsIgnoreCase(nb.id())) has = true;
            if (has) continue;
            Block pred = null; // danh hiệu đứng trước gần nhất (theo bản mặc định) mà file hiện có
            for (int j = i - 1; j >= 0 && pred == null; j--) for (Block b : cur) if (b.id().equalsIgnoreCase(db.get(j).id())) pred = b;
            int at = pred == null ? titlesLine(out) + 1 : pred.contentEnd();
            List<String> ins = new ArrayList<>(); ins.add(""); ins.addAll(d.subList(nb.start(), nb.contentEnd())); if (pred == null) ins.add("");
            out.addAll(at, ins); added.add(nb.id());
        }
        return new Result(String.join("\n", out), added);
    }
}
