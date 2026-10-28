package com.wolfool.prowess.skill;

import com.wolfool.prowess.integration.MythicSkills;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class SkillService {
   private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
   private final Plugin plugin;
   private final StatService stats;
   private final Bleed bleed;
   private final Vfx vfx;
   private final FireArts fire;
   private final MythicSkills mythic;
   private final Set<UUID> guaranteedCrit = new HashSet<>();
   private final Map<UUID, Long> lastCast = new HashMap<>();
   private static final Color BLOOD = Color.fromRGB(170, 12, 26);
   private static final Color BLOOD_LIGHT = Color.fromRGB(235, 60, 70);
   private static final Color SHADOW = Color.fromRGB(24, 18, 30);
   private static final Color SHADOW_LIGHT = Color.fromRGB(120, 70, 170);

   public SkillService(Plugin plugin, StatService stats) {
      this.plugin = plugin;
      this.stats = stats;
      this.bleed = new Bleed(plugin);
      this.vfx = new Vfx(plugin);
      this.mythic = new MythicSkills(plugin);
      this.fire = new FireArts(plugin, stats);
   }

   public Bleed bleed() {
      return this.bleed;
   }

   public MythicSkills mythic() {
      return this.mythic;
   }

   public boolean recentlyCast(Player player) {
      Long at = this.lastCast.get(player.getUniqueId());
      return at != null && System.currentTimeMillis() - at < 1200L;
   }

   public boolean consumeGuaranteedCrit(Player player) {
      return this.guaranteedCrit.remove(player.getUniqueId());
   }

   public double remaining(Player player, Skill skill) {
      Map<String, Long> mine = this.cooldowns.get(player.getUniqueId());
      if (mine == null) {
         return 0.0;
      }

      Long until = mine.get(skill.id());
      if (until == null) {
         return 0.0;
      }

      long left = until - System.currentTimeMillis();
      return left <= 0L ? 0.0 : left / 1000.0;
   }

   public void clear(Player player) {
      this.cooldowns.remove(player.getUniqueId());
      this.guaranteedCrit.remove(player.getUniqueId());
      this.lastCast.remove(player.getUniqueId());
   }

   public boolean use(Player player, Skill skill) {
      double left = this.remaining(player, skill);
      if (left > 0.0) {
         player.sendActionBar(Msg.of("&8" + skill.name() + " &7" + String.format("%.1f", left) + "초"));
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4F, 0.7F);
         return false;
      }

      this.cooldowns
         .computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
         .put(skill.id(), System.currentTimeMillis() + (long)(skill.cooldown() * 1000.0));
      switch (skill.type()) {
         case CLEAVE:
            this.cleave(player, skill);
            break;
         case WHIRL:
            this.whirl(player, skill);
            break;
         case DASH:
            this.dash(player, skill);
            break;
         case LEAP:
            this.leap(player, skill);
            break;
         case EMPOWER:
            this.empower(player, skill);
            break;
         case PIERCE:
            this.pierce(player, skill);
            break;
         case NOVA:
            this.nova(player, skill);
            break;
         case METEOR:
            this.meteor(player, skill);
            break;
         case CHAIN:
            this.chain(player, skill);
            break;
         case VORTEX:
            this.vortex(player, skill);
            break;
         case FISSURE:
            this.fissure(player, skill);
            break;
         case EXECUTE:
            this.execute(player, skill);
            break;
         case GUARD:
            this.guard(player, skill);
            break;
         case UPPERCUT:
            this.uppercut(player, skill);
            break;
         case BARRAGE:
            this.barrage(player, skill);
            break;
         case BLOOD_REND:
            this.bloodRend(player, skill);
            break;
         case SHADOW_STEP:
            this.shadowStep(player, skill);
            break;
         case BLOOD_BRAND:
            this.bloodBrand(player, skill);
            break;
         case CRIMSON_WALTZ:
            this.crimsonWaltz(player, skill);
            break;
         case HEAT_WAVE:
            this.fire.heatWave(player, skill);
            break;
         case FIRE_BRAND:
            this.fire.brand(player, skill);
            break;
         case ASCEND:
            this.fire.ascend(player, skill);
            break;
         case BLAZE_RELEASE:
            this.fire.release(player, skill);
            break;
         case MYTHIC:
            if (!this.mythic.cast(player, skill.mythicSkill())) {
               player.sendActionBar(Msg.of("&8스킬이 나가지 않았습니다"));
            } else if (skill.power() > 0.0) {
               this.mythicBonus(player, skill);
            }
      }

      this.lastCast.put(player.getUniqueId(), System.currentTimeMillis());
      player.sendActionBar(Msg.of(skill.type().color() + "✦ " + skill.name()));
      return true;
   }

   private void cleave(final Player player, final Skill skill) {
      this.vfx
         .slash(
            player.getEyeLocation().subtract(0.0, 0.35, 0.0).add(player.getLocation().getDirection().multiply(1.6)), 18.0F, 1.9F, Color.fromRGB(190, 235, 255)
         );
      final World w = this.world(player);
      final Location start = player.getEyeLocation().subtract(0.0, 0.3, 0.0);
      final Vector dir = start.getDirection().setY(0).normalize();
      final Color core = Color.fromRGB(255, 240, 200);
      final Color edge = Color.fromRGB(120, 200, 255);
      Fx.chord(w, start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.ITEM_TRIDENT_RIPTIDE_1, Sound.ENTITY_ARROW_SHOOT);
      final Set<UUID> hit = new HashSet<>();
      (new BukkitRunnable() {
         int step = 0;

         public void run() {
            if (++this.step <= 14 && player.isOnline()) {
               double dist = this.step * 0.75;
               Location at = start.clone().add(dir.clone().multiply(dist));
               double width = 1.1 + this.step * 0.14;
               Fx.slashArc(w, at, dir, width, 0.35, Fx.dust(core, 1.6F));
               Fx.slashArc(w, at.clone().add(0.0, 0.1, 0.0), dir, width * 0.9, 0.3, Fx.dust(edge, 1.0F));
               w.spawnParticle(Particle.CRIT, at, 6, width * 0.4, 0.3, width * 0.4, 0.05);
               if (this.step % 3 == 0) {
                  w.playSound(at, Sound.ENTITY_PHANTOM_FLAP, 0.4F, 1.8F);
               }

               for (LivingEntity e : SkillService.this.around(player, at, width + 1.0)) {
                  if (hit.add(e.getUniqueId())) {
                     SkillService.this.hurt(player, e, skill.power());
                     Fx.ringTilted(w, e.getLocation().add(0.0, 1.0, 0.0), 1.0, 24, dir, Fx.dust(edge, 1.3F));
                     w.spawnParticle(Particle.DAMAGE_INDICATOR, e.getLocation().add(0.0, 1.0, 0.0), 6, 0.3, 0.3, 0.3, 0.0);
                  }
               }

               if (dist > skill.range()) {
                  this.cancel();
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void whirl(final Player player, final Skill skill) {
      this.vfx.combo(player.getLocation().add(0.0, 1.0, 0.0), new float[]{0.0F, 120.0F, 240.0F}, 4, 2.3F, Color.fromRGB(255, 228, 150));
      final World w = this.world(player);
      final Color core = Color.fromRGB(255, 230, 160);
      (new BukkitRunnable() {
         int tick = 0;
         int turn = 0;

         public void run() {
            if (player.isOnline() && this.turn < 3) {
               Location at = player.getLocation().add(0.0, 0.9, 0.0);
               double r = skill.range() * (0.65 + this.turn * 0.14);

               for (int k = 0; k < 6; k++) {
                  double a = this.tick * 0.55 + k * 0.12;
                  Location p = at.clone().add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
                  w.spawnParticle(Particle.DUST, p, 1, 0.0, 0.0, 0.0, 0.0, Fx.dust(core, 1.5F - k * 0.15F));
                  if (k == 0) {
                     w.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0.0, 0.0, 0.0, 0.0);
                  }
               }

               this.tick++;
               if (this.tick % 12 == 0) {
                  this.turn++;
                  Fx.ring(w, at, r, 46, Fx.dust(core, 1.4F));

                  for (LivingEntity e : SkillService.this.around(player, player.getLocation(), r)) {
                     SkillService.this.hurt(player, e, skill.power() * (this.turn == 3 ? 1.6 : 0.6));
                  }

                  w.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F + this.turn * 0.25F);
                  if (this.turn == 3) {
                     Fx.sphere(w, at, r * 0.8, 80, Fx.dust(core, 1.3F));
                     Fx.chord(w, at, Sound.ENTITY_PLAYER_ATTACK_CRIT, Sound.ITEM_TRIDENT_RETURN, null);
                  }
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void dash(final Player player, final Skill skill) {
      final Vector push = player.getEyeLocation().getDirection().setY(0.12).normalize().multiply(Math.max(1.2, skill.range() * 0.4));
      player.setVelocity(push);
      this.world(player).playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0F, 1.2F);
      this.world(player).playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.9F, 1.4F);
      final Set<UUID> hit = new HashSet<>();
      (new BukkitRunnable() {
         int ticks = 0;
         Location last = player.getLocation().clone();

         public void run() {
            if (++this.ticks <= 14 && player.isOnline()) {
               Location now = player.getLocation();

               for (double f = 0.0; f <= 1.0; f += 0.25) {
                  Location p = this.last.clone().add(now.clone().subtract(this.last).toVector().multiply(f)).add(0.0, 1.0, 0.0);
                  SkillService.this.world(player).spawnParticle(Particle.DUST, p, 2, 0.2, 0.4, 0.2, 0.0, new DustOptions(Color.fromRGB(255, 240, 200), 1.2F));
               }

               this.last = now.clone();

               for (LivingEntity t : SkillService.this.around(player, now, 2.6)) {
                  if (hit.add(t.getUniqueId())) {
                     SkillService.this.hurt(player, t, skill.power());
                     t.setVelocity(push.clone().setY(0.35).multiply(0.5));
                     SkillService.this.world(player).spawnParticle(Particle.CRIT, t.getLocation().add(0.0, 1.0, 0.0), 12, 0.3, 0.3, 0.3, 0.1);
                     SkillService.this.world(player).playSound(t.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0F, 1.1F);
                  }
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private void leap(final Player player, final Skill skill) {
      player.setVelocity(player.getVelocity().setY(0.95).add(player.getEyeLocation().getDirection().setY(0).normalize().multiply(0.3)));
      this.world(player).playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.7F, 1.7F);
      (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            this.ticks++;
            if (!player.isOnline()) {
               this.cancel();
            } else {
               SkillService.this.world(player).spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 0.2, 0.0), 6, 0.4, 0.2, 0.4, 0.4);
               if (this.ticks > 9 && (player.isOnGround() || this.ticks > 70)) {
                  SkillService.this.slam(player, skill);
                  this.cancel();
               }
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private void slam(final Player player, final Skill skill) {
      final World w = this.world(player);
      Location at = player.getLocation();
      final Location ground = Fx.onGround(at);
      final BlockData block = at.clone().subtract(0.0, 1.0, 0.0).getBlock().getBlockData();
      final Color dustColor = Color.fromRGB(190, 170, 140);
      w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
      w.spawnParticle(Particle.BLOCK, at, 90, 1.8, 0.3, 1.8, 0.25, block);
      Fx.chord(w, at, Sound.ENTITY_GENERIC_EXPLODE, Sound.BLOCK_ANVIL_LAND, Sound.ITEM_TRIDENT_THUNDER);
      Fx.pillar(w, ground, 2.0, 0.9, Fx.dust(dustColor, 1.6F));
      (new BukkitRunnable() {
         double r = 0.6;

         public void run() {
            if (!(this.r > skill.range()) && player.isOnline()) {
               int points = (int)(this.r * 16.0);
               Fx.ring(w, ground, this.r, points, Fx.dust(dustColor, 1.5F));

               for (int i = 0; i < points / 2; i++) {
                  double a = (Math.PI * 2) * i / (points / 2.0);
                  w.spawnParticle(Particle.BLOCK, ground.clone().add(Math.cos(a) * this.r, 0.1, Math.sin(a) * this.r), 3, 0.12, 0.06, 0.12, 0.0, block);
               }

               w.playSound(ground, Sound.BLOCK_STONE_BREAK, 0.35F, 0.6F);
               this.r += 0.85;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);

      for (LivingEntity e : this.around(player, at, skill.range())) {
         this.hurt(player, e, skill.power());
         e.setVelocity(e.getLocation().toVector().subtract(at.toVector()).normalize().multiply(0.5).setY(0.55));
      }
   }

   private void empower(final Player player, Skill skill) {
      final int ticks = (int)(skill.duration() * 20.0);
      int amp = (int)Math.max(0L, Math.round(skill.power()) - 1L);
      player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, amp, true, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 1, true, false));
      this.world(player).playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.3F);
      this.world(player).playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5F, 1.6F);
      (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (this.t++ <= ticks && player.isOnline()) {
                  Location at = player.getLocation();
                  double a = this.t * 0.35;

                  for (int k = 0; k < 2; k++) {
                     double ang = a + Math.PI * k;
                     SkillService.this.world(player)
                        .spawnParticle(
                           Particle.DUST,
                           at.clone().add(Math.cos(ang) * 0.8, 0.2 + this.t % 20 * 0.08, Math.sin(ang) * 0.8),
                           1,
                           0.0,
                           0.0,
                           0.0,
                           0.0,
                           new DustOptions(Color.fromRGB(255, 120, 220), 1.1F)
                        );
                     SkillService.this.world(player)
                        .spawnParticle(
                           Particle.DUST,
                           at.clone().add(Math.cos(-ang) * 1.0, 1.6 - this.t % 20 * 0.06, Math.sin(-ang) * 1.0),
                           1,
                           0.0,
                           0.0,
                           0.0,
                           0.0,
                           new DustOptions(Color.fromRGB(160, 80, 255), 1.0F)
                        );
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void pierce(final Player player, final Skill skill) {
      this.vfx
         .thrust(
            player.getEyeLocation().subtract(0.0, 0.4, 0.0).add(player.getLocation().getDirection().multiply(1.4)), 0.0F, 2.0F, Color.fromRGB(255, 250, 220)
         );
      final Location eye = player.getEyeLocation();
      final Vector step = eye.getDirection().normalize().multiply(0.6);
      this.world(player).playSound(eye, Sound.ITEM_TRIDENT_THROW, 1.0F, 1.3F);
      (new BukkitRunnable() {
         Location at = eye.clone();
         int i = 0;
         final Set<UUID> hit = new HashSet<>();

         public void run() {
            for (int k = 0; k < 3; k++) {
               if (this.i++ > skill.range() * 1.7 || this.at.getBlock().getType().isSolid()) {
                  this.cancel();
                  return;
               }

               this.at.add(step);
               SkillService.this.world(player).spawnParticle(Particle.END_ROD, this.at, 2, 0.04, 0.04, 0.04, 0.0);
               SkillService.this.world(player).spawnParticle(Particle.CRIT, this.at, 1, 0.1, 0.1, 0.1, 0.0);

               for (LivingEntity t : SkillService.this.around(player, this.at, 1.2)) {
                  if (this.hit.add(t.getUniqueId())) {
                     SkillService.this.hurt(player, t, skill.power());
                     SkillService.this.world(player).spawnParticle(Particle.ENCHANTED_HIT, t.getLocation().add(0.0, 1.0, 0.0), 15, 0.2, 0.3, 0.2, 0.3);
                  }
               }
            }

            if (!player.isOnline()) {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void nova(final Player player, final Skill skill) {
      this.vfx.shock(player.getLocation(), 3.2F, Color.fromRGB(255, 210, 120));
      final World w = this.world(player);
      Location at = player.getLocation();
      final Location ground = Fx.onGround(at);
      final Color ice = Color.fromRGB(150, 230, 255);
      final Color deep = Color.fromRGB(60, 130, 220);
      Fx.chord(w, at, Sound.BLOCK_GLASS_BREAK, Sound.ENTITY_PLAYER_HURT_FREEZE, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
      Fx.rune(w, ground, skill.range() * 0.8, 0.0, Fx.dust(deep, 1.5F), Fx.dust(ice, 1.0F));
      (new BukkitRunnable() {
         double r = 0.6;

         public void run() {
            if (!(this.r > skill.range()) && player.isOnline()) {
               int points = (int)(this.r * 14.0);
               Fx.ring(w, ground, this.r, points, Fx.dust(ice, 1.6F));

               for (int i = 0; i < 8; i++) {
                  double a = (Math.PI * 2) * i / 8.0 + this.r;
                  Location p = ground.clone().add(Math.cos(a) * this.r, 0.0, Math.sin(a) * this.r);
                  Fx.pillar(w, p, 1.2, 0.18, Fx.dust(deep, 1.3F));
                  w.spawnParticle(Particle.SNOWFLAKE, p, 4, 0.1, 0.5, 0.1, 0.01);
               }

               w.playSound(ground, Sound.BLOCK_GLASS_BREAK, 0.35F, 1.6F);
               this.r += 0.7;
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);

      for (LivingEntity e : this.around(player, at, skill.range())) {
         this.hurt(player, e, skill.power());
         int ticks = (int)(skill.duration() * 20.0);
         e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 2, true, true));
         e.setFreezeTicks(Math.min(e.getMaxFreezeTicks(), ticks));
         Fx.sphere(w, e.getLocation().add(0.0, 1.0, 0.0), 1.0, 40, Fx.dust(ice, 1.2F));
         w.spawnParticle(Particle.SNOWFLAKE, e.getLocation().add(0.0, 1.0, 0.0), 30, 0.3, 0.6, 0.3, 0.05);
      }
   }

   private void meteor(final Player player, final Skill skill) {
      this.vfx
         .rune(
            player.getTargetBlockExact(40) != null ? player.getTargetBlockExact(40).getLocation().add(0.5, 1.02, 0.5) : player.getLocation(),
            3.4F,
            Color.fromRGB(255, 140, 60)
         );
      final World w = this.world(player);
      Block target = player.getTargetBlockExact((int)Math.max(6.0, skill.range() * 3.0));
      final Location ground = target != null
         ? target.getLocation().add(0.5, 1.0, 0.5)
         : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(skill.range() * 2.0));
      final Location sky = ground.clone().add(0.0, 20.0, 0.0);
      final Color fire = Color.fromRGB(255, 140, 40);
      final Color ember = Color.fromRGB(255, 60, 20);
      Fx.chord(w, player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, Sound.ITEM_FIRECHARGE_USE, Sound.ENTITY_EVOKER_PREPARE_SUMMON);
      (new BukkitRunnable() {
         Location at = sky.clone();
         int t = 0;

         public void run() {
            if (!player.isOnline()) {
               this.cancel();
            } else {
               this.t++;
               double warn = skill.range() * (1.0 - Math.min(0.4, this.t * 0.02));
               Fx.rune(w, Fx.onGround(ground), warn, this.t * 0.25, Fx.dust(ember, 1.5F), Fx.dust(fire, 1.0F));

               for (int k = 0; k < 3; k++) {
                  this.at.subtract(0.0, 0.55, 0.0);
                  Fx.sphere(w, this.at, 0.8, 24, Fx.dust(fire, 1.8F));
                  w.spawnParticle(Particle.FLAME, this.at, 12, 0.4, 0.4, 0.4, 0.03);
                  w.spawnParticle(Particle.LARGE_SMOKE, this.at.clone().add(0.0, 1.0, 0.0), 6, 0.35, 0.35, 0.35, 0.01);
                  w.spawnParticle(Particle.LAVA, this.at, 2, 0.3, 0.3, 0.3, 0.0);
               }

               if (this.t % 4 == 0) {
                  w.playSound(this.at, Sound.ENTITY_BLAZE_BURN, 0.7F, 0.7F);
               }

               if (this.at.getY() <= ground.getY() + 0.6) {
                  w.spawnParticle(Particle.EXPLOSION_EMITTER, ground, 3, 0.8, 0.3, 0.8, 0.0);
                  Fx.chord(w, ground, Sound.ENTITY_GENERIC_EXPLODE, Sound.ITEM_FIRECHARGE_USE, Sound.ITEM_TRIDENT_THUNDER);
                  Fx.sphere(w, ground.clone().add(0.0, 1.0, 0.0), 2.4, 100, Fx.dust(fire, 2.0F));
                  Fx.pillar(w, Fx.onGround(ground), 4.0, 1.0, Fx.dust(ember, 1.6F));

                  for (LivingEntity e : SkillService.this.around(player, ground, skill.range())) {
                     SkillService.this.hurt(player, e, skill.power());
                     e.setFireTicks((int)(skill.duration() * 20.0));
                  }

                  (new BukkitRunnable() {
                     double r = 1.0;

                     public void run() {
                        if (this.r > skill.range() * 1.5) {
                           this.cancel();
                        } else {
                           Fx.ring(w, Fx.onGround(ground), this.r, (int)(this.r * 14.0), Fx.dust(ember, 1.5F));
                           w.spawnParticle(Particle.FLAME, Fx.onGround(ground), (int)(this.r * 6.0), this.r, 0.2, this.r, 0.02);
                           this.r += 0.9;
                        }
                     }
                  }).runTaskTimer(SkillService.this.plugin, 0L, 1L);
                  this.cancel();
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void chain(final Player player, final Skill skill) {
      List<LivingEntity> targets = this.around(player, player.getLocation(), skill.range());
      if (targets.isEmpty()) {
         this.world(player).playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.7F, 1.3F);
      } else {
         (new BukkitRunnable() {
            Location from = player.getEyeLocation();
            final Set<UUID> hit = new HashSet<>();
            int jumps = 0;

            public void run() {
               if (this.jumps++ < 5 && player.isOnline()) {
                  LivingEntity next = null;
                  double best = Double.MAX_VALUE;

                  for (LivingEntity t : SkillService.this.around(player, this.from, skill.range())) {
                     if (!this.hit.contains(t.getUniqueId())) {
                        double d = t.getLocation().distanceSquared(this.from);
                        if (d < best) {
                           best = d;
                           next = t;
                        }
                     }
                  }

                  if (next == null) {
                     this.cancel();
                  } else {
                     this.hit.add(next.getUniqueId());
                     SkillService.this.bolt(player, this.from, next.getLocation().add(0.0, 1.0, 0.0));
                     SkillService.this.hurt(player, next, skill.power() * Math.pow(0.8, this.jumps - 1));
                     SkillService.this.world(player).playSound(next.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F);
                     this.from = next.getLocation().add(0.0, 1.0, 0.0);
                  }
               } else {
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 4L);
      }
   }

   private void bolt(Player player, Location a, Location b) {
      Vector d = b.toVector().subtract(a.toVector());
      int steps = Math.max(4, (int)(d.length() * 3.0));
      Vector step = d.multiply(1.0 / steps);
      Location at = a.clone();

      for (int i = 0; i < steps; i++) {
         at.add(step);
         this.world(player).spawnParticle(Particle.ELECTRIC_SPARK, at, 2, 0.08, 0.08, 0.08, 0.0);
         this.world(player).spawnParticle(Particle.DUST, at, 1, 0.05, 0.05, 0.05, 0.0, new DustOptions(Color.fromRGB(255, 255, 160), 1.0F));
      }
   }

   private void vortex(final Player player, final Skill skill) {
      this.vfx.rune(player.getLocation(), 2.8F, Color.fromRGB(190, 130, 255));
      final Location center = player.getEyeLocation()
         .add(player.getEyeLocation().getDirection().setY(0).normalize().multiply(skill.range() * 0.6))
         .subtract(0.0, 1.2, 0.0);
      this.world(player).playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0F, 0.8F);
      final int life = (int)(skill.duration() * 20.0);
      (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (this.t++ <= life && player.isOnline()) {
                  for (int k = 0; k < 3; k++) {
                     double a = this.t * 0.4 + k * (Math.PI * 2.0 / 3.0);
                     double r = skill.range() * 0.7 * (1.0 - this.t % 20 / 25.0);
                     SkillService.this.world(player)
                        .spawnParticle(
                           Particle.DUST,
                           center.clone().add(Math.cos(a) * r, this.t % 20 * 0.1, Math.sin(a) * r),
                           1,
                           0.0,
                           0.0,
                           0.0,
                           0.0,
                           new DustOptions(Color.fromRGB(150, 60, 220), 1.3F)
                        );
                  }

                  for (LivingEntity e : SkillService.this.around(player, center, skill.range())) {
                     Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.25);
                     e.setVelocity(e.getVelocity().add(pull));
                     if (this.t % 10 == 0) {
                        SkillService.this.hurt(player, e, skill.power() * 0.4);
                     }
                  }

                  if (this.t % 20 == 0) {
                     SkillService.this.world(player).playSound(center, Sound.BLOCK_CONDUIT_AMBIENT, 0.5F, 1.4F);
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fissure(final Player player, final Skill skill) {
      this.vfx.shock(player.getLocation(), 2.6F, Color.fromRGB(220, 160, 90));
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.vfx.shock(player.getLocation(), 4.0F, Color.fromRGB(255, 190, 110)), 6L);
      final Location start = player.getLocation();
      final Vector dir = start.getDirection().setY(0).normalize();
      this.world(player).playSound(start, Sound.BLOCK_DEEPSLATE_BREAK, 1.2F, 0.5F);
      final Set<UUID> hit = new HashSet<>();
      (new BukkitRunnable() {
         int step = 0;

         public void run() {
            if (!(++this.step > skill.range()) && player.isOnline()) {
               Location at = start.clone().add(dir.clone().multiply(this.step));

               for (int dy = 2; dy >= -2; dy--) {
                  Location probe = at.clone().add(0.0, dy, 0.0);
                  if (probe.getBlock().getType().isSolid() && !probe.clone().add(0.0, 1.0, 0.0).getBlock().getType().isSolid()) {
                     at = probe.add(0.0, 1.05, 0.0);
                     break;
                  }
               }

               BlockData ground = at.clone().subtract(0.0, 1.0, 0.0).getBlock().getBlockData();

               for (int i = -2; i <= 2; i++) {
                  Location p = at.clone().add(-dir.getZ() * i * 0.35, 0.0, dir.getX() * i * 0.35);
                  SkillService.this.world(player).spawnParticle(Particle.BLOCK, p, 6, 0.15, 0.1, 0.15, 0.05, ground);
               }

               SkillService.this.world(player).spawnParticle(Particle.LAVA, at, 1, 0.2, 0.1, 0.2, 0.0);
               SkillService.this.world(player).playSound(at, Sound.BLOCK_STONE_BREAK, 0.5F, 0.6F);

               for (LivingEntity t : SkillService.this.around(player, at, 2.0)) {
                  if (hit.add(t.getUniqueId())) {
                     SkillService.this.hurt(player, t, skill.power());
                     t.setVelocity(t.getVelocity().setY(0.5));
                  }
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   private void execute(Player player, Skill skill) {
      this.vfx.slash(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5)), -80.0F, 2.4F, Color.fromRGB(255, 120, 120));
      Location eye = player.getEyeLocation();
      this.world(player).playSound(eye, Sound.ENTITY_WITHER_SHOOT, 0.8F, 0.7F);
      boolean any = false;

      for (LivingEntity t : this.around(player, eye.add(eye.getDirection().multiply(1.5)), skill.range())) {
         AttributeInstance max = t.getAttribute(Attribute.MAX_HEALTH);
         double ratio = max == null ? 1.0 : t.getHealth() / max.getValue();
         double bonus = 1.0 + (1.0 - ratio) * 2.0;
         this.hurt(player, t, skill.power() * bonus);
         Location at = t.getLocation().add(0.0, 1.0, 0.0);
         this.world(player).spawnParticle(Particle.DUST, at, 40, 0.4, 0.6, 0.4, 0.0, new DustOptions(Color.fromRGB(180, 0, 30), 1.6F));
         this.world(player).spawnParticle(Particle.SOUL, at, 10, 0.3, 0.4, 0.3, 0.03);
         this.world(player).playSound(at, ratio < 0.35 ? Sound.ENTITY_WITHER_HURT : Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
         any = true;
      }

      if (!any) {
         this.world(player).playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.7F, 1.2F);
      }
   }

   private void guard(final Player player, Skill skill) {
      this.vfx.rune(player.getLocation(), 2.4F, Color.fromRGB(150, 200, 255));
      final int ticks = (int)(skill.duration() * 20.0);
      player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, ticks, (int)Math.max(0.0, skill.power() - 1.0), true, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, 1, true, false));
      this.world(player).playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 0.7F);
      this.world(player).playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6F, 1.5F);
      (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (this.t++ <= ticks && player.isOnline()) {
                  Location at = player.getLocation().add(0.0, 1.0, 0.0);

                  for (int i = 0; i < 6; i++) {
                     double a = this.t * 0.2 + i * (Math.PI / 3);
                     double y = Math.sin(this.t * 0.1 + i) * 0.8;
                     double r = Math.sqrt(Math.max(0.0, 1.44 - y * y));
                     SkillService.this.world(player)
                        .spawnParticle(
                           Particle.DUST,
                           at.clone().add(Math.cos(a) * r, y, Math.sin(a) * r),
                           1,
                           0.0,
                           0.0,
                           0.0,
                           0.0,
                           new DustOptions(Color.fromRGB(230, 230, 255), 1.0F)
                        );
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void uppercut(final Player player, final Skill skill) {
      this.vfx
         .slash(
            player.getEyeLocation().subtract(0.0, 0.6, 0.0).add(player.getLocation().getDirection().multiply(1.3)), 95.0F, 2.1F, Color.fromRGB(255, 236, 170)
         );
      Location eye = player.getEyeLocation();
      Location front = eye.clone().add(eye.getDirection().setY(0).normalize().multiply(skill.range() * 0.5));
      final List<LivingEntity> caught = this.around(player, front, skill.range());
      this.world(player).playSound(eye, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0F, 1.4F);

      for (LivingEntity t : caught) {
         t.setVelocity(new Vector(0.0, 0.8, 0.0));
         this.hurt(player, t, skill.power() * 0.5);
         this.world(player).spawnParticle(Particle.CLOUD, t.getLocation(), 15, 0.3, 0.1, 0.3, 0.05);
      }

      if (caught.isEmpty()) {
         this.world(player).playSound(eye, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.7F, 1.2F);
      } else {
         (new BukkitRunnable() {
            int n = 0;

            public void run() {
               if (++this.n <= 3 && player.isOnline()) {
                  for (LivingEntity t : caught) {
                     if (!t.isDead()) {
                        SkillService.this.hurt(player, t, skill.power() * 0.5);
                        t.setVelocity(t.getVelocity().setY(0.15));
                        SkillService.this.world(player).spawnParticle(Particle.SWEEP_ATTACK, t.getLocation().add(0.0, 1.0, 0.0), 2, 0.3, 0.3, 0.3, 0.0);
                        SkillService.this.world(player).playSound(t.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8F, 1.2F + this.n * 0.15F);
                     }
                  }
               } else {
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 5L, 5L);
      }
   }

   private void barrage(final Player player, final Skill skill) {
      final Vector look = player.getEyeLocation().getDirection();
      this.world(player).playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 0.8F);
      (new BukkitRunnable() {
         int n = 0;

         public void run() {
            if (++this.n <= 5 && player.isOnline()) {
               double spread = Math.toRadians((this.n - 3) * 7);
               Vector dir = look.clone().rotateAroundY(spread).normalize();
               Arrow arrow = (Arrow)player.launchProjectile(Arrow.class, dir.multiply(2.6));
               arrow.setShooter(player);
               arrow.setCritical(true);
               arrow.setDamage(Math.max(1.0, SkillService.this.stats.get(player, Stat.ATTACK) * skill.power() * 0.2));
               SkillService.this.world(player).playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.7F, 1.0F + this.n * 0.1F);
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   private void bloodRend(final Player player, final Skill skill) {
      this.vfx
         .combo(
            player.getEyeLocation().subtract(0.0, 0.4, 0.0).add(player.getLocation().getDirection().multiply(1.5)),
            new float[]{-35.0F, 35.0F, 0.0F},
            3,
            2.0F,
            Color.fromRGB(220, 40, 60)
         );
      final Vector dir = player.getEyeLocation().getDirection().setY(0).normalize();
      player.setVelocity(dir.clone().multiply(0.5).setY(0.08));
      final World w = this.world(player);
      Fx.chord(w, player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.ITEM_TRIDENT_RIPTIDE_1, Sound.ENTITY_ARROW_SHOOT);
      (new BukkitRunnable() {
            int cut = 0;

            public void run() {
               if (++this.cut <= 3 && player.isOnline()) {
                  Location at = player.getEyeLocation().add(dir.clone().multiply(1.7));

                  double tilt = switch (this.cut) {
                     case 1 -> 0.85;
                     case 2 -> -0.85;
                     default -> 0.0;
                  };
                  Fx.slashArc(w, at, dir, 1.9, tilt, Fx.dust(SkillService.BLOOD, 1.7F));
                  Fx.slashArc(w, at.clone().add(0.0, 0.06, 0.0), dir, 1.75, tilt * 0.95, Fx.dust(SkillService.BLOOD_LIGHT, 1.0F));
                  w.spawnParticle(Particle.CRIT, at, 14, 0.7, 0.5, 0.7, 0.15);
                  final Location keep = at.clone();
                  final double keepTilt = tilt;
                  (new BukkitRunnable() {
                        int fade = 0;

                        public void run() {
                           if (++this.fade > 3) {
                              this.cancel();
                           } else {
                              Fx.slashArc(
                                 w,
                                 keep.clone().subtract(0.0, this.fade * 0.08, 0.0),
                                 dir,
                                 1.9 + this.fade * 0.15,
                                 keepTilt,
                                 Fx.dust(SkillService.BLOOD, 1.5F - this.fade * 0.35F)
                              );
                           }
                        }
                     })
                     .runTaskTimer(SkillService.this.plugin, 2L, 2L);
                  w.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.4F + this.cut * 0.25F);
                  w.playSound(at, Sound.ITEM_TRIDENT_RETURN, 0.5F, 1.9F);

                  for (LivingEntity t : SkillService.this.around(player, at, 2.7)) {
                     SkillService.this.hurt(player, t, skill.power() * 0.6);
                     SkillService.this.bleed.apply(player, t, skill.bleed(), skill.duration());
                     w.spawnParticle(Particle.DUST, t.getLocation().add(0.0, 1.1, 0.0), 18, 0.3, 0.35, 0.3, 0.0, Fx.dust(SkillService.BLOOD, 1.2F));
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 4L);
   }

   private void shadowStep(final Player player, Skill skill) {
      this.vfx.shock(player.getLocation(), 2.0F, Color.fromRGB(90, 70, 140));
      final World w = this.world(player);
      LivingEntity target = null;
      double best = Double.MAX_VALUE;
      Vector look = player.getEyeLocation().getDirection();

      for (LivingEntity t : this.around(player, player.getEyeLocation(), skill.range())) {
         Vector to = t.getLocation().toVector().subtract(player.getLocation().toVector());
         if (!(to.lengthSquared() < 1.0E-6) && !(look.dot(to.clone().normalize()) < 0.6)) {
            double d = to.lengthSquared();
            if (d < best) {
               best = d;
               target = t;
            }
         }
      }

      Location from = player.getLocation().clone();
      Fx.chord(w, from, Sound.ENTITY_ENDERMAN_TELEPORT, Sound.BLOCK_BEACON_DEACTIVATE, Sound.ENTITY_PHANTOM_FLAP);
      Fx.pillar(w, from, 2.4, 0.55, Fx.dust(SHADOW, 1.6F));
      Fx.ring(w, from.clone().add(0.0, 0.08, 0.0), 1.1, 40, Fx.dust(SHADOW_LIGHT, 1.2F));
      w.spawnParticle(Particle.SQUID_INK, from.clone().add(0.0, 1.0, 0.0), 45, 0.4, 0.8, 0.4, 0.02);
      Location landed;
      if (target == null) {
         player.setVelocity(look.clone().setY(0.1).normalize().multiply(1.1));
         landed = player.getLocation();
      } else {
         Vector behind = target.getLocation().getDirection().setY(0).normalize().multiply(-1.4);
         Location to = target.getLocation().add(behind);
         to.setDirection(target.getLocation().toVector().subtract(to.toVector()).normalize());
         if (to.getBlock().getType().isSolid()) {
            to.add(0.0, 1.0, 0.0);
         }

         player.teleport(to);
         landed = to;
         Fx.helix(w, from.clone().add(0.0, 1.0, 0.0), to.clone().add(0.0, 1.0, 0.0), 0.5, 3.0, 60, Fx.dust(SHADOW_LIGHT, 1.0F));
      }

      Fx.sphere(w, landed.clone().add(0.0, 1.0, 0.0), 1.3, 70, Fx.dust(SHADOW, 1.4F));
      w.spawnParticle(Particle.SQUID_INK, landed.clone().add(0.0, 1.0, 0.0), 40, 0.4, 0.7, 0.4, 0.03);
      w.playSound(landed, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.9F);
      final int ticks = (int)(skill.duration() * 20.0);
      player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 0, true, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 1, true, false));
      this.guaranteedCrit.add(player.getUniqueId());
      player.sendActionBar(Msg.of("&8&l그림자 걸음 &7— 다음 한 대는 &c&l확정 치명타"));
      (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (this.t++ <= ticks && player.isOnline() && SkillService.this.guaranteedCrit.contains(player.getUniqueId())) {
                  Location at = player.getLocation().add(0.0, 0.15, 0.0);
                  double a = this.t * 0.5;

                  for (int k = 0; k < 3; k++) {
                     double ang = a + (Math.PI * 2) * k / 3.0;
                     w.spawnParticle(
                        Particle.DUST,
                        at.clone().add(Math.cos(ang) * 0.7, 0.2 + Math.sin(this.t * 0.2 + k) * 0.5, Math.sin(ang) * 0.7),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        Fx.dust(SkillService.SHADOW_LIGHT, 0.9F)
                     );
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void bloodBrand(final Player player, final Skill skill) {
      final World w = this.world(player);
      final Location center = Fx.onGround(player.getLocation());
      Fx.chord(w, center, Sound.ENTITY_WITHER_SHOOT, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, Sound.ENTITY_EVOKER_PREPARE_ATTACK);
      final List<LivingEntity> marked = this.around(player, center, skill.range());
      if (marked.isEmpty()) {
         w.playSound(center, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.7F, 1.2F);
      } else {
         for (LivingEntity t : marked) {
            Fx.rune(w, Fx.onGround(t.getLocation()), 1.0, 0.0, Fx.dust(BLOOD, 1.4F), Fx.dust(BLOOD_LIGHT, 0.9F));
         }

         (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (this.t <= 18 && player.isOnline()) {
                  double spin = this.t * this.t * 0.02;
                  Fx.rune(w, center, skill.range() * 0.9, spin, Fx.dust(SkillService.BLOOD, 1.5F), Fx.dust(SkillService.BLOOD_LIGHT, 1.0F));

                  for (LivingEntity e : marked) {
                     if (!e.isDead()) {
                        Fx.ring(w, Fx.onGround(e.getLocation()), 0.9 - this.t * 0.03, 20, Fx.dust(SkillService.BLOOD_LIGHT, 1.1F));
                     }
                  }

                  if (this.t % 6 == 0) {
                     w.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.7F + this.t * 0.05F);
                  }

                  this.t += 2;
               } else {
                  if (player.isOnline()) {
                     SkillService.this.detonate(player, skill, marked, center, w);
                  }

                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 2L);
      }
   }

   private void detonate(Player player, final Skill skill, List<LivingEntity> marked, final Location center, final World w) {
      w.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0.0, 0.5, 0.0), 1);
      Fx.chord(w, center, Sound.ENTITY_GENERIC_EXPLODE, Sound.ENTITY_WITHER_HURT, Sound.ITEM_TRIDENT_THUNDER);

      for (LivingEntity t : marked) {
         if (!t.isDead()) {
            int stacks = this.bleed.stacks(t);
            this.hurt(player, t, skill.power() * (1.0 + stacks * 0.25));
            double burst = this.bleed.detonate(player, t, 1.0);
            Location at = t.getLocation();
            Fx.pillar(w, at, 3.0, 0.7, Fx.dust(BLOOD, 1.7F));
            Fx.sphere(w, at.clone().add(0.0, 1.0, 0.0), 1.4, 60, Fx.dust(BLOOD_LIGHT, 1.2F));
            w.spawnParticle(Particle.DUST, at.clone().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, Fx.dust(BLOOD, 1.5F));
            if (burst > 0.0 || stacks > 0) {
               for (LivingEntity near : this.around(player, at, 3.5)) {
                  if (!near.equals(t)) {
                     this.bleed.apply(player, near, skill.bleed() * 0.66, skill.duration());
                     Fx.beam(w, at.clone().add(0.0, 1.0, 0.0), near.getLocation().add(0.0, 1.0, 0.0), 0.3, Fx.dust(BLOOD, 1.0F));
                  }
               }
            }
         }
      }

      (new BukkitRunnable() {
         double r = 1.0;

         public void run() {
            if (this.r > skill.range() * 1.4) {
               this.cancel();
            } else {
               Fx.ring(w, center, this.r, (int)(this.r * 14.0), Fx.dust(SkillService.BLOOD, 1.3F));
               this.r += 0.9;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void crimsonWaltz(final Player player, final Skill skill) {
      this.vfx
         .combo(
            player.getEyeLocation().subtract(0.0, 0.3, 0.0).add(player.getLocation().getDirection().multiply(1.4)),
            new float[]{0.0F, 72.0F, 144.0F, 216.0F, 288.0F},
            3,
            2.2F,
            Color.fromRGB(255, 60, 90)
         );
      this.vfx.rune(player.getLocation(), 3.0F, Color.fromRGB(180, 20, 40));
      final World w = this.world(player);
      final List<LivingEntity> pool = this.around(player, player.getLocation(), skill.range());
      if (pool.isEmpty()) {
         player.sendActionBar(Msg.of("&8벨 것이 없습니다"));
         w.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.7F, 1.2F);
      } else {
         final Location home = player.getLocation().clone();
         final Location ground = Fx.onGround(home);
         player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, true, false));
         player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false));
         Fx.chord(w, home, Sound.ENTITY_WITHER_SPAWN, Sound.BLOCK_BEACON_ACTIVATE, Sound.ITEM_TRIDENT_THUNDER);
         player.sendTitle("", "§4§l처형 무도", 0, 30, 10);
         (new BukkitRunnable() {
            int t = 0;

            public void run() {
               if (this.t++ <= 50 && player.isOnline()) {
                  Fx.rune(w, ground, 3.2, this.t * 0.15, Fx.dust(SkillService.BLOOD, 1.5F), Fx.dust(SkillService.BLOOD_LIGHT, 1.0F));
               } else {
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 2L);
         (new BukkitRunnable() {
               int n = 0;
               final List<LivingEntity> hit = new ArrayList<>();
               final List<Location> path = new ArrayList<>();

               public void run() {
                  if (!player.isOnline()) {
                     this.cancel();
                  } else if (this.n >= 6) {
                     this.finish();
                     this.cancel();
                  } else {
                     LivingEntity t = pool.stream()
                        .filter(e -> !e.isDead() && !this.hit.contains(e))
                        .findFirst()
                        .orElseGet(() -> pool.stream().filter(e -> !e.isDead()).findFirst().orElse(null));
                     if (t == null) {
                        this.finish();
                        this.cancel();
                     } else {
                        this.hit.add(t);
                        this.n++;
                        Location from = player.getLocation().clone();
                        Location to = t.getLocation().clone().add(t.getLocation().getDirection().setY(0).normalize().multiply(-1.2));
                        if (to.getBlock().getType().isSolid()) {
                           to.add(0.0, 1.0, 0.0);
                        }

                        to.setDirection(t.getLocation().toVector().subtract(to.toVector()).normalize());
                        player.teleport(to);
                        this.path.add(to.clone().add(0.0, 1.0, 0.0));
                        Fx.helix(w, from.add(0.0, 1.0, 0.0), to.clone().add(0.0, 1.0, 0.0), 0.35, 2.0, 40, Fx.dust(SkillService.BLOOD, 1.1F));
                        Vector d = t.getLocation().toVector().subtract(to.toVector()).setY(0).normalize();
                        Fx.slashArc(w, t.getLocation().add(0.0, 1.1, 0.0), d, 1.6, this.n % 2 == 0 ? 0.7 : -0.7, Fx.dust(SkillService.BLOOD_LIGHT, 1.5F));
                        w.spawnParticle(Particle.CRIT, t.getLocation().add(0.0, 1.0, 0.0), 20, 0.3, 0.4, 0.3, 0.2);
                        SkillService.this.hurt(player, t, skill.power());
                        SkillService.this.bleed.apply(player, t, skill.bleed(), 6.0);
                        w.playSound(t.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.1F + this.n * 0.12F);
                        w.playSound(t.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.5F, 1.9F);
                     }
                  }
               }

               private void finish() {
                  player.teleport(home);
                  Location at = home.clone().add(0.0, 1.0, 0.0);

                  for (int i = 0; i < this.path.size(); i++) {
                     Fx.beam(w, this.path.get(i), this.path.get((i + 1) % this.path.size()), 0.35, Fx.dust(SkillService.BLOOD, 1.2F));
                  }

                  w.spawnParticle(Particle.EXPLOSION_EMITTER, at, 3, 0.6, 0.4, 0.6, 0.0);
                  Fx.chord(w, at, Sound.ENTITY_GENERIC_EXPLODE, Sound.ENTITY_WITHER_DEATH, Sound.ITEM_TRIDENT_THUNDER);
                  Fx.sphere(w, at, 2.2, 120, Fx.dust(SkillService.BLOOD, 1.8F));

                  for (LivingEntity t : this.hit) {
                     if (!t.isDead()) {
                        SkillService.this.bleed.detonate(player, t, 1.5);
                     }
                  }

                  (new BukkitRunnable() {
                     double r = 1.0;

                     public void run() {
                        if (this.r > 9.0) {
                           this.cancel();
                        } else {
                           Fx.ring(w, Fx.onGround(home), this.r, (int)(this.r * 16.0), Fx.dust(SkillService.BLOOD, 1.6F));
                           Fx.ring(w, Fx.onGround(home).add(0.0, 0.4, 0.0), this.r * 0.8, (int)(this.r * 12.0), Fx.dust(SkillService.BLOOD_LIGHT, 1.1F));
                           this.r += 0.8;
                        }
                     }
                  }).runTaskTimer(SkillService.this.plugin, 0L, 1L);
               }
            })
            .runTaskTimer(this.plugin, 6L, 6L);
      }
   }

   private void trail(Player player, Location a, Location b) {
      Fx.beam(this.world(player), a, b, 0.3, Fx.dust(BLOOD, 1.0F));
   }

   private World world(Player player) {
      return player.getWorld();
   }

   private List<LivingEntity> around(Player player, Location at, double range) {
      List<LivingEntity> out = new ArrayList<>();

      for (Entity e : at.getWorld().getNearbyEntities(at, range, range, range)) {
         if (e instanceof LivingEntity t && !t.equals(player) && !t.isDead()) {
            out.add(t);
         }
      }

      return out;
   }

   private void mythicBonus(Player player, Skill skill) {
      double range = skill.range() > 0.0 ? skill.range() : 6.0;
      Vector look = player.getEyeLocation().getDirection();
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            for (LivingEntity e : this.around(player, player.getEyeLocation(), range)) {
               Vector to = e.getLocation().toVector().subtract(player.getLocation().toVector());
               if (!(to.lengthSquared() < 1.0E-4) && !(look.dot(to.normalize()) < 0.5)) {
                  this.hurt(player, e, skill.power());
               }
            }
         }
      }, 3L);
   }

   private void hurt(Player player, LivingEntity target, double power) {
      double base = Math.max(1.0, this.stats.get(player, Stat.ATTACK) * 0.5);
      target.setNoDamageTicks(0);
      target.damage(base * power, player);
   }
}
