package com.wolfool.prowess.stat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StatMap {
   private final Map<Stat, Double> values = new EnumMap<>(Stat.class);

   public static StatMap empty() {
      return new StatMap();
   }

   public static StatMap base() {
      StatMap m = new StatMap();

      for (Stat s : Stat.values()) {
         if (s.base() != 0.0) {
            m.add(s, s.base());
         }
      }

      return m;
   }

   public double get(Stat stat) {
      return this.values.getOrDefault(stat, 0.0);
   }

   public StatMap add(Stat stat, double amount) {
      if (amount == 0.0) {
         return this;
      }

      this.values.merge(stat, amount, Double::sum);
      return this;
   }

   public StatMap addAll(StatMap other) {
      other.values.forEach(this::add);
      return this;
   }

   public StatMap addScaled(StatMap other, double factor) {
      if (factor == 0.0) {
         return this;
      }

      other.values.forEach((s, v) -> this.add(s, v * factor));
      return this;
   }

   public boolean isEmpty() {
      return this.values.values().stream().allMatch(v -> v == 0.0);
   }

   public Map<Stat, Double> asMap() {
      return new EnumMap<>(this.values);
   }

   public List<String> lines(String prefix) {
      List<String> out = new ArrayList<>();

      for (Stat s : Stat.values()) {
         double v = this.get(s);
         if (v != 0.0) {
            out.add(prefix + s.color() + s.korean() + " &f" + (v > 0.0 ? "+" : "") + s.format(v));
         }
      }

      return out;
   }

   @Override
   public String toString() {
      return this.values.toString();
   }
}
