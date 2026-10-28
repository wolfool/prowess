package com.wolfool.prowess.gear;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.skill.Bind;
import com.wolfool.prowess.skill.SkillRegistry;
import com.wolfool.prowess.skill.WeaponSkills;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class WeaponGui implements Listener {
   private static final int SIZE = 54;
   private static final int PAGE_SLOT = 8;
   private static final int FIRST = 9;
   private static final int LAST = 44;
   private static final String MARK = "장비 고르기";
   private final ProwessConfig config;
   private final GearStats gear;
   private final SkillRegistry skills;
   private final Map<Inventory, List<String>> shown = new WeakHashMap<>();
   private final Map<Inventory, Integer> pages = new WeakHashMap<>();

   public WeaponGui(ProwessConfig config, GearStats gear, SkillRegistry skills) {
      this.config = config;
      this.gear = gear;
      this.skills = skills;
   }

   public void open(Player player) {
      this.open(player, 0);
   }

   private void open(Player player, int page) {
      final Inventory[] box = new Inventory[1];
      Inventory inv = Bukkit.createInventory(
         new InventoryHolder() {
            @NotNull
            public Inventory getInventory() {
               return box[0];
            }
         },
         54,
         ((TextComponent)((TextComponent)Component.text("[장비 고르기] ").color(NamedTextColor.GOLD))
               .append(Component.text("눌러서 받습니다").color(NamedTextColor.DARK_GRAY)))
            .decoration(TextDecoration.ITALIC, false)
      );
      box[0] = inv;
      this.render(inv, page);
      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.3F);
   }

   private void render(Inventory inv, int page) {
      inv.clear();
      List<String> keys = new ArrayList<>(this.config.allGearKeys());
      int per = 36;
      int total = Math.max(1, (keys.size() + per - 1) / per);
      if (page >= total) {
         page = 0;
      }

      if (page < 0) {
         page = total - 1;
      }

      this.pages.put(inv, page);
      ItemStack pane = blank();

      for (int i = 0; i < 9; i++) {
         inv.setItem(i, pane);
      }

      for (int i = 45; i < 54; i++) {
         inv.setItem(i, pane);
      }

      inv.setItem(4, this.info(keys.size()));
      if (total > 1) {
         inv.setItem(8, this.pageButton(page, total));
      }

      List<String> here = new ArrayList<>();
      int from = page * per;

      for (int i = 0; i < per && from + i < keys.size(); i++) {
         String key = keys.get(from + i);
         here.add(key);
         inv.setItem(9 + i, this.card(key));
      }

      this.shown.put(inv, here);
   }

   private ItemStack card(String key) {
      ItemStack it = this.make(key);
      ItemMeta meta = it.getItemMeta();
      if (meta == null) {
         return it;
      }

      ProwessConfig.GearEntry entry = this.config.entryFor(key.indexOf(58) >= 0 ? key : null, it.getType());
      List<String> lore = new ArrayList<>();
      lore.add("&8" + key);
      lore.add("");
      if (entry != null && !entry.stats().isEmpty()) {
         lore.addAll(entry.stats().lines(""));
      } else {
         lore.add("&8능력치 없음");
      }

      WeaponSkills weapon = this.skills.weapons().get(key.toLowerCase(Locale.ROOT));
      if (weapon != null) {
         lore.add("");
         lore.add("&e&l스킬");

         for (Bind bind : Bind.values()) {
            WeaponSkills.Slot slot = weapon.at(bind);
            if (slot != null) {
               lore.add("  " + bind.color() + bind.korean() + " &8+" + slot.unlock());
               lore.add("    &f" + slot.skill().name() + " &8[" + slot.skill().type().korean() + "]");

               for (String l : slot.skill().lore()) {
                  lore.add("    " + l);
               }
            }
         }
      }

      lore.add("");
      lore.add("&e클릭 &7받기 &8· &e쉬프트 클릭 &7&f+12&7 로 받기");
      meta.lore(Msg.lines(lore));
      meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
      it.setItemMeta(meta);
      return it;
   }

   private ItemStack make(String key) {
      if (key.indexOf(58) >= 0) {
         ItemStack ce = CraftEngineHook.build(key, 1);
         return ce != null ? ce : new ItemStack(Material.PAPER);
      } else {
         Material m = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
         return new ItemStack(m == null ? Material.PAPER : m);
      }
   }

   private ItemStack info(int count) {
      return simple(
         Material.WRITABLE_BOOK,
         "&6장비 &f" + count + "&6종",
         List.of(
            "&7gear.yml 에 적힌 것을 다 보여 줍니다.",
            "&8무기를 새로 만들면 여기에 저절로 들어옵니다.",
            "",
            "&e클릭 &7— 그냥 받기",
            "&e쉬프트 클릭 &7— &f+12 &7로 받기 (스킬 넷이 다 열립니다)",
            "&8스킬을 시험하려면 강화된 채로 받는 게 빠릅니다"
         )
      );
   }

   private ItemStack pageButton(int page, int total) {
      ItemStack it = new ItemStack(Material.PAPER);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         if (page + 1 <= 12) {
            meta.setItemModel(new NamespacedKey("ui", "page_" + (page + 1)));
         }

         meta.displayName(Msg.of("&e" + (page + 1) + " &7/ &f" + total + " &7쪽"));
         meta.lore(Msg.lines(List.of("&8클릭 · 다음 쪽")));
         it.setItemMeta(meta);
      }

      return it;
   }

   private boolean isOurs(Inventory inv) {
      return this.shown.containsKey(inv);
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (this.isOurs(event.getInventory())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      Inventory top = event.getInventory();
      if (this.isOurs(top)) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() == top) {
               int slot = event.getRawSlot();
               List<String> here = this.shown.get(top);
               int page = this.pages.getOrDefault(top, 0);
               if (slot == 8) {
                  this.render(top, page + 1);
                  player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8F, 1.2F);
               } else if (slot >= 9 && slot <= 44) {
                  int index = slot - 9;
                  if (here != null && index < here.size()) {
                     String key = here.get(index);
                     ItemStack give = this.make(key);
                     ProwessConfig.GearEntry entry = this.config.entryFor(key.indexOf(58) >= 0 ? key : null, give.getType());
                     StatMap stats = entry == null ? StatMap.empty() : entry.stats();
                     int level = event.isShiftClick() ? Math.min(12, this.config.maxLevel()) : 0;
                     this.gear.write(give, stats, level, this.config.enhancePerLevel());
                     player.getInventory()
                        .addItem(new ItemStack[]{give})
                        .values()
                        .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
                     player.sendMessage(Msg.of("&a[장비] &f" + key + (level > 0 ? " &e+" + level : "") + " &a을(를) 받았습니다."));
                     player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.2F);
                  }
               }
            }
         }
      }
   }

   private static ItemStack simple(Material mat, String name, List<String> lore) {
      ItemStack it = new ItemStack(mat);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         meta.displayName(Msg.of(name));
         meta.lore(Msg.lines(lore));
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
