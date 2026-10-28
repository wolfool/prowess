package com.wolfool.prowess.skill;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.util.Vector;

public final class Fx {
   private Fx() {
   }

   public static DustOptions dust(Color color, float size) {
      return new DustOptions(color, size);
   }

   public static DustTransition trans(Color from, Color to, float size) {
      return new DustTransition(from, to, size);
   }

   public static void ring(World w, Location at, double radius, int points, DustOptions color) {
      for (int i = 0; i < points; i++) {
         double a = (Math.PI * 2) * i / points;
         w.spawnParticle(Particle.DUST, at.getX() + Math.cos(a) * radius, at.getY(), at.getZ() + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0, color);
      }
   }

   public static void ringTilted(World w, Location at, double radius, int points, Vector axis, DustOptions color) {
      Vector a1 = perpendicular(axis).normalize();
      Vector a2 = axis.clone().crossProduct(a1).normalize();

      for (int i = 0; i < points; i++) {
         double t = (Math.PI * 2) * i / points;
         Vector p = a1.clone().multiply(Math.cos(t) * radius).add(a2.clone().multiply(Math.sin(t) * radius));
         w.spawnParticle(Particle.DUST, at.clone().add(p), 1, 0.0, 0.0, 0.0, 0.0, color);
      }
   }

   public static void sphere(World w, Location at, double radius, int points, DustOptions color) {
      double golden = Math.PI * (3.0 - Math.sqrt(5.0));

      for (int i = 0; i < points; i++) {
         double y = 1.0 - (double)i / (points - 1) * 2.0;
         double r = Math.sqrt(1.0 - y * y);
         double th = golden * i;
         w.spawnParticle(Particle.DUST, at.clone().add(Math.cos(th) * r * radius, y * radius, Math.sin(th) * r * radius), 1, 0.0, 0.0, 0.0, 0.0, color);
      }
   }

   public static void helix(World w, Location from, Location to, double radius, double turns, int points, DustOptions color) {
      Vector d = to.toVector().subtract(from.toVector());
      double len = d.length();
      if (!(len < 1.0E-6)) {
         Vector dir = d.clone().multiply(1.0 / len);
         Vector a1 = perpendicular(dir).normalize();
         Vector a2 = dir.clone().crossProduct(a1).normalize();

         for (int i = 0; i < points; i++) {
            double f = (double)i / points;
            double t = f * Math.PI * 2.0 * turns;
            Location p = from.clone()
               .add(dir.clone().multiply(len * f))
               .add(a1.clone().multiply(Math.cos(t) * radius))
               .add(a2.clone().multiply(Math.sin(t) * radius));
            w.spawnParticle(Particle.DUST, p, 1, 0.0, 0.0, 0.0, 0.0, color);
         }
      }
   }

   public static void beam(World w, Location from, Location to, double step, DustOptions color) {
      Vector d = to.toVector().subtract(from.toVector());
      int n = Math.max(2, (int)(d.length() / step));
      Vector s = d.multiply(1.0 / n);
      Location at = from.clone();

      for (int i = 0; i < n; i++) {
         at.add(s);
         w.spawnParticle(Particle.DUST, at, 1, 0.02, 0.02, 0.02, 0.0, color);
      }
   }

   public static void rune(World w, Location at, double radius, double spin, DustOptions outer, DustOptions inner) {
      ring(w, at, radius, 60, outer);
      ring(w, at, radius * 0.62, 36, inner);

      for (int i = 0; i < 8; i++) {
         double a = (Math.PI * 2) * i / 8.0 + spin;

         for (double r = radius * 0.62; r <= radius; r += 0.25) {
            w.spawnParticle(Particle.DUST, at.getX() + Math.cos(a) * r, at.getY(), at.getZ() + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0, inner);
         }
      }

      for (int i = 0; i < 12; i++) {
         double a = (Math.PI * 2) * i / 12.0 - spin * 1.5;
         w.spawnParticle(
            Particle.END_ROD, at.getX() + Math.cos(a) * radius * 0.82, at.getY() + 0.05, at.getZ() + Math.sin(a) * radius * 0.82, 1, 0.0, 0.0, 0.0, 0.0
         );
      }
   }

   public static void slashArc(World w, Location at, Vector dir, double radius, double tilt, DustOptions color) {
      Vector side = new Vector(-dir.getZ(), 0.0, dir.getX()).normalize();

      for (int i = -12; i <= 12; i++) {
         double f = i / 12.0;
         double bulge = (1.0 - f * f) * 0.55;
         Location p = at.clone().add(side.clone().multiply(f * radius)).add(dir.clone().normalize().multiply(bulge)).add(0.0, f * tilt, 0.0);
         w.spawnParticle(Particle.DUST, p, 1, 0.03, 0.03, 0.03, 0.0, color);
         if (i % 3 == 0) {
            w.spawnParticle(Particle.SWEEP_ATTACK, p, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   public static void pillar(World w, Location at, double height, double radius, DustOptions color) {
      for (double y = 0.0; y < height; y += 0.25) {
         double f = y / height;
         double r = radius * (1.0 - f * 0.4);

         for (int i = 0; i < 5; i++) {
            double a = (Math.PI * 2) * i / 5.0 + y * 1.2;
            w.spawnParticle(Particle.DUST, at.getX() + Math.cos(a) * r, at.getY() + y, at.getZ() + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0, color);
         }
      }
   }

   public static void bolt(World w, Location from, Location to, DustOptions color) {
      Vector d = to.toVector().subtract(from.toVector());
      int n = Math.max(4, (int)(d.length() * 2.5));
      Vector s = d.multiply(1.0 / n);
      Location at = from.clone();

      for (int i = 0; i < n; i++) {
         at.add(s);
         double amp = Math.sin(Math.PI * i / n) * 0.45;
         Location p = at.clone().add((Math.random() - 0.5) * amp, (Math.random() - 0.5) * amp, (Math.random() - 0.5) * amp);
         w.spawnParticle(Particle.DUST, p, 1, 0.0, 0.0, 0.0, 0.0, color);
         w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.03, 0.03, 0.03, 0.0);
      }
   }

   public static void chord(World w, Location at, Sound low, Sound mid, Sound high) {
      if (low != null) {
         w.playSound(at, low, 1.0F, 0.6F);
      }

      if (mid != null) {
         w.playSound(at, mid, 0.8F, 1.0F);
      }

      if (high != null) {
         w.playSound(at, high, 0.6F, 1.7F);
      }
   }

   private static Vector perpendicular(Vector v) {
      Vector a = Math.abs(v.getY()) < 0.9 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
      return v.clone().crossProduct(a);
   }

   public static Location onGround(Location at) {
      Location p = at.clone();

      for (int dy = 2; dy >= -3; dy--) {
         Location probe = at.clone().add(0.0, dy, 0.0);
         if (probe.getBlock().getType().isSolid() && !probe.clone().add(0.0, 1.0, 0.0).getBlock().getType().isSolid()) {
            return probe.add(0.0, 1.08, 0.0);
         }
      }

      return p;
   }
}
