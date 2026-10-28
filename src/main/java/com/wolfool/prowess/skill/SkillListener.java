package com.wolfool.prowess.skill;

import com.wolfool.prowess.gear.GearStats;
import com.wolfool.prowess.util.Msg;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class SkillListener implements Listener {
   private final SkillRegistry registry;
   private final SkillService service;
   private final GearStats gear;

   public SkillListener(SkillRegistry registry, SkillService service, GearStats gear) {
      this.registry = registry;
      this.service = service;
      this.gear = gear;
   }

   @EventHandler(priority = EventPriority.NORMAL)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Player player = event.getPlayer();
         boolean sneak = player.isSneaking();

         Bind bind = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> sneak ? Bind.SHIFT_RIGHT : Bind.RIGHT;
            case LEFT_CLICK_AIR -> sneak ? Bind.SHIFT_LEFT : Bind.LEFT;
            default -> null;
         };
         if (bind != null) {
            if (this.fire(player, bind, true)) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onAttack(PrePlayerAttackEntityEvent event) {
      Player player = event.getPlayer();
      Bind bind = player.isSneaking() ? Bind.SHIFT_LEFT : Bind.LEFT;
      if (this.fire(player, bind, false)) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onInteractEntity(PlayerInteractEntityEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Player player = event.getPlayer();
         Bind bind = player.isSneaking() ? Bind.SHIFT_RIGHT : Bind.RIGHT;
         if (this.fire(player, bind, true)) {
            event.setCancelled(true);
         }
      }
   }

   private boolean fire(Player player, Bind bind, boolean loud) {
      if (!player.hasPermission("prowess.skill")) {
         return false;
      }

      ItemStack held = player.getInventory().getItemInMainHand();
      WeaponSkills weapon = this.registry.forItem(held);
      if (weapon == null) {
         return false;
      }

      WeaponSkills.Slot slot = weapon.at(bind);
      if (slot == null) {
         return false;
      }

      int level = this.gear.level(held);
      if (level < slot.unlock()) {
         if (loud) {
            player.sendActionBar(Msg.of("&8" + bind.color() + slot.skill().name() + " &8은 &7+" + slot.unlock() + " &8부터 (지금 +" + level + ")"));
         }

         return false;
      } else {
         return !loud && this.service.remaining(player, slot.skill()) > 0.0 ? false : this.service.use(player, slot.skill());
      }
   }

   @EventHandler
   public void onHeld(PlayerItemHeldEvent event) {
      Player player = event.getPlayer();
      ItemStack now = player.getInventory().getItem(event.getNewSlot());
      WeaponSkills weapon = this.registry.forItem(now);
      if (weapon != null) {
         int level = this.gear.level(now);
         StringBuilder sb = new StringBuilder();

         for (Bind bind : Bind.values()) {
            WeaponSkills.Slot slot = weapon.at(bind);
            if (slot != null) {
               if (sb.length() > 0) {
                  sb.append(" &8· ");
               }

               if (level >= slot.unlock()) {
                  sb.append(bind.color()).append(bind.tag()).append(" &f").append(strip(slot.skill().name()));
               } else {
                  sb.append("&8").append(bind.tag()).append(" \ud83d\udd12+").append(slot.unlock());
               }
            }
         }

         if (sb.length() > 0) {
            player.sendActionBar(Msg.of(sb.toString()));
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.service.clear(event.getPlayer());
   }

   private static String strip(String s) {
      return s.replaceAll("&[0-9a-fk-or]", "");
   }
}
