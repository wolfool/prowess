package com.wolfool.prowess.skill;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum SkillType {
   CLEAVE("참격", "&c", "검기가 앞으로 날아간다"),
   WHIRL("회전베기", "&6", "세 바퀴 돌며 둘레를 벤다"),
   DASH("돌진", "&e", "잔상을 남기며 꿰뚫고 지나간다"),
   LEAP("도약", "&a", "뛰어올랐다 내리찍는다"),
   EMPOWER("각성", "&d", "잠깐 크게 강해진다"),
   PIERCE("꿰뚫기", "&b", "한 줄에 선 것을 다 꿰뚫는다"),
   NOVA("빙결", "&b", "얼음이 터지며 얼려 붙인다"),
   METEOR("운석", "&6", "하늘에서 불덩이가 떨어진다"),
   CHAIN("연쇄", "&e", "번개가 튀어 옮겨 간다"),
   VORTEX("소용돌이", "&5", "끌어당기며 계속 벤다"),
   FISSURE("균열", "&8", "땅이 갈라지며 앞으로 나아간다"),
   EXECUTE("처형", "&4", "다친 것일수록 크게 들어간다"),
   GUARD("수호", "&f", "잠깐 단단해진다"),
   UPPERCUT("띄우기", "&e", "띄워 올리고 연달아 벤다"),
   BLOOD_REND("혈참", "&4", "파고들며 세 번 그어 출혈을 쌓는다"),
   SHADOW_STEP("그림자 걸음", "&8", "등 뒤로 돌아 들어간다"),
   BLOOD_BRAND("피의 낙인", "&c", "낙인을 찍고 터뜨린다"),
   CRIMSON_WALTZ("처형 무도", "&4", "순간이동하며 베고 마지막에 터뜨린다"),
   MYTHIC("마법", "&d", "MythicMobs 스킬을 쏜다"),
   BARRAGE("연사", "&f", "화살을 한꺼번에 쏟아낸다"),
   HEAT_WAVE("열파", "&6", "불 초승달을 날린다 (3연격)"),
   FIRE_BRAND("화염 낙인", "&c", "낙인을 찍고 2초 뒤 터뜨린다"),
   ASCEND("업화", "&6", "뛰어올라 내리찍는다"),
   BLAZE_RELEASE("극염 해방", "&b", "화염진에서 푸른 불기둥이 솟는다");

   private final String korean;
   private final String color;
   private final String blurb;

   SkillType(String korean, String color, String blurb) {
      this.korean = korean;
      this.color = color;
      this.blurb = blurb;
   }

   public String korean() {
      return this.korean;
   }

   public String color() {
      return this.color;
   }

   public String blurb() {
      return this.blurb;
   }

   @Nullable
   public static SkillType byName(@Nullable String raw) {
      if (raw != null && !raw.isBlank()) {
         String s = raw.trim();

         for (SkillType t : values()) {
            if (t.korean.equals(s) || t.name().equalsIgnoreCase(s)) {
               return t;
            }
         }

         try {
            return valueOf(s.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException e) {
            return null;
         }
      } else {
         return null;
      }
   }
}
