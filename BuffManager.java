package vn.astrax.titles.buff;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import vn.astrax.titles.AstraxTitlesPlugin;
import vn.astrax.titles.model.*;

import java.util.*;

/** Áp buff của danh hiệu đang đeo bằng attribute modifier tạm thời (transient: không lưu vào file người chơi). */
public final class BuffManager {
    private final AstraxTitlesPlugin plugin;
    public BuffManager(AstraxTitlesPlugin plugin) { this.plugin = plugin; }

    private NamespacedKey keyFor(String attr) { return new NamespacedKey(plugin, "title_" + attr.replace('.', '_')); }
    private Attribute resolve(String name) {
        try { return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name)); } catch (Exception e) { return null; }
    }
    public boolean validAttribute(String name) { return resolve(name) != null; }

    /** Gỡ mọi buff của plugin rồi áp lại buff của danh hiệu đang đeo. */
    public void apply(Player p) {
        remove(p);
        PlayerTitleData d = plugin.data(p.getUniqueId());
        TitleDef def = d == null ? null : plugin.titles().get(d.selected());
        if (def == null || !plugin.getConfig().getBoolean("buffs.enabled", true)) return;
        for (var e : def.buffs().entrySet()) {
            Attribute a = resolve(e.getKey());
            AttributeInstance inst = a == null ? null : p.getAttribute(a);
            if (inst == null) continue;
            BuffValue v = e.getValue();
            inst.addTransientModifier(new AttributeModifier(keyFor(e.getKey()), v.amount(),
                    v.percent() ? AttributeModifier.Operation.ADD_SCALAR : AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
    }

    public void remove(Player p) {
        Set<String> names = new HashSet<>();
        for (TitleDef t : plugin.titles().all()) names.addAll(t.buffs().keySet());
        for (String n : names) {
            Attribute a = resolve(n); AttributeInstance inst = a == null ? null : p.getAttribute(a); if (inst == null) continue;
            NamespacedKey k = keyFor(n);
            for (AttributeModifier m : new ArrayList<>(inst.getModifiers())) if (m.getKey().equals(k)) inst.removeModifier(m);
        }
        var hp = p.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null && p.getHealth() > hp.getValue()) p.setHealth(hp.getValue());
    }
}
