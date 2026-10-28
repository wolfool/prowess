package com.wolfool.prowess.skill;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum Bind {
   RIGHT("우클릭", "&e", 0),
   SHIFT_RIGHT("웅크리기 + 우클릭", "&6", 4),
   LEFT("좌클릭", "&b", 8),
   SHIFT_LEFT("웅크리기 + 좌클릭", "&d", 12);

   private final String korean;
   private final String color;
   private final int defaultUnlock;

   Bind(String korean, String color, int defaultUnlock) {
      this.korean = korean;
      this.color = color;
      this.defaultUnlock = defaultUnlock;
   }

   public String korean() {
      return this.korean;
   }

   public String color() {
      return this.color;
   }

   public int defaultUnlock() {
      return this.defaultUnlock;
   }

   public String tag() {
      return switch (this) {
         case RIGHT -> "우클";
         case SHIFT_RIGHT -> "쉬프트+우클";
         case LEFT -> "좌클";
         case SHIFT_LEFT -> "쉬프트+좌클";
      };
   }

   @Nullable
   public static Bind byName(@Nullable String raw) {
      if (raw != null && !raw.isBlank()) {
         String s = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "-");

         return switch (s) {
            case "right", "우클릭", "우" -> RIGHT;
            case "shift-right", "shiftright", "웅크리기+우클릭", "쉬프트우클릭", "쉬우" -> SHIFT_RIGHT;
            case "left", "좌클릭", "좌" -> LEFT;
            case "shift-left", "shiftleft", "웅크리기+좌클릭", "쉬프트좌클릭", "쉬좌" -> SHIFT_LEFT;
            default -> null;
         };
      } else {
         return null;
      }
   }
}
