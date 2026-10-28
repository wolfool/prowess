package com.wolfool.prowess.stat;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Stat {
   ATTACK("공격력", "&c", false, 0.0, "⚔"),
   DEFENSE("방어", "&9", false, 0.0, "\ud83d\udee1"),
   HEALTH("최대 체력", "&c", false, 0.0, "❤"),
   CRIT_CHANCE("치명타 확률", "&e", true, 0.0, "✧"),
   CRIT_DAMAGE("치명타 피해", "&6", true, 50.0, "✦"),
   PIERCE("관통", "&d", true, 0.0, "➶"),
   LIFESTEAL("흡혈", "&4", true, 0.0, "❥"),
   ATTACK_SPEED("공격 속도", "&f", true, 0.0, "⌚"),
   MOVE_SPEED("이동 속도", "&b", true, 0.0, "➤");

   private final String korean;
   private final String color;
   private final boolean percent;
   private final double base;
   private final String icon;

   Stat(String korean, String color, boolean percent, double base, String icon) {
      this.korean = korean;
      this.color = color;
      this.percent = percent;
      this.base = base;
      this.icon = icon;
   }

   public String korean() {
      return this.korean;
   }

   public String color() {
      return this.color;
   }

   public boolean percent() {
      return this.percent;
   }

   public double base() {
      return this.base;
   }

   public String icon() {
      return this.icon;
   }

   public String format(double value) {
      String n = value == Math.floor(value) ? String.valueOf((long)value) : String.format("%.1f", value);
      return this.percent ? n + "%" : n;
   }

   @Nullable
   public static Stat byName(@Nullable String raw) {
      if (raw != null && !raw.isBlank()) {
         String s = raw.trim();

         for (Stat stat : values()) {
            if (stat.korean.equals(s)) {
               return stat;
            }

            if (stat.name().equalsIgnoreCase(s)) {
               return stat;
            }

            if (stat.name().replace("_", "").equalsIgnoreCase(s.replace("-", "").replace("_", ""))) {
               return stat;
            }
         }

         String key = s.toUpperCase(Locale.ROOT).replace('-', '_');

         for (Stat stat : values()) {
            if (stat.name().equals(key)) {
               return stat;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
