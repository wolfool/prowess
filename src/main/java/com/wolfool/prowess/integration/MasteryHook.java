package com.wolfool.prowess.integration;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.stat.StatMap;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MasteryHook {
   private final Plugin plugin;
   private Method levelOf;
   private Method isReady;
   private Method jobOf;
   private Method displayName;
   private boolean tried;

   public MasteryHook(Plugin plugin) {
      this.plugin = plugin;
   }

   private void connect() {
      if (!this.tried) {
         this.tried = true;
         Plugin mastery = Bukkit.getPluginManager().getPlugin("Mastery");
         if (mastery != null && mastery.isEnabled()) {
            try {
               Class<?> api = Class.forName("com.wolfool.mastery.MasteryApi", true, mastery.getClass().getClassLoader());
               this.levelOf = api.getMethod("levelOf", Player.class, String.class);
               this.isReady = api.getMethod("isReady");
               this.jobOf = findQuietly(api, "jobOf", UUID.class);
               this.displayName = findQuietly(api, "displayName", String.class);
               this.plugin.getLogger().info("Mastery 와 연결됐습니다 — 직업 특전이 켜집니다.");
            } catch (Throwable t) {
               this.levelOf = null;
               this.jobOf = null;
               this.displayName = null;
               this.plugin.getLogger().warning("Mastery 는 있는데 MasteryApi 를 못 찾았습니다: " + t.getClass().getSimpleName() + " — 직업 특전 없이 돕니다.");
            }
         } else {
            this.plugin.getLogger().info("Mastery 가 없습니다. 직업 특전 없이 돕니다.");
         }
      }
   }

   public void contribute(Player player, StatMap out, ProwessConfig config) {
      this.connect();
      if (this.levelOf != null) {
         for (Entry<String, StatMap> e : config.jobPerLevel().entrySet()) {
            int level = this.levelOf(player, e.getKey());
            if (level > 0) {
               out.addScaled(e.getValue(), level);
            }
         }
      }
   }

   public String jobName(Player player) {
      this.connect();
      if (this.jobOf == null) {
         return "";
      }

      try {
         Object key = this.jobOf.invoke(null, player.getUniqueId());
         if (key == null) {
            return "";
         }

         if (this.displayName != null) {
            Object ko = this.displayName.invoke(null, key);
            if (ko != null) {
               return String.valueOf(ko);
            }
         }

         return String.valueOf(key);
      } catch (ReflectiveOperationException e) {
         return "";
      }
   }

   private static Method findQuietly(Class<?> api, String name, Class<?>... args) {
      try {
         return api.getMethod(name, args);
      } catch (NoSuchMethodException e) {
         return null;
      }
   }

   public int levelOf(Player player, String jobName) {
      this.connect();
      if (this.levelOf == null) {
         return 0;
      }

      try {
         return this.levelOf.invoke(null, player, jobName) instanceof Number n ? n.intValue() : 0;
      } catch (Throwable t) {
         return 0;
      }
   }

   public boolean connected() {
      this.connect();
      if (this.levelOf == null) {
         return false;
      }

      try {
         return this.isReady == null || Boolean.TRUE.equals(this.isReady.invoke(null));
      } catch (Throwable t) {
         return false;
      }
   }

   public void reset() {
      this.tried = false;
      this.levelOf = null;
      this.jobOf = null;
      this.displayName = null;
      this.isReady = null;
   }
}
