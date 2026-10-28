package com.wolfool.prowess.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public final class RawDamage {
   private static final Set<UUID> PENDING = new HashSet<>();

   private RawDamage() {
   }

   public static void deal(LivingEntity target, double amount, Entity by) {
      if (!(amount <= 0.0) && !target.isDead()) {
         UUID id = target.getUniqueId();
         PENDING.add(id);

         try {
            target.setNoDamageTicks(0);
            target.damage(amount, by);
         } finally {
            PENDING.remove(id);
         }
      }
   }

   static boolean isRaw(LivingEntity target) {
      return PENDING.contains(target.getUniqueId());
   }
}
