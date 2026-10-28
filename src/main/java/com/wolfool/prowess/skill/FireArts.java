package com.wolfool.prowess.skill;

import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class FireArts {
   private static final Color EMBER = Color.fromRGB(255, 138, 24);
   private static final Color CORE = Color.fromRGB(255, 238, 150);
   private static final Color DEEP = Color.fromRGB(168, 26, 8);
   private static final Color AZURE = Color.fromRGB(70, 180, 255);
   private static final Color AZURE_CORE = Color.fromRGB(210, 244, 255);
   private final Plugin plugin;
   private final StatService stats;
   private final FireVfx vfx;
   private static final long CHAIN_WINDOW_MS = 3000L;
   private final ConcurrentHashMap<UUID, long[]> chain = new ConcurrentHashMap<>();

   public FireArts(Plugin plugin, StatService stats) {
      this.plugin = plugin;
      this.stats = stats;
      this.vfx = new FireVfx(plugin);
   }

   public void heatWave(final Player player, Skill skill) {
      final int step = this.nextStep(player);
      final World w = world(player);
      Location eye = player.getEyeLocation();
      final Vector dir = eye.getDirection();
      final Location at = eye.clone().subtract(0.0, 0.35, 0.0).add(dir.clone().multiply(1.5));
      float size = 0.85F + step * 0.28F;

      float roll = switch (step) {
         case 0 -> 25.0F;
         case 1 -> -25.0F;
         default -> 0.0F;
      };
      this.vfx.slash(at, roll, size, 1);
      if (step == 2) {
         this.later(() -> this.vfx.slash(at, 90.0F, size, 1), 2L);
      }

      float pitch = 0.85F + step * 0.18F;
      w.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.1F, pitch);
      w.playSound(at, Sound.ITEM_FIRECHARGE_USE, 0.9F, 0.7F + step * 0.15F);
      if (step == 2) {
         w.playSound(at, Sound.ENTITY_BLAZE_SHOOT, 1.2F, 0.75F);
      }

      final double range = skill.range() * (1.0 + step * 0.18);
      final double power = skill.power() * (1.0 + step * 0.35);
      final Set<UUID> hit = new HashSet<>();
      (new BukkitRunnable() {
         int t = 0;

         public void run() {
            if (++this.t <= 13 && player.isOnline()) {
               double d = this.t * (range / 13.0);
               Location p = at.clone().add(dir.clone().multiply(d));
               w.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 6, 0.35, 0.35, 0.35, 0.0, Fx.trans(FireArts.CORE, FireArts.DEEP, 1.5F));
               w.spawnParticle(Particle.FLAME, p, 3, 0.3, 0.3, 0.3, 0.01);
               if (this.t % 3 == 0) {
                  w.spawnParticle(Particle.LAVA, p, 1, 0.2, 0.2, 0.2, 0.0);
               }

               double reach = 1.5 + step * 0.35;

               for (LivingEntity e : FireArts.this.around(player, p, reach)) {
                  if (hit.add(e.getUniqueId())) {
                     FireArts.this.hurt(player, e, power);
                     FireArts.burn(e, 60 + step * 20);
                     w.spawnParticle(Particle.LAVA, e.getLocation().add(0.0, 1.0, 0.0), 8, 0.3, 0.5, 0.3, 0.0);
                     w.playSound(e.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8F, 1.4F);
                  }
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      player.sendActionBar(Msg.of(step == 2 ? "&6&l열파 &c✦✦✦" : "&6열파 &7" + "✦".repeat(step + 1)));
   }

   private int nextStep(Player player) {
      long now = System.currentTimeMillis();
      long[] s = this.chain
         .compute(player.getUniqueId(), (k, old) -> old != null && now - old[1] <= 3000L ? new long[]{(old[0] + 1L) % 3L, now} : new long[]{0L, now});
      return (int)s[0];
   }

   public void brand(Player player, Skill skill) {
      final World w = world(player);
      Location eye = player.getEyeLocation();
      final List<LivingEntity> marked = new ArrayList<>();

      for (LivingEntity e : this.around(player, eye.clone().add(eye.getDirection().multiply(skill.range() * 0.5)), skill.range())) {
         marked.add(e);
         this.vfx.rune(e.getLocation(), 0.55F, 3);
         w.playSound(e.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0F, 0.6F);
      }

      if (marked.isEmpty()) {
         player.sendActionBar(Msg.of("&8낙인을 찍을 것이 없습니다"));
      } else {
         w.playSound(eye, Sound.ITEM_FLINTANDSTEEL_USE, 1.2F, 0.55F);
         w.playSound(eye, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9F, 1.5F);
         player.sendActionBar(Msg.of("&c낙인 &f" + marked.size() + "&c — 2초 뒤 터집니다"));
         (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (++this.t > 40) {
                  this.cancel();
               } else {
                  for (LivingEntity e : marked) {
                     if (e.isValid()) {
                        double a = this.t * 0.4;
                        Location p = e.getLocation().add(Math.cos(a) * 0.7, 0.4 + this.t % 20 * 0.06, Math.sin(a) * 0.7);
                        w.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.0, 0.0, 0.0, 0.0, Fx.trans(FireArts.EMBER, FireArts.DEEP, 1.1F));
                     }
                  }
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         this.later(() -> {
            for (LivingEntity ex : marked) {
               if (ex.isValid()) {
                  Location p = ex.getLocation();
                  this.vfx.wave(Fx.onGround(p), 1.2F, 1);
                  this.vfx.pillar(Fx.onGround(p), 0.8F, 1);
                  w.spawnParticle(Particle.EXPLOSION, p.clone().add(0.0, 1.0, 0.0), 1);
                  w.spawnParticle(Particle.FLAME, p.clone().add(0.0, 0.8, 0.0), 45, 0.5, 0.6, 0.5, 0.12);
                  w.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, 0.9F, 1.5F);
                  w.playSound(p, Sound.ITEM_FIRECHARGE_USE, 1.0F, 0.8F);
                  this.hurt(player, ex, skill.power());
                  burn(ex, 100);
               }
            }
         }, 40L);
      }
   }

   public void ascend(final Player player, final Skill skill) {
      final World w = world(player);
      player.setVelocity(player.getVelocity().setY(0.92));
      w.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, 0.7F);
      (new BukkitRunnable() {
         int t = 0;

         public void run() {
            if (++this.t <= 30 && player.isOnline()) {
               Location f = player.getLocation();
               w.spawnParticle(Particle.FLAME, f, 6, 0.3, 0.1, 0.3, 0.02);
               w.spawnParticle(Particle.DUST_COLOR_TRANSITION, f, 4, 0.4, 0.2, 0.4, 0.0, Fx.trans(FireArts.CORE, FireArts.EMBER, 1.3F));
               if (this.t > 6 && player.isOnGround()) {
                  FireArts.this.smash(player, skill);
                  this.cancel();
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void smash(Player player, Skill skill) {
      World w = world(player);
      Location g = Fx.onGround(player.getLocation());
      this.vfx.wave(g, 1.7F, 1);
      this.later(() -> this.vfx.wave(g, 2.6F, 1), 4L);
      this.vfx.rune(g, 1.1F, 2);
      w.playSound(g, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.85F);
      w.playSound(g, Sound.BLOCK_LAVA_EXTINGUISH, 1.2F, 0.6F);
      w.playSound(g, Sound.ITEM_TRIDENT_THUNDER, 0.7F, 1.6F);
      w.spawnParticle(Particle.EXPLOSION_EMITTER, g, 1);

      for (int k = 0; k < 8; k++) {
         double a = (Math.PI * 2) * k / 8.0;
         Vector out = new Vector(Math.cos(a), 0.0, Math.sin(a));
         this.later(() -> {
            for (int d = 1; d <= 6; d++) {
               Location p = g.clone().add(out.clone().multiply(d * 0.8));
               w.spawnParticle(Particle.FLAME, p, 4, 0.15, 0.1, 0.15, 0.03);
               w.spawnParticle(Particle.LAVA, p, 1, 0.1, 0.1, 0.1, 0.0);
            }
         }, k % 2);
      }

      double range = skill.range();

      for (LivingEntity e : this.around(player, g, range)) {
         double dist = e.getLocation().distance(g);
         double k = 1.0 - Math.min(0.6, dist / range * 0.6);
         this.hurt(player, e, skill.power() * k);
         burn(e, 80);
         e.setVelocity(e.getLocation().toVector().subtract(g.toVector()).normalize().multiply(0.55).setY(0.45));
      }

      player.sendActionBar(Msg.of("&6&l업화"));
   }

   public void release(Player player, Skill skill) {
      final World w = world(player);
      final Location aim = this.aimGround(player, skill.range());
      this.vfx.rune(aim, 1.4F, 3);
      this.later(() -> this.vfx.rune(aim, 2.2F, 3), 8L);
      this.later(() -> this.vfx.rune(aim, 3.0F, 3), 16L);
      w.playSound(aim, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.4F, 0.5F);
      w.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0F, 0.6F);
      player.sendActionBar(Msg.of("&b&l극염 해방"));
      (new BukkitRunnable() {
         int t = 0;

         public void run() {
            if (++this.t > 24) {
               this.cancel();
            } else {
               double k = 1.0 - this.t / 24.0;

               for (int i = 0; i < 5; i++) {
                  double a = this.t * 0.5 + i * 1.256;
                  double r = 3.4 * k;
                  Location p = aim.clone().add(Math.cos(a) * r, 0.4 + k * 2.0, Math.sin(a) * r);
                  w.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.0, 0.0, 0.0, 0.0, Fx.trans(FireArts.AZURE_CORE, FireArts.AZURE, 1.4F));
               }

               w.spawnParticle(Particle.SOUL_FIRE_FLAME, aim.clone().add(0.0, 0.2, 0.0), 3, 1.2, 0.1, 1.2, 0.01);
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      this.later(() -> {
         this.vfx.azure(aim.clone().add(0.0, 1.6, 0.0), 0.0F, 2.4F, 1);
         this.later(() -> this.vfx.azure(aim.clone().add(0.0, 1.6, 0.0), 90.0F, 2.4F, 1), 3L);
         this.vfx.pillar(aim, 2.2F, 2);

         for (int k = 0; k < 8; k++) {
            double a = (Math.PI * 2) * k / 8.0;
            Location p = Fx.onGround(aim.clone().add(Math.cos(a) * 2.6, 0.0, Math.sin(a) * 2.6));
            this.later(() -> this.vfx.pillar(p, 1.3F, 2), 2 + k);
         }

         w.playSound(aim, Sound.ENTITY_GENERIC_EXPLODE, 1.6F, 0.6F);
         w.playSound(aim, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.1F, 1.4F);
         w.playSound(aim, Sound.ITEM_TRIDENT_THUNDER, 1.0F, 1.2F);
         w.spawnParticle(Particle.SOUL_FIRE_FLAME, aim.clone().add(0.0, 1.0, 0.0), 80, 2.0, 1.2, 2.0, 0.16);
         w.spawnParticle(Particle.EXPLOSION_EMITTER, aim.clone().add(0.0, 1.0, 0.0), 3, 1.5, 0.5, 1.5, 0.0);

         for (LivingEntity e : this.around(player, aim, skill.range() * 0.8)) {
            this.hurt(player, e, skill.power());
            burn(e, 160);
            e.setVelocity(new Vector(0.0, 0.75, 0.0));
         }
      }, 24L);
      this.later(() -> (new BukkitRunnable() {
         int t = 0;

         public void run() {
            if (++this.t > 60) {
               this.cancel();
            } else {
               for (int i = 0; i < 6; i++) {
                  double a = Math.random() * Math.PI * 2.0;
                  double r = Math.sqrt(Math.random()) * 3.2;
                  Location p = aim.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                  w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.0, 0.05, 0.0, 0.01);
               }

               if (this.t % 10 == 0) {
                  for (LivingEntity e : FireArts.this.around(player, aim, 3.2)) {
                     FireArts.this.hurt(player, e, skill.power() * 0.12);
                     FireArts.burn(e, 40);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L), 24L);
   }

   private Location aimGround(Player player, double range) {
      Block block = player.getTargetBlockExact((int)Math.max(4.0, range));
      Location at = block != null
         ? block.getLocation().add(0.5, 1.0, 0.5)
         : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
      at.setYaw(player.getLocation().getYaw());
      return Fx.onGround(at);
   }

   private static void burn(LivingEntity e, int ticks) {
      if (e.getFireTicks() < ticks) {
         e.setFireTicks(ticks);
      }
   }

   private List<LivingEntity> around(Player player, Location at, double range) {
      List<LivingEntity> out = new ArrayList<>();
      if (at.getWorld() == null) {
         return out;
      }

      for (Entity e : at.getWorld().getNearbyEntities(at, range, range, range)) {
         if (e instanceof LivingEntity le && !e.equals(player) && le.isValid()) {
            out.add(le);
         }
      }

      return out;
   }

   private void hurt(Player player, LivingEntity target, double power) {
      double base = Math.max(1.0, this.stats.get(player, Stat.ATTACK) * 0.5);
      target.setNoDamageTicks(0);
      target.damage(base * power, player);
   }

   private void later(Runnable r, long delay) {
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, r, Math.max(0L, delay));
   }

   private static World world(Player p) {
      return p.getWorld();
   }
}
