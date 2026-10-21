# AstraxTitles 1.3.1 - Danh hiệu nhiệm vụ cho BoxMine RPG

Danh hiệu mở khóa bằng nhiệm vụ, mỗi danh hiệu có **buff** và **nội tại** riêng, hiển thị **trước tên trong Tab** (chữ in hoa nhỏ). Paper 1.21.4, Java 21.

> **Trạng thái:** logic (nhiệm vụ, tiến độ, mô tả nội tại) đã chạy 19 test đạt; phần Bukkit (nội tại chiến đấu, GUI, hook plugin khác) **chưa build bằng Maven và chưa chạy trên server thật**. Hãy thử trên server test.

## Tích hợp với 2 plugin của bạn (hai plugin KHÁC NHAU)
| Plugin | Cách đọc | Nhiệm vụ trong titles.yml |
|---|---|---|
| **AstraxElementer** (`/elementer`) | reflection, chỉ đọc: `ElementerCore.elementService.get(uuid).hasUnlocked(...)`; boss nhận diện qua khóa PDC `astraxelementer:boss_id` | `ELEMENTER_UNLOCK` (filter: FIRE ICE LIGHTNING EARTH WIND LIGHT DARK DRAGON), `BOSS_KILL` (filter: id boss như `dragon_lord`) |
| **NguyenTo** (`/nguyento`) | đọc PDC người chơi `nguyento:owned` | `NGUYENTO_UNLOCK` (filter: lua nuoc dat gio set bang bong_toi anh_sang) |

Không sửa code của hai plugin kia. Nếu bạn đổi tên field `elementService` trong `ElementerCore`, cầu nối tự tắt và ghi cảnh báo; cách bền hơn là thêm vào `ElementerCore` một getter `public ElementService elementService()` (bản hiện tại chưa cần).
Kiểm tra kết nối: `/titleadmin check <tên>`.

## Cách mở khóa và thông báo
Mỗi danh hiệu có danh sách `requirements`; phải hoàn thành **tất cả**. Khi đủ, người chơi nhận: tin nhắn, title lớn giữa màn hình, âm thanh, và thông báo toàn server nếu danh hiệu Huyền Thoại trở lên (hoặc `announce: true`). Danh hiệu đầu tiên mở được tự động đeo.
Nhiệm vụ "mở khóa nguyên tố" được kiểm tra mỗi `poll-seconds` (mặc định 5 giây) và ghi nhớ; nên mở khóa xong sẽ có thông báo trong vài giây. Nhiệm vụ đếm (kill, online...) cập nhật tức thì.

## 18 danh hiệu mẫu
| # | Danh hiệu | Nhiệm vụ | Buff | Nội tại |
|---|---|---|---|---|
| 1 | Tân Thủ | Online 15 phút | đào +1, đánh +0.15, +1 máu | - |
| 2 | Chiến Binh | Hạ 10 người | +1 dame, +2 máu, đánh +0.20 | - |
| 3 | Cuồng Chiến | Hạ 40 người | +1.25 dame, +3 máu, đánh +0.30 | - |
| 4 | Đồ Sát | Hạ 100 người | +2 dame, +5 máu, đánh +0.40 | - |
| 5 | Hỏa Viễn | Elementer Hỏa + NguyenTo Lửa | +10% tốc độ, +1.5 dame, +3 máu | đòn đánh gây cháy; +20% dame lên mục tiêu đang cháy |
| 6 | Thủy Hộ | Elementer Băng + NguyenTo Nước | +20% tốc độ, +5 máu | kháng lửa 50%; +10% hồi máu |
| 7 | Địa Thổ | NguyenTo Đất + Elementer Thổ | +5 giáp, +6 máu | giảm 10% sát thương nhận |
| 8 | Lôi Đế | NguyenTo Sét + Elementer Lôi | +1.5 dame, đánh +0.4 | 10% giáng sét |
| 9 | Phong Nhã | NguyenTo Gió + Elementer Phong | +20% tốc độ, đánh +0.5 | 20% né tránh |
| 10 | Băng Quang | NguyenTo Băng + Elementer Quang | +6 máu | +20% hồi máu; 20% gây lạnh |
| 11 | Long Thần | Hạ boss Long Vương Hủy Diệt + Elementer Rồng | +2.5 dame, +5 máu, đánh +0.5 | Long Uy / Long Tức / Long Hồn |
| 12 | Thánh Đế | NguyenTo Ánh Sáng | +2.5 dame, +20% tốc độ, +5 máu | Quang Kích / Thánh Quang / Quang Hồi |
| 13 | Bóng Tối | Elementer Bóng Tối + NguyenTo Bóng Tối + 100 kill | +3 dame, +8 máu, đánh +0.6, +10% tốc độ | Hắc Thực / Ám Ảnh / Tử Vực / Hắc Giới (+10% hút máu) |
| 14 | Khai Khoáng | Đào 4000 block + hạ 10 người chơi | đào +2, +20% tốc độ, đánh +0.15 | - |
| 15 | Nhà Thám Hiểm | Di chuyển 2000 block | +30% tốc độ, +5 máu, +2 giáp | - |
| 16 | VIP | Rank VIP3 trở lên (`group.vip3/vip4/vip5/vipmax`) | +2 dame, +6 máu, +3 giáp, đánh +0.5 | - |
| 17 | Thợ Săn Boss | Hạ 10 boss bất kỳ | +2 dame, đánh +0.25 | +50% sát thương lên Boss |
| 18 | Chiến Binh Bất Khuất | Chuỗi hạ gục 50 người chơi (không chết) | +2 dame, +5 máu | giảm 20% sát thương nhận |

