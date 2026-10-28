package com.wolfool.prowess.skill;

import com.wolfool.prowess.integration.CraftEngineHook;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class FireVfx {
   private final Plugin plugin;

   public FireVfx(Plugin plugin) {
      this.plugin = plugin;
   }

   public void slash(Location at, float roll, float size, int hold) {
      this.upright(FireVfx.Sheet.SLASH, at, roll, size, hold);
   }

   public void azure(Location at, float roll, float size, int hold) {
      this.upright(FireVfx.Sheet.AZURE, at, roll, size, hold);
   }

   public void combo(Location at, float[] rolls, int gap, float size) {
      for (int i = 0; i < rolls.length; i++) {
         float roll = rolls[i];
         this.later(() -> this.slash(at, roll, size, 1), (long)i * gap);
      }
   }

   public void wave(Location at, float size, int hold) {
      this.flat(FireVfx.Sheet.WAVE, at, size, hold);
   }

   public void rune(Location at, float size, int hold) {
      this.flat(FireVfx.Sheet.RUNE, at, size, hold);
   }

   public void pillar(Location ground, float size, int hold) {
      FireVfx.Sheet s = FireVfx.Sheet.PILLAR;
      ItemDisplay d = this.spawn(s, ground.clone().add(0.0, 0.02, 0.0));
      if (d != null) {
         d.setBillboard(Billboard.FIXED);
         d.setRotation(ground.getYaw(), 0.0F);
         this.shape(d, size, 0.0F, 0.0F);
         this.animate(d, s, hold);
      }
   }

   private void upright(FireVfx.Sheet s, Location at, float roll, float size, int hold) {
      ItemDisplay d = this.spawn(s, at);
      if (d != null) {
         d.setBillboard(Billboard.FIXED);
         d.setRotation(at.getYaw(), 0.0F);
         this.shape(d, size, roll, -0.5F * size);
         this.animate(d, s, hold);
      }
   }

   private void flat(FireVfx.Sheet s, Location at, float size, int hold) {
      Location on = at.clone();
      on.setY(on.getY() + 0.06);
      ItemDisplay d = this.spawn(s, on);
      if (d != null) {
         d.setBillboard(Billboard.FIXED);
         d.setRotation(at.getYaw(), 90.0F);
         this.shape(d, size, 0.0F, -0.5F * size);
         this.animate(d, s, hold);
      }
   }

   @Nullable
   private ItemDisplay spawn(FireVfx.Sheet s, Location at) {
      ItemStack item = CraftEngineHook.build("w11fx:" + s.id + "_1", 1);
      return item != null && at.getWorld() != null ? (ItemDisplay)at.getWorld().spawn(at, ItemDisplay.class, e -> {
         e.setItemStack(item);
         e.setItemDisplayTransform(ItemDisplayTransform.FIXED);
         e.setPersistent(false);
         e.setViewRange(1.4F);
         e.setInterpolationDuration(1);
         e.setTeleportDuration(1);
         e.setBrightness(new Brightness(15, 15));
      }) : null;
   }

   private void shape(ItemDisplay d, float size, float roll, float drop) {
      d.setTransformation(
         new Transformation(
            new Vector3f(0.0F, drop, 0.0F),
            new AxisAngle4f((float)Math.toRadians(roll), 0.0F, 0.0F, 1.0F),
            new Vector3f(size, size, size),
            new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
         )
      );
   }

   private void animate(ItemDisplay d, FireVfx.Sheet s, int hold) {
      for (int i = 2; i <= s.frames; i++) {
         String id = s.id + "_" + i;
         this.later(() -> {
            if (d.isValid()) {
               ItemStack next = CraftEngineHook.build("w11fx:" + id, 1);
               if (next != null) {
                  d.setItemStack(next);
               }
            }
         }, (long)(i - 1) * hold);
      }

      this.later(() -> {
         if (d.isValid()) {
            d.remove();
         }
      }, (long)s.frames * hold + 1L);
   }

   private void later(Runnable r, long delay) {
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, r, Math.max(0L, delay));
   }

   public enum Sheet {
      SLASH("fireslash", 14),
      AZURE("azureslash", 14),
      WAVE("firewave", 10),
      RUNE("firerune", 12),
      PILLAR("firepillar", 10);

      final String id;
      final int frames;

      Sheet(String id, int frames) {
         this.id = id;
         this.frames = frames;
      }
   }
}
