package com.wolfool.prowess.integration;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class MythicSkills {
   private final Plugin plugin;
   private Object helper;
   private Method castSkill;
   private Method getInstance;
   private Method getMobType;
   private boolean tried;

   public MythicSkills(Plugin plugin) {
      this.plugin = plugin;
   }

   private void connect() {
      if (!this.tried) {
         this.tried = true;
         Plugin mm = Bukkit.getPluginManager().getPlugin("MythicMobs");
         if (mm != null && mm.isEnabled()) {
            try {
               Class<?> bukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit", true, mm.getClass().getClassLoader());
               Object inst = bukkitClass.getMethod("inst").invoke(null);
               this.helper = bukkitClass.getMethod("getAPIHelper").invoke(inst);
               this.castSkill = this.helper.getClass().getMethod("castSkill", Entity.class, String.class, Location.class);
               this.getInstance = this.helper.getClass().getMethod("getMythicMobInstance", Entity.class);
               this.getMobType = this.getInstance.getReturnType().getMethod("getMobType");
               this.plugin.getLogger().info("MythicMobs 스킬을 쏠 수 있습니다.");
            } catch (Throwable t) {
               this.helper = null;
               this.plugin.getLogger().warning("MythicMobs 스킬 창구를 못 찾았습니다: " + t.getClass().getSimpleName());
            }
         }
      }
   }

   public boolean cast(Player player, String skillName) {
      this.connect();
      if (this.helper != null && skillName != null && !skillName.isBlank()) {
         try {
            Object ok = this.castSkill.invoke(this.helper, player, skillName, player.getEyeLocation());
            return Boolean.TRUE.equals(ok);
         } catch (Throwable t) {
            this.plugin.getLogger().warning("MythicMobs 스킬 '" + skillName + "' 을 못 쐈습니다: " + t.getClass().getSimpleName());
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   public String mobType(Entity entity) {
      this.connect();
      if (this.helper != null && entity != null) {
         try {
            Object mob = this.getInstance.invoke(this.helper, entity);
            return mob == null ? null : (String)this.getMobType.invoke(mob);
         } catch (Throwable t) {
            return null;
         }
      } else {
         return null;
      }
   }

   public boolean connected() {
      this.connect();
      return this.helper != null;
   }

   public void reset() {
      this.tried = false;
      this.helper = null;
   }
}
