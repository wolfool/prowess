package com.wolfool.prowess.combat;

public final class Formula {
   private Formula() {
   }

   public static double reduction(double defense, double pierce, double k) {
      double effective = Math.max(0.0, defense) * (1.0 - clamp01(pierce / 100.0));
      return effective <= 0.0 ? 0.0 : effective / (effective + Math.max(1.0, k));
   }

   public static double afterDefense(double damage, double defense, double pierce, double k) {
      return damage * (1.0 - reduction(defense, pierce, k));
   }

   public static double withCrit(double damage, double critDamage) {
      return damage * (1.0 + Math.max(0.0, critDamage) / 100.0);
   }

   public static boolean roll(double chancePercent) {
      if (chancePercent <= 0.0) {
         return false;
      } else {
         return chancePercent >= 100.0 ? true : Math.random() * 100.0 < chancePercent;
      }
   }

   public static double effectiveHealthFactor(double defense, double k) {
      return 1.0 + Math.max(0.0, defense) / Math.max(1.0, k);
   }

   private static double clamp01(double v) {
      return v < 0.0 ? 0.0 : Math.min(v, 1.0);
   }
}
