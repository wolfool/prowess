package com.wolfool.prowess.accessory;

import com.wolfool.prowess.gear.GearStats;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AccessoryGui implements Listener {
   private static final int SIZE = 27;
   private static final int[] SLOTS = new int[]{10, 11, 13, 15};
   private final Plugin plugin;
   private final Accessories accessories;
   private final GearStats gear;
   private final StatService stats;

   public AccessoryGui(Plugin plugin, Accessories accessories, GearStats gear, StatService stats) {
      this.plugin = plugin;
      this.accessories = accessories;
      this.gear = gear;
      this.stats = stats;
   }

   public void open(Player player) {
      final Inventory[] box = new Inventory[1];
      Inventory inv = Bukkit.createInventory(
         new InventoryHolder() {
            @NotNull
            public Inventory getInventory() {
               return box[0];
            }
         },
         27,
         ((TextComponent)((TextComponent)Component.text("[장신구] ").color(NamedTextColor.LIGHT_PURPLE))
               .append(Component.text("끼고 빼세요").color(NamedTextColor.DARK_GRAY)))
            .decoration(TextDecoration.ITALIC, false)
      );
      box[0] = inv;
      ItemStack pane = blank();

      for (int i = 0; i < 27; i++) {
         inv.setItem(i, pane);
      }

      ItemStack[] worn = this.accessories.all(player);
      Accessories.Slot[] slots = Accessories.Slot.values();

      for (int i = 0; i < SLOTS.length && i < slots.length; i++) {
         ItemStack it = worn[i];
         inv.setItem(SLOTS[i], it != null && !it.getType().isAir() ? it : this.placeholder(slots[i]));
      }

      inv.setItem(22, this.summary(player, inv));
      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.3F);
   }

   private boolean isOurs(Inventory inv) {
      if (inv.getSize() != 27) {
         return false;
      }

      ItemStack mark = inv.getItem(22);
      if (mark == null) {
         return false;
      }

      ItemMeta meta = mark.getItemMeta();
      return meta != null && meta.hasDisplayName() && PlainTextComponentSerializer.plainText().serialize(meta.displayName()).contains("장신구가 주는 힘");
   }

   private ItemStack placeholder(Accessories.Slot slot) {
      String model = switch (slot) {
         case RING1, RING2 -> "prowess:ring";
         default -> null;
      };

      Material icon = switch (slot) {
         case RING1, RING2 -> Material.PAPER;
         case NECKLACE -> Material.AMETHYST_SHARD;
         case EARRING -> Material.QUARTZ;
      };
      ItemStack it = item(icon, "&8" + slot.korean() + " &7(빈 칸)", List.of("&7여기에 &f" + kindKorean(slot.kind()) + " &7을(를) 놓습니다", "", "&8가방에서 클릭하거나 끌어다 놓으세요"));
      if (model != null) {
         ItemMeta meta = it.getItemMeta();
         NamespacedKey key = NamespacedKey.fromString(model);
         if (meta != null && key != null) {
            meta.setItemModel(key);
            it.setItemMeta(meta);
         }
      }

      return it;
   }

   private static String kindKorean(String kind) {
      return switch (kind) {
         case "ring" -> "반지";
         case "necklace" -> "목걸이";
         case "earring" -> "귀걸이";
         default -> kind;
      };
   }

   private ItemStack summary(Player player, @Nullable Inventory live) {
      StatMap sum = StatMap.empty();
      List<ItemStack> worn = new ArrayList<>();
      if (live != null) {
         for (int s : SLOTS) {
            ItemStack it = live.getItem(s);
            if (it != null && this.accessories.kindOf(it) != null) {
               worn.add(it);
            }
         }
      } else {
         worn.addAll(this.accessories.worn(player));
      }

      for (ItemStack it : worn) {
         sum.addAll(this.gear.effective(it, this.stats.config().enhancePerLevel()));
      }

      List<String> lore = new ArrayList<>();
      if (sum.isEmpty()) {
         lore.add("&8아직 낀 것이 없습니다");
      } else {
         lore.addAll(sum.lines(""));
      }

      lore.add("");
      lore.add("&8지금 올려 둔 것을 그대로 셉니다");
      lore.add("&8창을 닫으면 저장됩니다");
      return item(Material.NETHER_STAR, "&e장신구가 주는 힘", lore);
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (this.isOurs(event.getInventory())) {
         for (int raw : event.getRawSlots()) {
            if (raw < event.getInventory().getSize() && !isSlot(raw)) {
               event.setCancelled(true);
               return;
            }
         }
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      Inventory top = event.getInventory();
      if (this.isOurs(top)) {
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() != top) {
               ItemStack moved = event.getCurrentItem();
               if (moved != null && !moved.getType().isAir()) {
                  ItemStack cursor = event.getCursor();
                  if (cursor == null || cursor.getType().isAir()) {
                     String kind = this.accessories.kindOf(moved);
                     if (kind == null) {
                        if (event.isShiftClick()) {
                           event.setCancelled(true);
                        }
                     } else {
                        event.setCancelled(true);
                        Accessories.Slot[] slots = Accessories.Slot.values();

                        for (int i = 0; i < SLOTS.length && i < slots.length; i++) {
                           if (slots[i].kind().equalsIgnoreCase(kind)) {
                              ItemStack now = top.getItem(SLOTS[i]);
                              boolean occupied = now != null && this.accessories.kindOf(now) != null;
                              if (!occupied) {
                                 top.setItem(SLOTS[i], moved.clone());
                                 event.setCurrentItem(null);
                                 player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 0.8F, 1.4F);
                                 this.refresh(top, player);
                                 return;
                              }
                           }
                        }

                        player.sendMessage(Msg.of("&7[장신구] &f" + kindKorean(kind) + " &7을(를) 낄 빈 칸이 없습니다."));
                     }
                  }
               }
            } else {
               int slot = event.getRawSlot();
               if (!isSlot(slot)) {
                  event.setCancelled(true);
               } else {
                  Accessories.Slot which = slotAt(slot);
                  ItemStack cursor = event.getCursor();
                  ItemStack current = event.getCurrentItem();
                  boolean cursorEmpty = cursor == null || cursor.getType().isAir();
                  if (cursorEmpty && current != null && this.accessories.kindOf(current) == null) {
                     event.setCancelled(true);
                  } else if (!cursorEmpty && which != null && !this.accessories.fits(which, cursor)) {
                     event.setCancelled(true);
                     player.sendMessage(Msg.of("&c[장신구] " + which.korean() + " 칸에는 &f" + kindKorean(which.kind()) + " &c만 들어갑니다."));
                  } else {
                     this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                        if (which != null) {
                           ItemStack after = top.getItem(slot);
                           if (after == null || after.getType().isAir()) {
                              top.setItem(slot, this.placeholder(which));
                           }
                        }

                        this.refresh(top, player);
                     });
                  }
               }
            }
         }
      }
   }

   private void refresh(Inventory top, Player player) {
      top.setItem(22, this.summary(player, top));
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      Inventory top = event.getInventory();
      if (this.isOurs(top)) {
         if (event.getPlayer() instanceof Player player) {
            ItemStack[] var7 = new ItemStack[Accessories.Slot.values().length];

            for (int i = 0; i < SLOTS.length && i < var7.length; i++) {
               ItemStack it = top.getItem(SLOTS[i]);
               var7[i] = it != null && this.accessories.kindOf(it) != null ? it.clone() : null;
            }

            this.accessories.save(player, var7);
            this.stats.refreshLater(player);
         }
      }
   }

   private static boolean isSlot(int raw) {
      for (int s : SLOTS) {
         if (s == raw) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   private static Accessories.Slot slotAt(int raw) {
      Accessories.Slot[] slots = Accessories.Slot.values();

      for (int i = 0; i < SLOTS.length && i < slots.length; i++) {
         if (SLOTS[i] == raw) {
            return slots[i];
         }
      }

      return null;
   }

   private static ItemStack item(Material mat, String name, List<String> lore) {
      ItemStack it = new ItemStack(mat);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.displayName(Msg.of(name));
         meta.lore(Msg.lines(lore));
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
         it.setItemMeta(meta);
      }

      return it;
   }

   private static ItemStack blank() {
      ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.text(" "));
         it.setItemMeta(meta);
      }

      return it;
   }
}
