package com.wolfool.prowess.gear;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.skill.Bind;
import com.wolfool.prowess.skill.SkillRegistry;
import com.wolfool.prowess.skill.WeaponSkills;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Economy;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EnhanceGui implements InventoryHolder {
   public static final int SLOT_ITEM = 20;
   public static final int SLOT_ARROW = 22;
   public static final int SLOT_PREVIEW = 24;
   public static final int SLOT_NEED = 31;
   public static final int SLOT_GO = 40;
   private static final int SIZE = 45;
   private final Enhance enhance;
   private final GearStats gear;
   private final ProwessConfig config;
   private final StatService stats;
   private final Economy economy;
   private final SkillRegistry skills;
   private final Player owner;
   private final Inventory inventory;

   public EnhanceGui(Player owner, Enhance enhance, StatService stats, Economy economy, SkillRegistry skills) {
      this.owner = owner;
      this.enhance = enhance;
      this.gear = enhance.gear();
      this.config = enhance.config();
      this.stats = stats;
      this.economy = economy;
      this.skills = skills;
      this.inventory = Bukkit.createInventory(
         this,
         45,
         ((TextComponent)((TextComponent)Component.text("[강화] ").color(NamedTextColor.GOLD))
               .append(Component.text("장비를 눌러 올려놓으세요").color(NamedTextColor.DARK_GRAY)))
            .decoration(TextDecoration.ITALIC, false)
      );
      this.render();
   }

   public Player owner() {
      return this.owner;
   }

   @Nullable
   public ItemStack target() {
      return this.inventory.getItem(20);
   }

   public void render() {
      ItemStack pane = blank();

      for (int i = 0; i < 45; i++) {
         if (i != 20) {
            this.inventory.setItem(i, pane);
         }
      }

      ItemStack item = this.target();
      boolean has = item != null && !item.getType().isAir();
      if (has) {
         this.enhance.imprintIfNeeded(item);
      }

      this.inventory.setItem(22, icon(Material.PAPER, null, "&8→", List.of("&8왼쪽이 지금 · 오른쪽이 올린 뒤")));
      if (!has) {
         this.inventory
            .setItem(
               24,
               icon(
                  Material.PAPER,
                  "prowess:anvil",
                  "&7장비를 올려놓으세요",
                  List.of("&7가방에서 &f클릭&7하면 바로 올라갑니다.", "", "&8무기 · 방어구 · 장신구를 올릴 수 있습니다.", "&8올리면 능력치가 오르고,", "&8무기는 &f새 스킬&8이 열립니다.")
               )
            );
         this.inventory.setItem(31, pane);
         this.inventory.setItem(40, icon(Material.PAPER, "prowess:lock", "&8강화", List.of("&7장비를 먼저 올려놓으세요")));
      } else {
         int level = this.gear.level(item);
         if (!this.gear.isGear(item)) {
            this.inventory.setItem(24, icon(Material.BARRIER, null, "&c강화할 수 없습니다", List.of("&7이 물건에는 능력치가 없습니다.", "&8gear.yml 에 적힌 장비만 강화됩니다.")));
            this.inventory.setItem(40, icon(Material.PAPER, "prowess:lock", "&8강화", List.of("&7강화할 수 있는 장비가 아닙니다")));
         } else if (level >= this.config.maxLevel()) {
            this.inventory.setItem(24, icon(Material.NETHER_STAR, null, "&6&l최대 단계", List.of("&7+" + level + " 가 끝입니다.", "&8더 올릴 수 없습니다.")));
            this.inventory.setItem(40, icon(Material.PAPER, "prowess:lock", "&8강화", List.of("&7더 올릴 수 없습니다")));
         } else {
            this.inventory.setItem(24, this.preview(item, level));
            ProwessConfig.Step step = this.enhance.nextStep(item);
            if (step == null) {
               this.inventory.setItem(40, icon(Material.PAPER, "prowess:lock", "&8강화", List.of("&7다음 단계 설정이 없습니다")));
            } else {
               int have = this.enhance.countMaterial(this.owner, step);
               boolean enoughMat = have >= step.materialAmount();
               boolean enoughMoney = this.economy.has(this.owner, step.cost());
               this.inventory.setItem(31, this.need(step, have, enoughMat, enoughMoney));
               boolean ready = enoughMat && enoughMoney;
               this.inventory
                  .setItem(
                     40,
                     ready
                        ? icon(Material.PAPER, "prowess:anvil", "&a&l강화하기", List.of("&7눌러서 시도합니다.", "&8재료와 비용은 실패해도 안 돌아옵니다."))
                        : icon(Material.PAPER, "prowess:lock", "&8강화 (아직 안 됩니다)", List.of(enoughMat ? "&c돈이 모자랍니다" : "&c재료가 모자랍니다"))
                  );
            }
         }
      }
   }

   private ItemStack preview(ItemStack item, int level) {
      StatMap gain = this.enhance.gainOfNextLevel(item);
      List<String> lore = new ArrayList<>();
      lore.add("&7+" + level + " &8→ &e&l+" + (level + 1));
      lore.add("");

      for (String line : gain.lines("&8늘어남  ")) {
         lore.add(line);
      }

      WeaponSkills weapon = this.skills.forItem(item);
      if (weapon != null) {
         List<String> opening = new ArrayList<>();

         for (Bind bind : Bind.values()) {
            WeaponSkills.Slot slot = weapon.at(bind);
            if (slot != null && slot.unlock() == level + 1) {
               opening.add("  " + bind.color() + bind.korean() + " &f" + slot.skill().name());
            }
         }

         if (!opening.isEmpty()) {
            lore.add("");
            lore.add("&e&l▶ 새 스킬이 열립니다");
            lore.addAll(opening);
         }
      }

      ItemStack shown = item.clone();
      ItemMeta meta = shown.getItemMeta();
      if (meta != null) {
         meta.displayName(this.gear.nameWithLevel(item));
         meta.lore(Msg.lines(lore));
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         shown.setItemMeta(meta);
      }

      return shown;
   }

   private ItemStack need(ProwessConfig.Step step, int have, boolean enoughMat, boolean enoughMoney) {
      List<String> lore = new ArrayList<>();
      lore.add("&7재료  " + (enoughMat ? "&a" : "&c") + have + " &7/ &f" + step.materialAmount());
      if (step.materialId() != null && !step.materialId().isBlank()) {
         lore.add("&8      " + this.materialName(step));
      }

      lore.add("");
      lore.add("&7비용  " + (enoughMoney ? "&f" : "&c") + Msg.comma(step.cost()) + this.economy.unit());
      lore.add("&8      내 지갑 " + Msg.comma(this.economy.balance(this.owner)) + this.economy.unit());
      lore.add("");
      lore.add("&7성공  &f" + Math.round(step.chance() * 100.0) + "%");
      int level = this.gear.level(this.target());
      if (level < this.config.safeLevel()) {
         lore.add("&8      실패해도 단계가 안 내려갑니다 &7(+" + this.config.safeLevel() + "까지)");
      } else if (this.config.downgradeOnFail()) {
         lore.add("&c      실패하면 한 단계 내려갑니다");
      }

      lore.add("&8      장비가 부서지지는 않습니다");
      String ceId = step.materialId();
      return icon(step.materialFallback(), ceId != null && !ceId.isBlank() ? ceId : null, "&b필요한 것", lore);
   }

   private String materialName(ProwessConfig.Step step) {
      ItemStack real = CraftEngineHook.build(step.materialId(), 1);
      if (real != null) {
         ItemMeta meta = real.getItemMeta();
         if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(meta.displayName()));
         }
      }

      return step.materialId();
   }

   private static ItemStack icon(Material mat, @Nullable String model, String name, List<String> lore) {
      ItemStack it = new ItemStack(mat);
      ItemMeta meta = it.getItemMeta();
      if (meta != null) {
         if (model != null && !model.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(model.toLowerCase());
            if (key != null) {
               meta.setItemModel(key);
            }
         }

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

   public void effect(Player player, Enhance.Result result) {
      Location at = player.getLocation().add(0.0, 1.0, 0.0);
      switch (result) {
         case SUCCESS:
            player.playSound(at, Sound.BLOCK_ANVIL_USE, 1.0F, 1.4F);
            player.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.6F);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, at, 40, 0.4, 0.6, 0.4, 0.25);
            player.getWorld().spawnParticle(Particle.END_ROD, at, 15, 0.3, 0.5, 0.3, 0.05);
            break;
         case DOWNGRADED:
            player.playSound(at, Sound.BLOCK_ANVIL_DESTROY, 0.9F, 0.8F);
            player.playSound(at, Sound.ENTITY_ITEM_BREAK, 0.8F, 0.7F);
            player.getWorld().spawnParticle(Particle.SMOKE, at, 40, 0.4, 0.5, 0.4, 0.03);
            break;
         case KEPT:
            player.playSound(at, Sound.BLOCK_ANVIL_LAND, 0.7F, 0.9F);
            player.getWorld().spawnParticle(Particle.SMOKE, at, 15, 0.3, 0.4, 0.3, 0.02);
            break;
         default:
            player.playSound(at, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.7F);
      }
   }

   public StatService stats() {
      return this.stats;
   }

   @NotNull
   public Inventory getInventory() {
      return this.inventory;
   }
}
