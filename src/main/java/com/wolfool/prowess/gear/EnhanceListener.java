package com.wolfool.prowess.gear;

import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Msg;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class EnhanceListener implements Listener {
   private final Plugin plugin;
   private final Enhance enhance;
   private final StatService stats;

   public EnhanceListener(Plugin plugin, Enhance enhance, StatService stats) {
      this.plugin = plugin;
      this.enhance = enhance;
      this.stats = stats;
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof EnhanceGui gui) {
         for (int raw : event.getRawSlots()) {
            if (raw < event.getInventory().getSize() && raw != 20) {
               event.setCancelled(true);
               return;
            }
         }

         this.plugin.getServer().getScheduler().runTask(this.plugin, gui::render);
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof EnhanceGui gui) {
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() == event.getInventory()) {
               int slot = event.getRawSlot();
               if (slot == 20) {
                  this.plugin.getServer().getScheduler().runTask(this.plugin, gui::render);
               } else {
                  event.setCancelled(true);
                  if (slot == 40) {
                     if (event.getClick() != ClickType.DOUBLE_CLICK) {
                        ItemStack item = gui.target();
                        if (item != null && !item.getType().isAir()) {
                           Enhance.Result result = this.enhance.attempt(player, item);
                           gui.effect(player, result);
                           switch (result) {
                              case SUCCESS:
                                 player.sendMessage(Msg.of("&a[강화] 성공했습니다. &f+" + this.enhance.gear().level(item)));
                                 break;
                              case KEPT:
                                 player.sendMessage(Msg.of("&7[강화] 실패했습니다. &8단계는 그대로입니다."));
                                 break;
                              case DOWNGRADED:
                                 player.sendMessage(Msg.of("&c[강화] 실패했습니다. &7한 단계 내려갔습니다 &f+" + this.enhance.gear().level(item)));
                                 break;
                              case MAXED:
                                 player.sendMessage(Msg.of("&6[강화] 최대 단계입니다."));
                                 break;
                              case NOT_GEAR:
                                 player.sendMessage(Msg.of("&c[강화] 강화할 수 있는 장비가 아닙니다."));
                                 break;
                              case NO_MONEY:
                                 player.sendMessage(Msg.of("&c[강화] 돈이 모자랍니다."));
                                 break;
                              case NO_MATERIAL:
                                 player.sendMessage(Msg.of("&c[강화] 재료가 모자랍니다."));
                           }

                           gui.render();
                        } else {
                           player.sendMessage(Msg.of("&c[강화] 장비를 넣어 주세요."));
                        }
                     }
                  }
               }
            } else {
               ItemStack moved = event.getCurrentItem();
               if (moved != null && !moved.getType().isAir()) {
                  ItemStack cursor = event.getCursor();
                  if (cursor == null || cursor.getType().isAir()) {
                     event.setCancelled(true);
                     ItemStack now = gui.target();
                     if (now != null && !now.getType().isAir()) {
                        event.setCurrentItem(now.clone());
                     } else {
                        event.setCurrentItem(null);
                     }

                     gui.getInventory().setItem(20, moved.clone());
                     player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7F, 1.4F);
                     gui.render();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      if (event.getInventory().getHolder() instanceof EnhanceGui gui) {
         if (event.getPlayer() instanceof Player player) {
            ItemStack left = gui.target();
            if (left != null && !left.getType().isAir()) {
               player.getInventory().addItem(new ItemStack[]{left}).values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
            }

            gui.getInventory().setItem(20, null);
            this.stats.refreshLater(player);
         }
      }
   }
}
