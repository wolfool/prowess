package com.wolfool.prowess.gear;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.util.Economy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class Enhance {
   private final ProwessConfig config;
   private final GearStats gear;
   private final Economy economy;

   public Enhance(ProwessConfig config, GearStats gear, Economy economy) {
      this.config = config;
      this.gear = gear;
      this.economy = economy;
   }

   public boolean imprintIfNeeded(ItemStack item) {
      if (item == null || item.getType().isAir()) {
         return false;
      } else if (this.gear.isGear(item)) {
         return false;
      } else {
         ProwessConfig.GearEntry entry = this.config.entryFor(CraftEngineHook.idOf(item), item.getType());
         if (entry != null && !entry.stats().isEmpty()) {
            this.gear.write(item, entry.stats(), this.gear.level(item), this.config.enhancePerLevel());
            return true;
         } else {
            return false;
         }
      }
   }

   @Nullable
   public ProwessConfig.Step nextStep(ItemStack item) {
      return this.config.step(this.gear.level(item) + 1);
   }

   public boolean hasMaterial(Player player, @Nullable ProwessConfig.Step step) {
      return step != null && step.materialAmount() > 0 ? this.countMaterial(player, step) >= step.materialAmount() : true;
   }

   public int countMaterial(Player player, @Nullable ProwessConfig.Step step) {
      if (step == null) {
         return 0;
      }

      int n = 0;

      for (ItemStack it : player.getInventory().getStorageContents()) {
         if (this.matches(it, step)) {
            n += it.getAmount();
         }
      }

      return n;
   }

   private boolean matches(@Nullable ItemStack it, ProwessConfig.Step step) {
      if (it != null && !it.getType().isAir()) {
         String want = step.materialId();
         return want != null && !want.isBlank()
            ? want.equalsIgnoreCase(CraftEngineHook.idOf(it))
            : it.getType() == step.materialFallback() && CraftEngineHook.idOf(it) == null;
      } else {
         return false;
      }
   }

   private void takeMaterial(Player player, ProwessConfig.Step step) {
      int left = step.materialAmount();
      ItemStack[] contents = player.getInventory().getStorageContents();

      for (int i = 0; i < contents.length && left > 0; i++) {
         ItemStack it = contents[i];
         if (this.matches(it, step)) {
            int take = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) {
               contents[i] = null;
            }

            left -= take;
         }
      }

      player.getInventory().setStorageContents(contents);
   }

   public Enhance.Result attempt(Player player, ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         this.imprintIfNeeded(item);
         if (!this.gear.isGear(item)) {
            return Enhance.Result.NOT_GEAR;
         }

         int level = this.gear.level(item);
         if (level >= this.config.maxLevel()) {
            return Enhance.Result.MAXED;
         }

         ProwessConfig.Step step = this.config.step(level + 1);
         if (step == null) {
            return Enhance.Result.MAXED;
         }

         if (!this.hasMaterial(player, step)) {
            return Enhance.Result.NO_MATERIAL;
         }

         if (step.cost() > 0L && !this.economy.has(player, step.cost())) {
            return Enhance.Result.NO_MONEY;
         }

         if (step.materialAmount() > 0) {
            this.takeMaterial(player, step);
         }

         if (step.cost() > 0L && !this.economy.take(player, step.cost())) {
            return Enhance.Result.NO_MONEY;
         } else if (Math.random() < step.chance()) {
            this.gear.setLevel(item, level + 1, this.config.enhancePerLevel());
            return Enhance.Result.SUCCESS;
         } else if (level > this.config.safeLevel() && this.config.downgradeOnFail()) {
            this.gear.setLevel(item, level - 1, this.config.enhancePerLevel());
            return Enhance.Result.DOWNGRADED;
         } else {
            return Enhance.Result.KEPT;
         }
      } else {
         return Enhance.Result.NOT_GEAR;
      }
   }

   public StatMap gainOfNextLevel(ItemStack item) {
      StatMap raw = this.gear.raw(item);
      StatMap out = StatMap.empty();
      out.addScaled(raw, this.config.enhancePerLevel());
      return out;
   }

   public GearStats gear() {
      return this.gear;
   }

   public ProwessConfig config() {
      return this.config;
   }

   public enum Result {
      SUCCESS,
      KEPT,
      DOWNGRADED,
      MAXED,
      NOT_GEAR,
      NO_MONEY,
      NO_MATERIAL;
   }
}
