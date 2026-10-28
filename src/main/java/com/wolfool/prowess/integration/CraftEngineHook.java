package com.wolfool.prowess.integration;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public final class CraftEngineHook {
   private static Method byItem;
   private static Method byId;
   private static boolean tried;
   private static boolean available;

   private CraftEngineHook() {
   }

   private static void connect() {
      if (!tried) {
         tried = true;
         if (Bukkit.getPluginManager().getPlugin("CraftEngine") != null) {
            try {
               Class<?> items = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
               byItem = items.getMethod("getCustomItemId", ItemStack.class);
               byId = items.getMethod("byId", String.class);
               available = true;
            } catch (Throwable t) {
               available = false;
            }
         }
      }
   }

   @Nullable
   public static String idOf(@Nullable ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         connect();
         if (available) {
            try {
               Object key = byItem.invoke(null, item);
               if (key != null) {
                  return key.toString();
               }
            } catch (Throwable var2) {
            }
         }

         return modelIdOf(item);
      } else {
         return null;
      }
   }

   @Nullable
   public static String modelIdOf(@Nullable ItemStack item) {
      if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasItemModel()) {
            NamespacedKey key = meta.getItemModel();
            return key == null ? null : key.toString();
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static ItemStack build(@Nullable String id, int amount) {
      if (id != null && !id.isBlank()) {
         connect();
         if (available && byId != null) {
            try {
               Object def = byId.invoke(null, id);
               if (def == null) {
                  return null;
               } else if (def.getClass().getMethod("buildBukkitItem").invoke(def) instanceof ItemStack it) {
                  it.setAmount(Math.max(1, Math.min(amount, it.getMaxStackSize())));
                  return it;
               } else {
                  return null;
               }
            } catch (Throwable t) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean available() {
      connect();
      return available;
   }
}
