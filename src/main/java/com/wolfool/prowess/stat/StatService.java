package com.wolfool.prowess.stat;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.accessory.Accessories;
import com.wolfool.prowess.gear.GearStats;
import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.integration.EpithetHook;
import com.wolfool.prowess.integration.MasteryHook;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class StatService {
   private final Plugin plugin;
   private final ProwessConfig config;
   private final GearStats gear;
   private final Accessories accessories;
   private final MasteryHook mastery;
   private final EpithetHook epithet;
   private final Map<UUID, StatMap> cache = new ConcurrentHashMap<>();

   public StatService(Plugin plugin, ProwessConfig config, GearStats gear, Accessories accessories, MasteryHook mastery, EpithetHook epithet) {
      this.plugin = plugin;
      this.config = config;
      this.gear = gear;
      this.accessories = accessories;
      this.mastery = mastery;
      this.epithet = epithet;
   }

   public void invalidate(Player player) {
      this.cache.remove(player.getUniqueId());
   }

   public void invalidateAll() {
      this.cache.clear();
   }

   public StatMap of(Player player) {
      return this.cache.computeIfAbsent(player.getUniqueId(), id -> this.compute(player));
   }

   public double get(Player player, Stat stat) {
      return this.of(player).get(stat);
   }

   private StatMap compute(Player player) {
      StatMap out = StatMap.base();

      for (ItemStack piece : player.getInventory().getArmorContents()) {
         out.addAll(this.statsOf(piece));
      }

      out.addAll(this.statsOf(player.getInventory().getItemInMainHand()));
      out.addAll(this.statsOf(player.getInventory().getItemInOffHand()));

      for (ItemStack worn : this.accessories.worn(player)) {
         out.addAll(this.statsOf(worn));
      }

      AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
      if (armor != null) {
         out.add(Stat.DEFENSE, armor.getValue() * this.config.defensePerArmorPoint());
      }

      AttributeInstance tough = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
      if (tough != null) {
         out.add(Stat.DEFENSE, tough.getValue() * this.config.defensePerToughness());
      }

      this.mastery.contribute(player, out, this.config);
      this.epithet.contribute(player, out, this.config);
      return out;
   }

   private StatMap statsOf(ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         StatMap own = this.gear.effective(item, this.config.enhancePerLevel());
         if (!own.isEmpty()) {
            return own;
         }

         ProwessConfig.GearEntry entry = this.config.entryFor(CraftEngineHook.idOf(item), item.getType());
         return entry == null ? StatMap.empty() : entry.stats();
      } else {
         return StatMap.empty();
      }
   }

   public void applyVanilla(Player player) {
      StatMap stats = this.of(player);
      this.set(player, Attribute.MAX_HEALTH, "prowess_health", stats.get(Stat.HEALTH), false);
      this.set(player, Attribute.MOVEMENT_SPEED, "prowess_speed", stats.get(Stat.MOVE_SPEED) / 100.0, true);
      this.set(player, Attribute.ATTACK_SPEED, "prowess_atkspeed", stats.get(Stat.ATTACK_SPEED) / 100.0, true);
      AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
      if (max != null && player.getHealth() > max.getValue()) {
         player.setHealth(max.getValue());
      }
   }

   private void set(LivingEntity who, Attribute attribute, String id, double amount, boolean multiply) {
      AttributeInstance inst = who.getAttribute(attribute);
      if (inst != null) {
         NamespacedKey key = new NamespacedKey(this.plugin, id);
         inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
         if (amount != 0.0) {
            inst.addModifier(new AttributeModifier(key, amount, multiply ? Operation.MULTIPLY_SCALAR_1 : Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
         }
      }
   }

   public void refresh(Player player) {
      this.invalidate(player);
      this.applyVanilla(player);
   }

   public void refreshLater(Player player) {
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
         if (player.isOnline()) {
            this.refresh(player);
         }
      });
   }

   public GearStats gear() {
      return this.gear;
   }

   public ProwessConfig config() {
      return this.config;
   }

   public Accessories accessories() {
      return this.accessories;
   }

   public ItemStack held(Player player, EquipmentSlot slot) {
      return slot == EquipmentSlot.OFF_HAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
   }
}
