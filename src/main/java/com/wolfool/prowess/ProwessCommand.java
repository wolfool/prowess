package com.wolfool.prowess;

import com.wolfool.prowess.accessory.Accessories;
import com.wolfool.prowess.accessory.AccessoryGui;
import com.wolfool.prowess.combat.Formula;
import com.wolfool.prowess.gear.Enhance;
import com.wolfool.prowess.gear.EnhanceGui;
import com.wolfool.prowess.gear.WeaponGui;
import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.integration.EpithetHook;
import com.wolfool.prowess.integration.MasteryHook;
import com.wolfool.prowess.skill.Bind;
import com.wolfool.prowess.skill.SkillRegistry;
import com.wolfool.prowess.skill.SkillService;
import com.wolfool.prowess.skill.WeaponSkills;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatGui;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.title.TitleGui;
import com.wolfool.prowess.util.Economy;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProwessCommand implements CommandExecutor, TabCompleter {
   private final ProwessPlugin plugin;
   private final ProwessConfig config;
   private final StatService stats;
   private final Enhance enhance;
   private final Accessories accessories;
   private final SkillRegistry skills;
   private final SkillService skillService;
   private final Economy economy;
   private final StatGui statGui;
   private final AccessoryGui accessoryGui;
   private final MasteryHook mastery;
   private final EpithetHook epithet;
   private final WeaponGui weaponGui;
   private final TitleGui titleGui;

   public ProwessCommand(
      ProwessPlugin plugin,
      ProwessConfig config,
      StatService stats,
      Enhance enhance,
      Accessories accessories,
      SkillRegistry skills,
      SkillService skillService,
      Economy economy,
      StatGui statGui,
      AccessoryGui accessoryGui,
      MasteryHook mastery,
      EpithetHook epithet,
      WeaponGui weaponGui,
      TitleGui titleGui
   ) {
      this.plugin = plugin;
      this.config = config;
      this.stats = stats;
      this.enhance = enhance;
      this.accessories = accessories;
      this.skills = skills;
      this.skillService = skillService;
      this.economy = economy;
      this.statGui = statGui;
      this.accessoryGui = accessoryGui;
      this.mastery = mastery;
      this.epithet = epithet;
      this.weaponGui = weaponGui;
      this.titleGui = titleGui;
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
      String name = command.getName().toLowerCase(Locale.ROOT);
      if (name.equals("stats")) {
         if (sender instanceof Player p) {
            this.statGui.open(p);
            return true;
         } else {
            sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
            return true;
         }
      } else if (name.equals("enhance")) {
         if (sender instanceof Player p) {
            EnhanceGui var11 = new EnhanceGui(p, this.enhance, this.stats, this.economy, this.skills);
            p.openInventory(var11.getInventory());
            return true;
         } else {
            sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
            return true;
         }
      } else if (!name.equals("accessory")) {
         if (args.length == 0) {
            this.usage(sender, label);
            return true;
         }

         switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
            case "리로드":
               if (!sender.hasPermission("prowess.admin")) {
                  sender.sendMessage(Msg.of("&c권한이 없습니다."));
                  return true;
               }

               this.plugin.reloadAll();
               sender.sendMessage(Msg.of("&a[능력치] 설정을 다시 읽었습니다."));
               break;
            case "info":
            case "진단":
               this.info(sender);
               break;
            case "skills":
            case "스킬":
               this.listSkills(sender);
               break;
            case "give":
            case "새기기":
               this.imprint(sender, args);
               break;
            case "weapon":
            case "무기":
            case "장비":
               this.openAdminGui(sender, true);
               break;
            case "title":
            case "칭호":
               this.openAdminGui(sender, false);
               break;
            case "level":
            case "단계":
               this.setLevel(sender, args);
               break;
            case "vfx":
            case "이펙트":
               this.probe(sender);
               break;
            case "dummy":
            case "허수아비":
               this.clearDummies(sender, args);
               break;
            default:
               this.usage(sender, label);
         }

         return true;
      } else if (args.length <= 0 || !args[0].equals("만들기") && !args[0].equalsIgnoreCase("make")) {
         if (sender instanceof Player p) {
            this.accessoryGui.open(p);
            return true;
         } else {
            sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
            return true;
         }
      } else {
         return this.makeAccessory(sender, args);
      }
   }

   private void clearDummies(CommandSender sender, String[] args) {
      if (!(sender instanceof Player p)) {
         sender.sendMessage(Msg.of("&8게임 안에서 써 주십시오."));
      } else if (!p.hasPermission("prowess.admin")) {
         p.sendMessage(Msg.of("&c권한이 없습니다."));
      } else {
         double radius = 32.0;
         if (args.length >= 2) {
            try {
               radius = Math.min(256.0, Math.max(1.0, Double.parseDouble(args[args.length - 1])));
            } catch (NumberFormatException var10) {
            }
         }

         int mobs = 0;
         int boxes = 0;

         for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
            if (!(e instanceof Player)) {
               if (this.plugin.dummies() != null && this.plugin.dummies().isDummy(e)) {
                  e.remove();
                  mobs++;
               } else if (isOrphanHitbox(e)) {
                  e.remove();
                  boxes++;
               }
            }
         }

         p.sendMessage(Msg.of("&a[허수아비] &f" + mobs + "&7개를 치웠습니다. 남아 있던 판정 상자 &f" + boxes + "&7개도 함께 걷었습니다. &8(" + (int)radius + "칸)"));
      }
   }

   private static boolean isOrphanHitbox(Entity e) {
      if (!(e instanceof ArmorStand a)) {
         return false;
      } else {
         if (!a.isInvisible() || a.hasGravity() || a.isMarker()) {
            return false;
         }

         if (a.customName() != null) {
            return false;
         }

         EntityEquipment eq = a.getEquipment();
         if (eq != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
               try {
                  ItemStack it = eq.getItem(slot);
                  if (it != null && !it.getType().isAir()) {
                     return false;
                  }
               } catch (IllegalArgumentException var8) {
               }
            }
         }

         return true;
      }
   }

   private void probe(CommandSender sender) {
      if (sender instanceof Player p) {
         if (!p.hasPermission("prowess.admin")) {
            p.sendMessage(Msg.of("&c권한이 없습니다."));
         } else {
            boolean on = this.plugin.vfxProbe().toggle(p);
            p.sendMessage(Msg.of(on ? "&a이펙트 살펴보기 켬 &7— 스킬을 써 보세요. 2분 뒤 저절로 꺼집니다" : "&7이펙트 살펴보기 끔"));
         }
      } else {
         sender.sendMessage(Msg.of("&8게임 안에서 써 주십시오."));
      }
   }

   private void usage(CommandSender sender, String label) {
      sender.sendMessage(Msg.of("&6Prowess &7— 능력치 · 강화 · 장신구 · 무기 스킬"));
      sender.sendMessage(Msg.of("&e/스탯 &7- 지금 내 능력치"));
      sender.sendMessage(Msg.of("&e/강화 &7- 장비를 올립니다"));
      sender.sendMessage(Msg.of("&e/장신구 &7- 반지 · 목걸이 · 귀걸이"));
      sender.sendMessage(Msg.of("&8웅크리고 우클릭 &7- 손에 든 무기의 스킬"));
      if (sender.hasPermission("prowess.admin")) {
         sender.sendMessage(Msg.of("&e/" + label + " 진단 &7- 연결 상태와 공식"));
         sender.sendMessage(Msg.of("&e/" + label + " 스킬 &7- 등록된 무기 스킬"));
         sender.sendMessage(Msg.of("&e/" + label + " 무기 &7- 장비를 골라 받습니다 (스킬 설명 포함)"));
         sender.sendMessage(Msg.of("&e/" + label + " 칭호 &7- 칭호에 능력치를 붙입니다"));
         sender.sendMessage(Msg.of("&e/" + label + " 단계 <숫자> &7- 손에 든 것의 강화 단계"));
         sender.sendMessage(Msg.of("&e/" + label + " 새기기 <능력치> <값> &7- 손에 든 것에 새깁니다"));
         sender.sendMessage(Msg.of("&e/장신구 만들기 <종류> &7- 손에 든 것을 장신구로"));
         sender.sendMessage(Msg.of("&e/" + label + " 허수아비 [거리] &7- 둘레의 허수아비와 남은 판정을 치웁니다"));
         sender.sendMessage(Msg.of("&e/" + label + " 리로드"));
      }
   }

   private void info(CommandSender sender) {
      sender.sendMessage(Msg.of("&6── Prowess 진단 ──"));
      sender.sendMessage(Msg.of("&7Mastery &f" + (this.mastery.connected() ? "&a연결됨" : "&8없음")));
      sender.sendMessage(Msg.of("&7CraftEngine &f" + (CraftEngineHook.available() ? "&a연결됨" : "&8없음")));
      sender.sendMessage(Msg.of("&7Epithet &f" + (this.epithet.connected() ? "&a연결됨" : "&8없음")));
      sender.sendMessage(Msg.of("&7경제 &f" + (this.economy.connected() ? "&a연결됨" : "&8없음")));
      sender.sendMessage(Msg.of("&7무기 스킬 &f" + this.skills.size() + "종"));
      sender.sendMessage(
         Msg.of(
            "&7강화 &f최대 +"
               + this.config.maxLevel()
               + " &8· 안전 +"
               + this.config.safeLevel()
               + " &8· 단계당 +"
               + Math.round(this.config.enhancePerLevel() * 100.0)
               + "%"
         )
      );
      sender.sendMessage(Msg.of("&7피해 공식 &8감소 = 방어 / (방어 + " + Msg.trim(this.config.defenseK()) + ")"));

      for (int d : new int[]{50, 100, 300, 900}) {
         sender.sendMessage(
            Msg.of(
               "&8  방어 "
                  + d
                  + " → -"
                  + Msg.percent(Formula.reduction(d, 0.0, this.config.defenseK()))
                  + " · 맷집 ×"
                  + String.format("%.1f", Formula.effectiveHealthFactor(d, this.config.defenseK()))
            )
         );
      }
   }

   private void listSkills(CommandSender sender) {
      sender.sendMessage(Msg.of("&6무기 스킬 &f" + this.skills.size() + "&6종 · 무기 &f" + this.skills.weapons().size() + "&6개"));

      for (Entry<String, WeaponSkills> e : this.skills.weapons().entrySet()) {
         sender.sendMessage(Msg.of("&f" + e.getKey()));

         for (Bind bind : Bind.values()) {
            WeaponSkills.Slot slot = e.getValue().at(bind);
            if (slot != null) {
               sender.sendMessage(
                  Msg.of(
                     "  "
                        + bind.color()
                        + bind.korean()
                        + " &f"
                        + slot.skill().name()
                        + " &8["
                        + slot.skill().type().korean()
                        + "] +"
                        + slot.unlock()
                        + " 부터 · 쿨 "
                        + Msg.trim(slot.skill().cooldown())
                        + "초"
                  )
               );
            }
         }
      }
   }

   private void openAdminGui(CommandSender sender, boolean weapon) {
      if (!sender.hasPermission("prowess.admin")) {
         sender.sendMessage(Msg.of("&c권한이 없습니다."));
      } else if (sender instanceof Player player) {
         if (weapon) {
            this.weaponGui.open(player);
         } else {
            this.titleGui.open(player);
         }
      } else {
         sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
      }
   }

   private void setLevel(CommandSender sender, String[] args) {
      if (!sender.hasPermission("prowess.admin")) {
         sender.sendMessage(Msg.of("&c권한이 없습니다."));
      } else if (sender instanceof Player player) {
         ItemStack held = player.getInventory().getItemInMainHand();
         if (held.getType().isAir()) {
            sender.sendMessage(Msg.of("&c손에 아무것도 없습니다."));
         } else if (args.length < 2) {
            sender.sendMessage(Msg.of("&c쓰는 법: &f/prowess 단계 <0~" + this.config.maxLevel() + ">"));
            sender.sendMessage(Msg.of("&7지금 &f+" + this.plugin.gearStats().level(held)));
         } else {
            int level;
            try {
               level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
               sender.sendMessage(Msg.of("&c숫자로 적어 주세요."));
               return;
            }

            level = Math.max(0, Math.min(this.config.maxLevel(), level));
            this.enhance.imprintIfNeeded(held);
            if (!this.plugin.gearStats().isGear(held)) {
               sender.sendMessage(Msg.of("&c강화할 수 있는 장비가 아닙니다. &7gear.yml 에 적힌 것만 됩니다."));
            } else {
               this.plugin.gearStats().setLevel(held, level, this.config.enhancePerLevel());
               sender.sendMessage(Msg.of("&a[강화] 단계를 &f+" + level + " &a로 맞췄습니다."));
               this.stats.refreshLater(player);
            }
         }
      } else {
         sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
      }
   }

   private void imprint(CommandSender sender, String[] args) {
      if (!sender.hasPermission("prowess.admin")) {
         sender.sendMessage(Msg.of("&c권한이 없습니다."));
      } else if (sender instanceof Player player) {
         ItemStack held = player.getInventory().getItemInMainHand();
         if (held.getType().isAir()) {
            sender.sendMessage(Msg.of("&c손에 아무것도 없습니다."));
         } else if (args.length < 3) {
            sender.sendMessage(Msg.of("&c쓰는 법: &f/prowess 새기기 <능력치> <값>"));
            sender.sendMessage(Msg.of("&7능력치: &f" + names()));
         } else {
            String rawName = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
            Stat stat = Stat.byName(rawName);
            if (stat == null) {
               sender.sendMessage(Msg.of("&c'" + rawName + "' 는 없는 능력치입니다."));
               sender.sendMessage(Msg.of("&7" + names()));
            } else {
               double value;
               try {
                  value = Double.parseDouble(args[args.length - 1]);
               } catch (NumberFormatException e) {
                  sender.sendMessage(Msg.of("&c마지막에 숫자를 적어 주세요. &8예: /기량 새기기 공격 속도 10"));
                  return;
               }

               StatMap now = this.plugin.gearStats().raw(held);
               now.add(stat, value - now.get(stat));
               this.plugin.gearStats().write(held, now, this.plugin.gearStats().level(held), this.config.enhancePerLevel());
               sender.sendMessage(Msg.of("&a[능력치] " + stat.korean() + " &f" + stat.format(value) + " &a을(를) 새겼습니다."));
               this.stats.refreshLater(player);
            }
         }
      } else {
         sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
      }
   }

   private boolean makeAccessory(CommandSender sender, String[] args) {
      if (!sender.hasPermission("prowess.admin")) {
         sender.sendMessage(Msg.of("&c권한이 없습니다."));
         return true;
      }

      if (sender instanceof Player player) {
         ItemStack held = player.getInventory().getItemInMainHand();
         if (held.getType().isAir()) {
            sender.sendMessage(Msg.of("&c손에 아무것도 없습니다."));
            return true;
         } else if (args.length < 2) {
            sender.sendMessage(Msg.of("&c쓰는 법: &f/장신구 만들기 <ring|necklace|earring>"));
            return true;
         } else {
            String kind = args[1].toLowerCase(Locale.ROOT);
            this.accessories.mark(held, kind);
            sender.sendMessage(Msg.of("&a[장신구] 이 물건을 &f" + kind + " &a으로 만들었습니다."));
            sender.sendMessage(Msg.of("&8능력치는 /prowess 새기기 로 넣습니다."));
            return true;
         }
      } else {
         sender.sendMessage(Msg.of("&c플레이어만 쓸 수 있습니다."));
         return true;
      }
   }

   private static String names() {
      List<String> out = new ArrayList<>();

      for (Stat s : Stat.values()) {
         out.add(s.korean());
      }

      return String.join(" · ", out);
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
      List<String> out = new ArrayList<>();
      String name = command.getName().toLowerCase(Locale.ROOT);
      if (name.equals("accessory") && args.length == 1) {
         for (String s : List.of("만들기")) {
            if (s.startsWith(args[0])) {
               out.add(s);
            }
         }

         return out;
      } else if (name.equals("accessory") && args.length == 2) {
         for (String s : List.of("ring", "necklace", "earring")) {
            if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) {
               out.add(s);
            }
         }

         return out;
      } else {
         if (!name.equals("prowess")) {
            return out;
         }

         if (args.length == 1) {
            for (String s : List.of("진단", "스킬", "무기", "칭호", "단계", "새기기", "허수아비", "리로드")) {
               if (s.startsWith(args[0])) {
                  out.add(s);
               }
            }

            return out;
         } else {
            if (args.length == 2 && (args[0].equals("새기기") || args[0].equalsIgnoreCase("give"))) {
               for (Stat s : Stat.values()) {
                  if (s.korean().startsWith(args[1])) {
                     out.add(s.korean());
                  }
               }
            }

            return out;
         }
      }
   }
}
