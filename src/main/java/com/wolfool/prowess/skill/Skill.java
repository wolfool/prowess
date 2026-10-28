package com.wolfool.prowess.skill;

import java.util.List;

public record Skill(
   String id, String name, SkillType type, double cooldown, double power, double range, double duration, List<String> lore, String mythicSkill, double bleed
) {
   public Skill {
      lore = List.copyOf(lore);
   }
}
