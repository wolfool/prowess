package com.wolfool.prowess.skill;

import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.util.Msg;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class VfxProbe {
   private final Plugin plugin;
   private final Set<UUID> watching = new HashSet<>();

   public VfxProbe(Plugin plugin) {
      this.plugin = plugin;
   }

   public boolean toggle(Player player) {
      UUID id = player.getUniqueId();
      if (this.watching.remove(id)) {
         return false;
      }

      this.watching.add(id);
      this.start(player);
      return true;
   }

   private void start(final Player player) {
      final Set<UUID> seen = new HashSet<>();

      for (Entity e : player.getNearbyEntities(24.0, 24.0, 24.0)) {
         seen.add(e.getUniqueId());
      }

      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (player.isOnline() && VfxProbe.this.watching.contains(player.getUniqueId())) {
               if (++this.tick > 2400) {
                  VfxProbe.this.watching.remove(player.getUniqueId());
                  player.sendMessage(Msg.of("&8이펙트 살펴보기를 껐습니다 (2분)"));
                  this.cancel();
               } else {
                  for (Entity e : player.getNearbyEntities(24.0, 24.0, 24.0)) {
                     if (seen.add(e.getUniqueId())) {
                        VfxProbe.this.report(player, e);
                     }
                  }
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private void report(Player to, Entity e) {
      StringBuilder sb = new StringBuilder("&e▸ &f").append(e.getType());
      if (e instanceof ArmorStand a) {
         sb.append(" &7marker=").append(flag(a.isMarker())).append(" &7보임=").append(flag(a.isVisible())).append(" &7작음=").append(flag(a.isSmall()));
      } else if (e instanceof Display d) {
         sb.append(" &7보이는거리=").append(String.format("%.1f", d.getViewRange()));
      }

      Location l = e.getLocation();
      sb.append(String.format(" &8(%.1f %.1f %.1f)", l.getX(), l.getY(), l.getZ()));
      to.sendMessage(Msg.of(sb.toString()));
      if (e instanceof LivingEntity le && le.getEquipment() != null) {
         boolean any = false;

         for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack it;
            try {
               it = le.getEquipment().getItem(slot);
            } catch (Exception ignored) {
               continue;
            }

            if (it != null && !it.getType().isAir()) {
               any = true;
               to.sendMessage(Msg.of("   &b" + slot + " &f" + it.getType() + " &7×" + it.getAmount() + " &8" + modelOf(it)));
            }
         }

         if (!any) {
            to.sendMessage(Msg.of("   &c입은 것이 없습니다 — 그래서 안 보입니다"));
         }
      }
   }

   private static String modelOf(ItemStack it) {
      ItemMeta meta = it.getItemMeta();
      if (meta == null) {
         return "메타 없음";
      }

      if (meta.hasItemModel()) {
         return "item_model=" + meta.getItemModel();
      }

      String ce = CraftEngineHook.idOf(it);
      return ce != null ? "CE=" + ce : "&c그림 지정 없음 — 맨 " + it.getType() + " 로 보입니다";
   }

   private static String flag(boolean b) {
      return b ? "&a참" : "&c거짓";
   }
}
