package vn.astrax.titles.model;

import java.util.List;
import java.util.Map;

/**
 * Một danh hiệu. requirements: TẤT CẢ nhiệm vụ phải hoàn thành mới mở khóa (rỗng = admin cấp tay).
 * buffs: attribute -> giá trị. passives: nội tại chiến đấu.
 */
public record TitleDef(String id, String display, String description, Rarity rarity, String icon, List<Requirement> requirements,
                       Map<String, BuffValue> buffs, List<PassiveDef> passives, boolean announce, boolean hidden) {
    public TitleDef {
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        buffs = buffs == null ? Map.of() : Map.copyOf(buffs);
        passives = passives == null ? List.of() : List.copyOf(passives);
    }
    public boolean manual() { return requirements.isEmpty(); }
}
