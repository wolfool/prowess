package com.wolfool.prowess.combat;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.party.PartyService;
import com.wolfool.prowess.skill.SkillService;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.stat.StatService;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class DamageListener implements Listener {
   private final StatService stats;
   private final ProwessConfig config;
   private final SkillService skills;
   private PartyService parties;

   public DamageListener(StatService stats, ProwessConfig config, SkillService skills) {
      this.stats = stats;
      this.config = config;
      this.skills = skills;
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onDamage(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof LivingEntity victim) {
         Player var14 = this.attackerOf(event.getDamager());
         Player defender = victim instanceof Player p ? p : null;
         if (var14 != null || defender != null) {
            if (this.parties != null
               && !this.config.partyFriendlyFire()
               && var14 != null
               && defender != null
               && this.parties.together(var14.getUniqueId(), defender.getUniqueId())) {
               event.setCancelled(true);
            } else {
               double damage = event.getDamage(DamageModifier.BASE);
               if (!(damage <= 0.0)) {
                  if (RawDamage.isRaw(victim)) {
                     this.zero(event, DamageModifier.ARMOR);
                     this.zero(event, DamageModifier.MAGIC);
                     this.zero(event, DamageModifier.RESISTANCE);
                  } else {
                     StatMap atk = var14 == null ? StatMap.empty() : this.stats.of(var14);
                     StatMap def = defender == null ? StatMap.empty() : this.stats.of(defender);
                     damage += atk.get(Stat.ATTACK);
                     boolean crit = var14 != null && this.skills.consumeGuaranteedCrit(var14);
                     if (!crit) {
                        crit = Formula.roll(atk.get(Stat.CRIT_CHANCE));
                     }

                     if (crit) {
                        damage = Formula.withCrit(damage, atk.get(Stat.CRIT_DAMAGE));
                     }

                     double defense = defender != null ? def.get(Stat.DEFENSE) : this.mobDefense(victim);
                     damage = Formula.afterDefense(damage, defense, atk.get(Stat.PIERCE), this.config.defenseK());
                     if (var14 != null && defender != null) {
                        damage *= this.config.pvpFactor();
                     }

                     damage = Math.max(0.5, damage);
                     this.zero(event, DamageModifier.ARMOR);
                     this.zero(event, DamageModifier.MAGIC);
                     this.zero(event, DamageModifier.RESISTANCE);
                     event.setDamage(DamageModifier.BASE, damage);
                     this.mark(victim, crit, atk.get(Stat.PIERCE) >= 30.0 && defense > 0.0);
                     if (crit && var14 != null) {
                        var14.playSound(var14.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8F, 1.4F);
                        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0.0, victim.getHeight() * 0.6, 0.0), 12, 0.3, 0.3, 0.3, 0.1);
                     }

                     if (var14 != null) {
                        double steal = atk.get(Stat.LIFESTEAL);
                        if (steal > 0.0) {
                           this.heal(var14, damage * steal / 100.0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void mark(LivingEntity victim, boolean crit, boolean pierced) {
      if (crit) {
         this.put(victim, "prowess_crit");
      }

      if (pierced) {
         this.put(victim, "prowess_pierce");
      }
   }

   private void put(LivingEntity victim, String key) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("Prowess");
      if (plugin != null) {
         victim.setMetadata(key, new FixedMetadataValue(plugin, true));
         Bukkit.getScheduler().runTask(plugin, () -> victim.removeMetadata(key, plugin));
      }
   }

   private double mobDefense(LivingEntity victim) {
      AttributeInstance armor = victim.getAttribute(Attribute.ARMOR);
      return armor == null ? 0.0 : armor.getValue() * this.config.mobDefensePerArmor();
   }

   @Nullable
   private Player attackerOf(Entity damager) {
      if (damager instanceof Player p) {
         return p;
      } else {
         return damager instanceof Projectile pr && pr.getShooter() instanceof Player p ? p : null;
      }
   }

   private void heal(Player who, double amount) {
      if (!(amount <= 0.0)) {
         AttributeInstance max = who.getAttribute(Attribute.MAX_HEALTH);
         double cap = max == null ? 20.0 : max.getValue();
         who.setHealth(Math.min(cap, who.getHealth() + amount));
      }
   }

   private void zero(EntityDamageEvent event, DamageModifier mod) {
      if (event.isApplicable(mod)) {
         event.setDamage(mod, 0.0);
      }
   }

   public void useParties(PartyService value) {
      this.parties = value;
   }
}
