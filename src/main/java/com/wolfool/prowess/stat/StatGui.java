package com.wolfool.prowess.stat;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.combat.Formula;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

public final class StatGui implements InventoryHolder, Listener {
   private static final int SIZE = 45;
   private final StatService stats;
   private final ProwessConfig config;
   private Inventory inventory;

   public StatGui(StatService stats, ProwessConfig config) {
      this.stats = stats;
      this.config = config;
   }

   public void open(Player player) {
      this.stats.invalidate(player);
      StatMap map = this.stats.of(player);
      Inventory inv = Bukkit.createInventory(
         this,
         45,
         ((TextComponent)((TextComponent)Component.text("[능력치] ").color(NamedTextColor.GOLD))
               .append(Component.text(player.getName()).color(NamedTextColor.WHITE)))
            .decoration(TextDecoration.ITALIC, false)
      );
      ItemStack pane = blank();

      for (int i = 0; i < 45; i++) {
         inv.setItem(i, pane);
      }

      int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20};
      Stat[] order = Stat.values();

      for (int i = 0; i < order.length && i < slots.length; i++) {
         inv.setItem(slots[i], this.card(order[i], map));
      }

      inv.setItem(31, this.summary(player, map));
      this.inventory = inv;
      player.openInventory(inv);
   }

   private ItemStack card(Stat stat, StatMap map) {
      double v = map.get(stat);

      Material icon = switch (stat) {
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
      List<String> lore = new ArrayList<>();
      lore.add("&f" + stat.format(v));
      lore.add("");
      switch (stat) {
         case ATTACK:
            lore.add("&8때릴 때 이만큼 더 들어갑니다");
            break;
         case DEFENSE:
            double cut = Formula.reduction(v, 0.0, this.config.defenseK());
            lore.add("&7받는 피해 &f-" + Msg.percent(cut));
            lore.add("&7맷집 &f×" + String.format("%.2f", Formula.effectiveHealthFactor(v, this.config.defenseK())));
            lore.add("");
            lore.add("&8상한이 없습니다. 올릴수록 계속 단단해집니다");
            break;
         case HEALTH:
            lore.add("&8하트 " + Msg.trim(v / 2.0) + "칸만큼 늘어납니다");
            break;
         case CRIT_CHANCE:
            lore.add("&8이 확률로 치명타가 터집니다");
            break;
         case CRIT_DAMAGE:
            lore.add("&8치명타일 때 더 들어가는 피해입니다");
            break;
         case PIERCE:
            lore.add("&8상대 방어를 이만큼 무시합니다");
            break;
         case LIFESTEAL:
            lore.add("&8준 피해의 이만큼을 회복합니다");
            break;
         default:
            lore.add("&8" + stat.korean());
      }

      return item(icon, stat.color() + stat.korean(), lore);
   }

   private ItemStack summary(Player player, StatMap map) {
      AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
      double hp = max == null ? 20.0 : max.getValue();
      double def = map.get(Stat.DEFENSE);
      List<String> lore = new ArrayList<>();
      lore.add("&7체력 &f" + Msg.trim(hp) + " &8(하트 " + Msg.trim(hp / 2.0) + "칸)");
      lore.add("&7방어 &f" + Msg.trim(def) + " &8→ 실질 " + Msg.trim(hp * Formula.effectiveHealthFactor(def, this.config.defenseK())));
      lore.add("");
      lore.add("&8이 값은 장비 · 강화 · 장신구 · 직업을 다 더한 것입니다");
      lore.add("&8/강화 · /장신구 로 올릴 수 있습니다");
      return item(Material.NETHER_STAR, "&e버티는 힘", lore);
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

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof StatGui) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof StatGui) {
         event.setCancelled(true);
      }
   }

   @NotNull
   public Inventory getInventory() {
      return this.inventory;
   }
}
