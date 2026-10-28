package com.wolfool.prowess.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Msg {
   private static final DecimalFormat COMMA = new DecimalFormat("#,##0");

   private Msg() {
   }

   public static Component of(String legacy) {
      return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy).decoration(TextDecoration.ITALIC, false);
   }

   public static List<Component> lines(List<String> legacy) {
      List<Component> out = new ArrayList<>(legacy.size());

      for (String s : legacy) {
         out.add(of(s));
      }

      return out;
   }

   public static String comma(long v) {
      return COMMA.format(v);
   }

   public static String trim(double v) {
      return v == Math.floor(v) ? String.valueOf((long)v) : String.format("%.1f", v);
   }

   public static String percent(double ratio) {
      return Math.round(ratio * 100.0) + "%";
   }
}
