package com.wolfool.prowess.integration;

import com.wolfool.prowess.ProwessPlugin;
import com.wolfool.prowess.combat.Formula;
import com.wolfool.prowess.gear.GearStats;
import com.wolfool.prowess.skill.Bind;
import com.wolfool.prowess.skill.SkillRegistry;
import com.wolfool.prowess.skill.SkillService;
import com.wolfool.prowess.skill.WeaponSkills;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Economy;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PapiExpansion extends PlaceholderExpansion {
   private final ProwessPlugin plugin;
   private final StatService stats;
   private final SkillRegistry skills;
   private final SkillService service;
   private final GearStats gear;
   private final Economy economy;

   public PapiExpansion(ProwessPlugin plugin, StatService stats, SkillRegistry skills, SkillService service, GearStats gear, Economy economy) {
      this.plugin = plugin;
      this.stats = stats;
      this.skills = skills;
      this.service = service;
      this.gear = gear;
      this.economy = economy;
   }

   @NotNull
   public String getIdentifier() {
      return "prowess";
   }

   @NotNull
   public String getAuthor() {
      return "wolfool";
   }

   @NotNull
   public String getVersion() {
      return this.plugin.getPluginMeta().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onPlaceholderRequest(Player player, @NotNull String params) {
      if (player == null) {
         return "";
      }

      String p = params.toLowerCase(Locale.ROOT);
      if (p.equals("money")) {
         return String.valueOf(this.economy.balance(player));
      }

      if (p.equals("level")) {
         return String.valueOf(this.gear.level(player.getInventory().getItemInMainHand()));
      }

      if (p.equals("defense_reduction")) {
         double def = this.stats.get(player, Stat.DEFENSE);
         return String.valueOf(Math.round(Formula.reduction(def, 0.0, this.stats.config().defenseK()) * 100.0));
      }

      if (p.startsWith("stat_")) {
         Stat stat = Stat.byName(p.substring("stat_".length()).replace('_', ' '));
         if (stat == null) {
            return "";
         }

         double v = this.stats.get(player, stat);
         return v == Math.floor(v) ? String.valueOf((long)v) : String.format("%.1f", v);
      } else {
         return p.startsWith("skill_") ? this.skill(player, p.substring("skill_".length())) : null;
      }
   }

   private String skill(Player player, String rest) {
      int cut = rest.lastIndexOf(95);
      if (cut <= 0) {
         return "";
      }

      Bind bind = bindOf(rest.substring(0, cut));
      String what = rest.substring(cut + 1);
      if (bind == null) {
         return "";
      }

      ItemStack held = player.getInventory().getItemInMainHand();
      WeaponSkills weapon = this.skills.forItem(held);
      WeaponSkills.Slot slot = weapon == null ? null : weapon.at(bind);
      if (slot == null) {
         return what.equals("state") ? "none" : "";
      }

      int level = this.gear.level(held);
      boolean locked = level < slot.unlock();
      double left = locked ? 0.0 : this.service.remaining(player, slot.skill());

      return switch (what) {
         case "name" -> strip(slot.skill().name());
         case "state" -> locked ? "lock" : (left > 0.0 ? "cool" : "ready");
         case "cooldown" -> left <= 0.0 ? "0" : (left >= 10.0 ? String.valueOf(Math.round(left)) : String.format("%.1f", left));
         case "percent" -> locked
            ? "0"
            : (left <= 0.0 ? "100" : String.valueOf((int)Math.round((1.0 - left / Math.max(0.1, slot.skill().cooldown())) * 100.0)));
         case "unlock" -> String.valueOf(slot.unlock());
         case "type" -> slot.skill().type().korean();
         default -> "";
      };
   }

   @Nullable
   private static Bind bindOf(String raw) {
      return switch (raw) {
         case "right" -> Bind.RIGHT;
         case "shift_right" -> Bind.SHIFT_RIGHT;
         case "left" -> Bind.LEFT;
         case "shift_left" -> Bind.SHIFT_LEFT;
         default -> null;
      };
   }

   private static String strip(String s) {
      return s.replaceAll("&[0-9a-fk-or]", "");
   }
}
