package com.wolfool.prowess.title;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.integration.EpithetHook;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class TitleGui implements Listener {
   private static final int SIZE = 54;
   private static final int FIRST = 9;
   private static final int LAST = 44;
   private static final int BACK = 49;
   private final ProwessConfig config;
   private final EpithetHook epithet;
   private final StatService stats;
   private final Map<Inventory, TitleGui.View> views = new WeakHashMap<>();

   public TitleGui(ProwessConfig config, EpithetHook epithet, StatService stats) {
      this.config = config;
      this.epithet = epithet;
      this.stats = stats;
   }

   public void open(Player player) {
      Inventory inv = this.create("칭호에 능력치 붙이기");
      this.renderList(inv);
      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.3F);
   }

   private Inventory create(String title) {
      final Inventory[] box = new Inventory[1];
      Inventory inv = Bukkit.createInventory(
         new InventoryHolder() {
            @NotNull
            public Inventory getInventory() {
               return box[0];
            }
         },
         54,
         ((TextComponent)((TextComponent)Component.text("[칭호] ").color(NamedTextColor.LIGHT_PURPLE))
               .append(Component.text(title).color(NamedTextColor.DARK_GRAY)))
            .decoration(TextDecoration.ITALIC, false)
      );
      box[0] = inv;
      return inv;
   }

   private void renderList(Inventory inv) {
      inv.clear();
      ItemStack pane = blank();

      for (int i = 0; i < 9; i++) {
         inv.setItem(i, pane);
      }

      for (int i = 45; i < 54; i++) {
         inv.setItem(i, pane);
      }

      Set<String> ids = new LinkedHashSet<>(this.epithet.allTitleIds());
      ids.addAll(this.config.allTitleStats().keySet());
      List<String> here = new ArrayList<>(ids);
      inv.setItem(
         4,
         simple(
            Material.NAME_TAG,
            "&d칭호 &f" + here.size() + "&d종",
            List.of("&7칭호마다 능력치를 붙일 수 있습니다.", "&8칭호 자체는 Epithet 이 만듭니다 &7(/칭호)", "", this.epithet.connected() ? "&aEpithet 연결됨" : "&8Epithet 없음 — 적어 둔 것만 보입니다")
         )
      );

      for (int i = 0; i < here.size() && 9 + i <= 44; i++) {
         inv.setItem(9 + i, this.titleCard(here.get(i)));
      }

      this.views.put(inv, new TitleGui.View(here, null));
   }

   private ItemStack titleCard(String id) {
      StatMap map = this.config.titleStats(id);
      List<String> lore = new ArrayList<>();
      lore.add("&8" + id);
      lore.add("");
      if (map != null && !map.isEmpty()) {
         lore.addAll(map.lines(""));
      } else {
         lore.add("&8아직 아무것도 안 붙었습니다");
      }

      lore.add("");
      lore.add("&e클릭 &7— 능력치 고치기");
      return simple(map != null && !map.isEmpty() ? Material.NAME_TAG : Material.PAPER, "&f" + id, lore);
   }

   private void renderEdit(Inventory inv, String titleId) {
      inv.clear();
      ItemStack pane = blank();

      for (int i = 0; i < 54; i++) {
         inv.setItem(i, pane);
      }

      StatMap map = this.config.titleStats(titleId);
      inv.setItem(
         4, simple(Material.NAME_TAG, "&d" + titleId, List.of("&7이 칭호를 단 사람이 받는 능력치입니다.", "", "&e좌클릭 &7+1 &8/ &e우클릭 &7-1", "&e쉬프트 &7±10", "&8누르는 즉시 저장됩니다"))
      );
      int[] slots = new int[]{19, 20, 21, 22, 23, 24, 25, 30, 32};
      Stat[] all = Stat.values();

      for (int i = 0; i < all.length && i < slots.length; i++) {
         Stat stat = all[i];
         double v = map == null ? 0.0 : map.get(stat);
         inv.setItem(
            slots[i],
            simple(
               iconOf(stat),
               stat.color() + stat.korean() + " &f" + stat.format(v),
               List.of("&8" + (v == 0.0 ? "안 줍니다" : "지금 " + stat.format(v)), "", "&e좌클릭 &7+1 &8/ &e우클릭 &7-1", "&e쉬프트 &7±10")
            )
         );
      }

      inv.setItem(49, simple(Material.ARROW, "&f목록으로", List.of()));
      TitleGui.View old = this.views.get(inv);
      this.views.put(inv, new TitleGui.View(old == null ? List.of() : old.titles(), titleId));
   }

   private static Material iconOf(Stat stat) {
      return switch (stat) {
         case ATTACK -> Material.IRON_SWORD;
         case DEFENSE -> Material.SHIELD;
         case HEALTH -> Material.RED_DYE;
         case CRIT_CHANCE -> Material.QUARTZ;
         case CRIT_DAMAGE -> Material.BLAZE_POWDER;
         case PIERCE -> Material.ARROW;
         case LIFESTEAL -> Material.GHAST_TEAR;
         case ATTACK_SPEED -> Material.CLOCK;
         case MOVE_SPEED -> Material.FEATHER;
      };
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (this.views.containsKey(event.getInventory())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      Inventory top = event.getInventory();
      TitleGui.View view = this.views.get(top);
      if (view != null) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player player) {
            if (event.getClickedInventory() == top) {
               int slot = event.getRawSlot();
               if (view.editing() == null) {
                  if (slot >= 9 && slot <= 44) {
                     int index = slot - 9;
                     if (index < view.titles().size()) {
                        this.renderEdit(top, view.titles().get(index));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.5F);
                     }
                  }
               } else if (slot == 49) {
                  this.renderList(top);
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.0F);
               } else {
                  int[] slots = new int[]{19, 20, 21, 22, 23, 24, 25, 30, 32};
                  Stat[] all = Stat.values();

                  for (int i = 0; i < slots.length && i < all.length; i++) {
                     if (slots[i] == slot) {
                        Stat stat = all[i];
                        double step = event.isShiftClick() ? 10.0 : 1.0;
                        boolean down = event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT;
                        StatMap map = this.config.titleStats(view.editing());
                        double now = map == null ? 0.0 : map.get(stat);
                        this.config.setTitleStat(view.editing(), stat, now + (down ? -step : step));
                        this.renderEdit(top, view.editing());
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, down ? 1.0F : 1.6F);
                        Bukkit.getOnlinePlayers().forEach(this.stats::refreshLater);
                        return;
                     }
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
         if (!lore.isEmpty()) {
            meta.lore(Msg.lines(lore));
         }

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

   private record View(List<String> titles, String editing) {
   }
}
