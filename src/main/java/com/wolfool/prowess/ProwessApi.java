package com.wolfool.prowess;

import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatMap;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class ProwessApi {
   private static ProwessConfig config;

   private ProwessApi() {
   }

   static void bind(ProwessConfig cfg) {
      config = cfg;
   }

   static void unbind() {
      config = null;
   }

   public static boolean isReady() {
      return config != null;
   }

   public static boolean titleHasStats(@Nullable String titleId) {
      if (config != null && titleId != null) {
         StatMap map = config.titleStats(titleId);
         return map != null && !map.isEmpty();
      } else {
         return false;
      }
   }

   public static List<String> titleStatLines(@Nullable String titleId) {
      if (config != null && titleId != null) {
         StatMap map = config.titleStats(titleId);
         return map == null ? List.of() : map.lines("");
      } else {
         return List.of();
      }
   }

   public static double titleStat(@Nullable String titleId, @Nullable String statName) {
      if (config != null && titleId != null) {
         Stat stat = Stat.byName(statName);
         if (stat == null) {
            return 0.0;
         }

         StatMap map = config.titleStats(titleId);
         return map == null ? 0.0 : map.get(stat);
      } else {
         return 0.0;
      }
   }

   public static List<String> statNames() {
      List<String> out = new ArrayList<>();

      for (Stat s : Stat.values()) {
         out.add(s.korean());
      }

      return out;
   }
}
