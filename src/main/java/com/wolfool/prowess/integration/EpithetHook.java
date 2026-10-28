package com.wolfool.prowess.integration;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.stat.StatMap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class EpithetHook {
   private final Plugin plugin;
   private Object epithet;
   private Method dataMethod;
   private Method getMethod;
   private Method equippedMethod;
   private Method titlesMethod;
   private boolean tried;

   public EpithetHook(Plugin plugin) {
      this.plugin = plugin;
   }

   private void connect() {
      if (!this.tried) {
         this.tried = true;
         Plugin ep = Bukkit.getPluginManager().getPlugin("Epithet");
         if (ep != null && ep.isEnabled()) {
            try {
               this.epithet = ep;
               this.dataMethod = ep.getClass().getMethod("data");
               this.titlesMethod = ep.getClass().getMethod("titles");
               Object manager = this.dataMethod.invoke(ep);
               this.getMethod = findGet(manager.getClass());
               if (this.getMethod == null) {
                  throw new NoSuchMethodException("PlayerDataManager.get");
               }

               Class<?> dataClass = this.getMethod.getReturnType();
               this.equippedMethod = dataClass.getMethod("equipped");
               this.plugin.getLogger().info("Epithet 과 연결됐습니다 — 칭호 특전이 켜집니다.");
            } catch (Throwable t) {
               this.epithet = null;
               this.plugin.getLogger().warning("Epithet 은 있는데 물어볼 길을 못 찾았습니다: " + t.getClass().getSimpleName() + " — 칭호 특전 없이 돕니다.");
            }
         } else {
            this.plugin.getLogger().info("Epithet 이 없습니다. 칭호 특전 없이 돕니다.");
         }
      }
   }

   @Nullable
   private static Method findGet(Class<?> managerClass) {
      for (Method m : managerClass.getMethods()) {
         if (m.getName().equals("get") && m.getParameterCount() == 1) {
            Class<?> p = m.getParameterTypes()[0];
            if (p.isAssignableFrom(Player.class) || p == UUID.class) {
               return m;
            }
         }
      }

      return null;
   }

   @Nullable
   public String equipped(Player player) {
      this.connect();
      if (this.epithet == null) {
         return null;
      }

      try {
         Object manager = this.dataMethod.invoke(this.epithet);
         Object arg = this.getMethod.getParameterTypes()[0] == UUID.class ? player.getUniqueId() : player;
         Object data = this.getMethod.invoke(manager, arg);
         if (data == null) {
            return null;
         }

         Object id = this.equippedMethod.invoke(data);
         return id == null ? null : id.toString();
      } catch (Throwable t) {
         return null;
      }
   }

   public void contribute(Player player, StatMap out, ProwessConfig config) {
      String id = this.equipped(player);
      if (id != null && !id.isBlank()) {
         StatMap bonus = config.titleStats(id);
         if (bonus != null) {
            out.addAll(bonus);
         }
      }
   }

   public List<String> allTitleIds() {
      this.connect();
      List<String> out = new ArrayList<>();
      if (this.epithet != null && this.titlesMethod != null) {
         try {
            Object registry = this.titlesMethod.invoke(this.epithet);

            for (String name : new String[]{"all", "titles", "values", "asMap"}) {
               try {
                  Method m = registry.getClass().getMethod(name);
                  Object v = m.invoke(registry);
                  if (v instanceof Map<?, ?> map) {
                     map.keySet().forEach(k -> out.add(String.valueOf(k)));
                     return out;
                  }

                  if (v instanceof Iterable) {
                     for (Object o : (Iterable)v) {
                        out.add(idOf(o));
                     }

                     return out;
                  }
               } catch (NoSuchMethodException var12) {
               }
            }
         } catch (Throwable var13) {
         }

         return out;
      } else {
         return out;
      }
   }

   private static String idOf(Object title) {
      try {
         return String.valueOf(title.getClass().getMethod("id").invoke(title));
      } catch (Throwable t) {
         return String.valueOf(title);
      }
   }

   public boolean connected() {
      this.connect();
      return this.epithet != null;
   }

   public void reset() {
      this.tried = false;
      this.epithet = null;
   }
}
