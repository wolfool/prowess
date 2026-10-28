package com.wolfool.prowess.skill;

import com.wolfool.prowess.gear.GearStats;
import com.wolfool.prowess.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SkillHud {
   private static final long PERIOD = 4L;
   private final Plugin plugin;
   private final SkillRegistry registry;
   private final SkillService service;
   private final GearStats gear;

   public SkillHud(Plugin plugin, SkillRegistry registry, SkillService service, GearStats gear) {
      this.plugin = plugin;
      this.registry = registry;
      this.service = service;
      this.gear = gear;
   }

   public void start() {
      (new BukkitRunnable() {
         public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
               if (!SkillHud.this.service.recentlyCast(player)) {
                  String line = SkillHud.this.line(player);
                  if (line != null) {
                     player.sendActionBar(Msg.of(line));
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 4L, 4L);
   }

   private String line(Player player) {
      ItemStack held = player.getInventory().getItemInMainHand();
      WeaponSkills weapon = this.registry.forItem(held);
      if (weapon == null) {
         return null;
      }

      int level = this.gear.level(held);
      StringBuilder sb = new StringBuilder();

      for (Bind bind : Bind.values()) {
         WeaponSkills.Slot slot = weapon.at(bind);
         if (slot != null) {
            if (sb.length() > 0) {
               sb.append(" &8┃ ");
            }

            if (level < slot.unlock()) {
               sb.append("&8").append(bind.tag()).append(" \ud83d\udd12+").append(slot.unlock());
            } else {
               double left = this.service.remaining(player, slot.skill());
               sb.append(bind.color()).append(bind.tag()).append(' ');
               if (left > 0.0) {
                  sb.append("&8").append(bar(left, slot.skill().cooldown())).append(" &7").append(fmt(left));
               } else {
                  sb.append("&f").append(strip(slot.skill().name()));
               }
            }
         }
      }

      return sb.length() == 0 ? null : sb.toString();
   }

   private static String bar(double left, double total) {
      int filled = (int)Math.round((1.0 - left / Math.max(0.1, total)) * 5.0);
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < 5; i++) {
         sb.append(i < filled ? "&7▰" : "&8▱");
      }

      return sb.toString();
   }

   private static String fmt(double seconds) {
      return seconds >= 10.0 ? String.valueOf(Math.round(seconds)) : String.format("%.1f", seconds);
   }

   private static String strip(String s) {
      return s.replaceAll("&[0-9a-fk-or]", "");
   }
}