## Các điểm mình đã diễn giải (sửa trong titles.yml nếu khác ý bạn)
- **Khai Khoáng "10 kill":** hiểu là hạ 10 **người chơi**. Muốn đổi sang quái: `type: KILL_MOB`.
- **VIP:** chỉ có một danh hiệu `VIP` (không có Ruby). Mở khóa khi người chơi thuộc MỘT trong các nhóm LuckPerms `vip3`, `vip4`, `vip5`, `vipmax` (LuckPerms tự cấp quyền `group.<tên nhóm>`). Server có nhóm tên khác thì sửa `filter` của `vip` trong `titles.yml`.
- **Thợ Săn Boss:** tính boss AstraxElementer và boss vanilla có giao diện Boss (Ender Dragon, Wither). Warden/Elder Guardian và boss của plugin khác chưa tính.
- **Chuỗi hạ gục:** mặc định chỉ tính **người chơi** (`filter: PLAYER`), reset khi chết, lưu qua restart. Muốn tính cả quái: `filter: ANY`.
- **Di chuyển:** đo quãng đường ngang mỗi 2 giây; không tính teleport, Creative/Spectator, và bước nhảy > 150 block. Đi thuyền/ngựa/cánh cánh vẫn tính; máy chạy tự động (nước, ray) có thể farm được.
- **"Tốc độ đào +1" (Tân Thủ):** áp đúng số bạn ghi = `block_break_speed +1` (đào nhanh gấp đôi). Nếu muốn nhẹ hơn, đổi thành `"20%"`.
- **HP:** `+1 máu` = 1 điểm máu (nửa tim), cùng thang với NguyenTo (2 máu = 1 tim).
- **Hỏa Viễn:** "đòn đánh gây cháy +20% dame" = đòn đánh luôn thiêu đốt 3s và **+20% sát thương lên mục tiêu đang cháy**.
- **Thủy Hộ:** "kháng lửa" = giảm 50% sát thương từ lửa/dung nham (chỉ khai báo một lần dù bạn ghi ở cả buff lẫn nội tại).
- **Bóng Tối:** "2 elementer + Nguyento" mình hiểu là **Elementer Bóng Tối + NguyenTo Bóng Tối + 100 kill**. Muốn khác, sửa mục `requirements` của `bong_toi`.
- **"Sát thương từ Bóng Tối / Quang Vương":** nhận diện theo nguyên tố Elementer đang dùng của kẻ tấn công hoặc boss Elementer (`dark_lord`, `light_paladin`... trong `boss-elements`). Chiêu của NguyenTo chưa nhận diện được.
- **Hạ boss:** tính cho người đánh đòn cuối **và** mọi người gây >= 5% máu boss (`boss-credit-percent`).
- **Chống farm:** hạ cùng một người chơi chỉ tính lại sau 120 giây (`anti-farm.same-victim-seconds`).

