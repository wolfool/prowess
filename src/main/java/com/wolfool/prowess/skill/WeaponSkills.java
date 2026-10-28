package com.wolfool.prowess.skill;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class WeaponSkills {
   private final String weapon;
   private final Map<Bind, WeaponSkills.Slot> slots = new EnumMap<>(Bind.class);

   public WeaponSkills(String weapon) {
      this.weapon = weapon;
   }

   public String weapon() {
      return this.weapon;
   }

   public void put(Bind bind, Skill skill, int unlock) {
      this.slots.put(bind, new WeaponSkills.Slot(skill, unlock));
   }

   @Nullable
   public WeaponSkills.Slot at(Bind bind) {
      return this.slots.get(bind);
   }

   public Map<Bind, WeaponSkills.Slot> all() {
      return this.slots;
   }

   public boolean unlocked(Bind bind, int enhanceLevel) {
      WeaponSkills.Slot s = this.slots.get(bind);
      return s != null && enhanceLevel >= s.unlock();
   }

   @Nullable
   public Bind nextLocked(int enhanceLevel) {
      Bind best = null;
      int bestAt = Integer.MAX_VALUE;

      for (Entry<Bind, WeaponSkills.Slot> e : this.slots.entrySet()) {
         int need = e.getValue().unlock();
         if (need > enhanceLevel && need < bestAt) {
            bestAt = need;
            best = e.getKey();
         }
      }

      return best;
   }

   public record Slot(Skill skill, int unlock) {
   }
}
