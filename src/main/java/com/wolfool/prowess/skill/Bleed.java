package com.wolfool.prowess.skill;

import com.wolfool.prowess.combat.RawDamage;
import com.wolfool.prowess.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Bleed {
   public static final int MAX_STACKS = 5;
   private static final int DAMAGE_EVERY = 5;
   private final Plugin plugin;
   private final Map<UUID, Bleed.Wound> wounds = new HashMap<>();
   private boolean running;
   private int tick;

   public Bleed(Plugin plugin) {
      this.plugin = plugin;
   }

   public int stacks(LivingEntity target) {
      Bleed.Wound w = this.wounds.get(target.getUniqueId());
      return w == null ? 0 : w.stacks;
   }

   public void apply(Player by, LivingEntity target, double damagePerSecond, double seconds) {
      Bleed.Wound w = this.wounds.computeIfAbsent(target.getUniqueId(), id -> new Bleed.Wound());
      w.stacks = Math.min(5, w.stacks + 1);
      w.ticksLeft = (int)(seconds * 20.0);
      w.by = by.getUniqueId();
      w.perTick = damagePerSecond / 5.0;
      this.showTag(target, w);
      this.start();
      target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.4F, 1.6F);
   }

   public double detonate(Player by, LivingEntity target, double multiplier) {
      Bleed.Wound w = this.wounds.remove(target.getUniqueId());
      if (w != null && w.stacks > 0) {
         removeTag(w);
         double burst = w.perTick * 5.0 * w.stacks * (w.ticksLeft / 20.0) * multiplier;
         if (burst <= 0.0) {
            return 0.0;
         }

         RawDamage.deal(target, burst, by);
         Location at = target.getLocation().add(0.0, 1.0, 0.0);
         target.getWorld().spawnParticle(Particle.DUST, at, 50, 0.4, 0.6, 0.4, 0.0, new DustOptions(Color.fromRGB(180, 10, 20), 1.6F));
         target.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.6F, 1.7F);
         return burst;
      } else {
         if (w != null) {
            removeTag(w);
         }

         return 0.0;
      }
   }

   private void start() {
      if (!this.running) {
         this.running = true;
         this.tick = 0;
         (new BukkitRunnable() {
               public void run() {
                  if (Bleed.this.wounds.isEmpty()) {
                     Bleed.this.running = false;
                     this.cancel();
                  } else {
                     boolean hurt = ++Bleed.this.tick % 5 == 0;
                     Bleed.this.wounds
                        .entrySet()
                        .removeIf(
                           e -> {
                              Bleed.Wound w = e.getValue();
                              if (Bukkit.getEntity(e.getKey()) instanceof LivingEntity t && !t.isDead()) {
                                 if (hurt) {
                                    w.ticksLeft -= 5;
                                    if (w.ticksLeft <= 0) {
                                       Bleed.removeTag(w);
                                       return true;
                                    }

                                    Player by = Bukkit.getPlayer(w.by);
                                    if (by == null) {
                                       Bleed.removeTag(w);
                                       return true;
                                    }

                                    RawDamage.deal(t, w.perTick * w.stacks, by);
                                    t.getWorld()
                                       .spawnParticle(
                                          Particle.DUST,
                                          t.getLocation().add(0.0, t.getHeight() * 0.5, 0.0),
                                          2 + w.stacks * 2,
                                          0.25,
                                          0.3,
                                          0.25,
                                          0.0,
                                          new DustOptions(Color.fromRGB(160, 0, 20), 1.0F)
                                       );
                                 }

                                 Bleed.this.follow(t, w);
                                 return false;
                              } else {
                                 Bleed.removeTag(w);
                                 return true;
                              }
                           }
                        );
                  }
               }
            })
            .runTaskTimer(this.plugin, 1L, 1L);
      }
   }

   private void showTag(LivingEntity target, Bleed.Wound w) {
      if (w.tag == null || !w.tag.isValid()) {
         w.tag = (TextDisplay)target.getWorld().spawn(above(target), TextDisplay.class, d -> {
            d.setBillboard(Billboard.CENTER);
            d.setPersistent(false);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(false);
            d.setSeeThrough(true);
            d.setViewRange(0.4F);
            d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.8F, 0.8F, 0.8F), new Quaternionf()));
         });
         w.shownStacks = -1;
      }

      if (w.shownStacks != w.stacks) {
         w.tag.text(Msg.of(bar(w.stacks)));
         w.shownStacks = w.stacks;
      }
   }

   private static String bar(int n) {
      StringBuilder sb = new StringBuilder("&4&l출혈 &r");

      for (int i = 0; i < 5; i++) {
         sb.append(i < n ? "&c❚" : "&8❚");
      }

      return sb.toString();
   }

   private void follow(LivingEntity target, Bleed.Wound w) {
      if (w.tag != null && w.tag.isValid()) {
         if (w.shownStacks != w.stacks) {
            w.tag.text(Msg.of(bar(w.stacks)));
            w.shownStacks = w.stacks;
         }

         w.tag.teleport(above(target));
      } else {
         this.showTag(target, w);
      }
   }

   private static Location above(LivingEntity target) {
      double up = target.getHeight() + (target instanceof Player ? 0.85 : 0.5);
      return target.getLocation().add(0.0, up, 0.0);
   }

   private static void removeTag(Bleed.Wound w) {
      if (w.tag != null) {
         w.tag.remove();
         w.tag = null;
      }
   }

   public void clear() {
      this.wounds.values().forEach(Bleed::removeTag);
      this.wounds.clear();
   }

   private static final class Wound {
      int stacks;
      int ticksLeft;
      UUID by;
      double perTick;
      TextDisplay tag;
      int shownStacks = -1;
   }
}
