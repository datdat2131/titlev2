package vn.astrax.titles.model;

import java.util.Locale;

/** Nội tại có thể gắn vào danh hiệu (xem README để biết tham số từng loại). */
public enum PassiveType {
    OUTGOING_DAMAGE_PERCENT, DAMAGE_VS_BURNING_PERCENT, BOSS_DAMAGE_PERCENT,
    IGNITE_ON_HIT, SLOW_ON_HIT, WEAKEN_ON_HIT, LIFESTEAL_PERCENT, LIFESTEAL_ON_HIT, LIGHTNING_ON_HIT, BONUS_DAMAGE_ON_HIT,
    DODGE, DAMAGE_REDUCTION_PERCENT, DAMAGE_REDUCTION_FROM_ELEMENT, HEAL_RECEIVED_PERCENT, LOW_HP_BOOST, LOW_HP_HEAL;

    public static PassiveType parse(String s) {
        if (s == null) return null;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }
}