## Nội tại: tham số
`type` + `name` (tên hiển thị) + `value`, `value2` (%), `chance` (%), `threshold` (% máu), `duration`/`cooldown` (giây), `filter`.
Loại: `OUTGOING_DAMAGE_PERCENT`, `DAMAGE_VS_BURNING_PERCENT`, `BOSS_DAMAGE_PERCENT`, `IGNITE_ON_HIT`, `SLOW_ON_HIT`, `WEAKEN_ON_HIT`, `LIFESTEAL_PERCENT`, `LIFESTEAL_ON_HIT`, `LIGHTNING_ON_HIT`, `BONUS_DAMAGE_ON_HIT` (filter FIRE/LIGHT/DARK chọn hiệu ứng hạt, `duration` > 0 thì thiêu đốt), `DODGE`, `DAMAGE_REDUCTION_PERCENT` (filter FIRE hoặc danh sách DamageCause), `DAMAGE_REDUCTION_FROM_ELEMENT`, `HEAL_RECEIVED_PERCENT`, `LOW_HP_BOOST`, `LOW_HP_HEAL`.
Nội tại chỉ áp cho **đòn cận chiến** (và nhận sát thương); tổng giảm sát thương bị chặn ở `max-damage-reduction` (80%).

## Tương tác với NguyenTo
NguyenTo đặt giá trị gốc (base) của máu/sát thương/tốc độ; AstraxTitles cộng bằng modifier tạm thời nên **cộng dồn, không ghi đè nhau**. Riêng "giáp %" của NguyenTo tính theo giáp mặc, nên giáp +5 của Địa Thổ có thể bị nhân nhẹ. Thoát game: buff của titles được gỡ, vào lại tự áp.

## Lệnh và quyền
`/title` (menu), `/title equip <id>`, `/title off`, `/title list` - `astraxtitles.use`.
`/titleadmin reload | list | give <p> <id> | take <p> <id> | progress <p> <id> <nhiệm vụ#> <số> | check <p>` - `astraxtitles.admin`.

## Chỉnh sửa danh hiệu (tên, buff...)
**Cách 1 - sửa file:** mở `plugins/AstraxTitles/titles.yml`, đổi `display` (tên), `description`, `rarity`, `icon`, `buffs`, `requirements`, `passives`, rồi `/titleadmin reload`.
**Cách 2 - ngay trong game (cần `astraxtitles.admin`):**
- `/titleadmin set <id> display &6✦ Tên Mới` (hoặc `description`, `rarity`, `icon`)
- `/titleadmin buff <id> attack_damage 2.5` (số thường) hoặc `/titleadmin buff <id> movement_speed 10%` (phần trăm)
- `/titleadmin buff <id> attack_damage remove` để xóa buff
Mỗi lần sửa plugin tự sao lưu `titles.yml.bak` rồi tải lại. Lưu ý: lệnh ghi lại cả file nên có thể làm mất một số dòng chú thích; muốn giữ chú thích thì sửa file bằng tay.

## Hiển thị trong Tab: `[Rank] Steve | ✦ ᴛᴀɴ ᴛʜᴜ`
### Cách 1 - server có plugin TAB + LuckPerms (khuyên dùng)
1. Cài PlaceholderAPI (TAB đọc placeholder của AstraxTitles qua PlaceholderAPI). Trong `config.yml` của AstraxTitles giữ `tab.enabled: false`.
2. Mở `plugins/TAB/groups.yml`, đặt (hoặc chạy lệnh `/tab group _DEFAULT_ tabsuffix %astraxtitles_suffix%`):
```yaml
_DEFAULT_:
  tabprefix: "%luckperms-prefix%"
  tabsuffix: "%astraxtitles_suffix%"
```
3. `/tab reload`. Nếu bạn đang dùng prefix khác (vd `%vault_prefix%`) thì giữ nguyên dòng `tabprefix` của bạn, chỉ thêm `tabsuffix`.
- Người chơi không đeo danh hiệu thì suffix rỗng (không hiện dấu `|`).
- Muốn danh hiệu thẳng cột bên phải thì bật *Aligned tabsuffix* trong `config.yml` của TAB.
- Muốn đổi dấu phân cách/màu: sửa `tab.suffix-format` (mặc định `" &8| %title%"`).
### Cách 2 - không có plugin quản lý Tab
Đặt `tab.enabled: true`: plugin tự thêm danh hiệu vào tên trong Tab, mặc định **sau** tên (`tab.position: AFTER`), giữ nguyên rank của plugin khác. Đừng bật cùng lúc với plugin TAB (hai bên sẽ tranh nhau).
### Placeholder
`%astraxtitles_suffix%` (` | ✦ ᴛᴀɴ ᴛʜᴜ`), `%astraxtitles_prefix%` (`✦ ᴛᴀɴ ᴛʜᴜ | `), `%astraxtitles_title%` (chỉ danh hiệu), `%astraxtitles_id%`, `%astraxtitles_count%`.
Chat mặc định tắt (`chat.enabled`).
