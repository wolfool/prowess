package com.wolfool.prowess.skill;

import com.wolfool.prowess.integration.CraftEngineHook;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class Vfx {
   private final Plugin plugin;

   public Vfx(Plugin plugin) {
      this.plugin = plugin;
   }

   public void slash(Location at, float roll, float size, Color color) {
      this.play(at, "slash", 8, roll, size, color, 1, 0.0);
   }

   public void combo(Location at, float[] rolls, int gap, float size, Color color) {
      for (int i = 0; i < rolls.length; i++) {
         float roll = rolls[i];
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.slash(at, roll, size, color), (long)i * gap);
      }
   }

   public void thrust(Location at, float roll, float size, Color color) {
      this.play(at, "thrust", 6, roll, size, color, 1, 0.0);
   }

   public void shock(Location at, float size, Color color) {
      this.flat(at, "ring", 6, size, color, 2);
   }

   public void rune(Location at, float size, Color color) {
      this.flat(at, "rune", 4, size, color, 5);
   }

   private void play(Location at, String set, int frames, float roll, float size, Color color, int hold, double rise) {
      ItemDisplay d = this.spawn(at, set + "_1", color);
      if (d != null) {
         d.setBillboard(Billboard.FIXED);
         d.setRotation(at.getYaw(), 0.0F);
         this.shape(d, size, roll);
         this.animate(d, set, frames, hold, rise);
      }
   }

   private void flat(Location at, String set, int frames, float size, Color color, int hold) {
      Location on = at.clone();
      on.setY(on.getY() + 0.06);
      ItemDisplay d = this.spawn(on, set + "_1", color);
      if (d != null) {
         d.setBillboard(Billboard.FIXED);
         d.setRotation(at.getYaw(), 90.0F);
         this.shape(d, size, 0.0F);
         this.animate(d, set, frames, hold, 0.0);
      }
   }

   @Nullable
   private ItemDisplay spawn(Location at, String id, Color color) {
      ItemStack item = CraftEngineHook.build("fx:" + id, 1);
      if (item == null) {
         return null;
      }

      dye(item, color);
      return at.getWorld() == null ? null : (ItemDisplay)at.getWorld().spawn(at, ItemDisplay.class, e -> {
         e.setItemStack(item);
         e.setPersistent(false);
         e.setViewRange(1.2F);
         e.setInterpolationDuration(1);
         e.setTeleportDuration(1);
         e.setBrightness(new Brightness(15, 15));
      });
   }

   private void shape(ItemDisplay d, float size, float roll) {
      d.setTransformation(
         new Transformation(
            new Vector3f(0.0F, 0.0F, 0.0F),
            new AxisAngle4f((float)Math.toRadians(roll), 0.0F, 0.0F, 1.0F),
            new Vector3f(size, size, size),
            new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
         )
      );
   }

   private void animate(ItemDisplay d, String set, int frames, int hold, double rise) {
      Color color = colorOf(d);

      for (int i = 2; i <= frames; i++) {
         String id = set + "_" + i;
         this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (d.isValid()) {
               ItemStack next = CraftEngineHook.build("fx:" + id, 1);
               if (next != null) {
                  dye(next, color);
                  d.setItemStack(next);
               }

               if (rise != 0.0) {
                  d.teleport(d.getLocation().add(0.0, rise, 0.0));
               }
            }
         }, (long)(i - 1) * hold);
      }

      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         if (d.isValid()) {
            d.remove();
         }
      }, (long)frames * hold + 1L);
   }

   private static void dye(ItemStack item, Color color) {
      item.editMeta(m -> {
         if (m instanceof LeatherArmorMeta leather) {
            leather.setColor(color);
         }
      });
   }

   private static Color colorOf(ItemDisplay d) {
      return d.getItemStack().getItemMeta() instanceof LeatherArmorMeta leather ? leather.getColor() : Color.WHITE;
   }
}
