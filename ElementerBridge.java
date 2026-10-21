package vn.astrax.titles.hook;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Cầu nối ĐỌC-CHỈ tới plugin AstraxElementer (plugin riêng, khác NguyenTo) bằng reflection:
 *   ElementerCore.elementService -> get(UUID) -> PlayerData.hasUnlocked(Element) / getElement().
 * Không sửa plugin kia. Nếu cấu trúc đổi, cầu nối tự tắt và ghi cảnh báo (nhiệm vụ ELEMENTER_UNLOCK sẽ không đạt).
 */
public final class ElementerBridge {
    private static final NamespacedKey BOSS_KEY = NamespacedKey.fromString("astraxelementer:boss_id");
    private final Logger log;
    private Plugin cached; private Object service; private Method get, hasUnlocked, getElement; private Class<?> elementEnum;
    private boolean broken, warned;

    public ElementerBridge(Logger log) { this.log = log; }

    public boolean available() { return ensure(); }

    private boolean ensure() {
        if (broken) return false;
        Plugin p = Bukkit.getPluginManager().getPlugin("AstraxElementer");
        if (p == null || !p.isEnabled()) { cached = null; service = null; return false; }
        if (p == cached && service != null) return true;
        try {
            ClassLoader cl = p.getClass().getClassLoader();
            Field f = p.getClass().getDeclaredField("elementService"); f.setAccessible(true);
            service = f.get(p); if (service == null) return false;
            elementEnum = Class.forName("com.astrax.elementer.Element", true, cl);
            Class<?> pd = Class.forName("com.astrax.elementer.PlayerData", true, cl);
            get = service.getClass().getMethod("get", UUID.class);
            hasUnlocked = pd.getMethod("hasUnlocked", elementEnum); getElement = pd.getMethod("getElement");
            cached = p; return true;
        } catch (Throwable t) {
            broken = true; log.warning("Không đọc được AstraxElementer (" + t + "): nhiệm vụ ELEMENTER_UNLOCK sẽ không hoạt động. Xem README mục 'Tích hợp'.");
            return false;
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean hasUnlocked(UUID id, String element) {
        if (!ensure() || element == null) return false;
        try {
            Object pdata = get.invoke(service, id); if (pdata == null) return false; // dữ liệu chưa tải xong
            return (boolean) hasUnlocked.invoke(pdata, Enum.valueOf((Class<Enum>) elementEnum, element.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException e) { if (!warned) { warned = true; log.warning("titles.yml: nguyên tố Elementer không tồn tại: " + element); } return false; }
        catch (Throwable t) { return false; }
    }
    /** Tên nguyên tố ĐANG DÙNG của người chơi (FIRE, DARK...) hoặc null. */
    public String activeElement(UUID id) {
        if (!ensure()) return null;
        try { Object pdata = get.invoke(service, id); if (pdata == null) return null; Object e = getElement.invoke(pdata); return e == null ? null : ((Enum<?>) e).name(); }
        catch (Throwable t) { return null; }
    }
    /** Id boss Elementer (dragon_lord...) nếu entity là boss của AstraxElementer. */
    public String bossId(Entity e) {
        return BOSS_KEY == null ? null : e.getPersistentDataContainer().get(BOSS_KEY, PersistentDataType.STRING);
    }
}
