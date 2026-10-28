package com.wolfool.prowess.combat;

import com.wolfool.prowess.integration.MythicSkills;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

public final class DummyListener implements Listener {
   private static final long WINDOW_MS = 5000L;
   private static final double ROOM = 1024.0;
   private final Plugin plugin;
   private final MythicSkills mythic;
   private final Set<String> types;
   private final ConcurrentHashMap<UUID, DummyListener.Tally> tally = new ConcurrentHashMap<>();

   public DummyListener(Plugin plugin, MythicSkills mythic, List<String> types) {
      this.plugin = plugin;
      this.mythic = mythic;
      this.types = new HashSet<>();

      for (String t : types) {
         this.types.add(t.toLowerCase(Locale.ROOT));
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof LivingEntity victim) {
         if (this.isDummy(victim)) {
            double dealt = event.getFinalDamage();
            if (!(dealt <= 0.0)) {
               double max = this.ensureRoom(victim);
               victim.setHealth(max);
               if (dealt >= max) {
                  event.setDamage(max - 1.0);
               }

               Bukkit.getScheduler().runTask(this.plugin, () -> this.refill(victim));
               this.record(victim, dealt);
            }
         }
      }
   }

   private double ensureRoom(LivingEntity dummy) {
      AttributeInstance attr = dummy.getAttribute(Attribute.MAX_HEALTH);
      if (attr == null) {
         return dummy.getHealth();
      }

      if (attr.getBaseValue() < 1024.0) {
         attr.setBaseValue(1024.0);
      }

      return attr.getValue();
   }

   private void refill(LivingEntity dummy) {
      if (dummy.isValid()) {
         AttributeInstance max = dummy.getAttribute(Attribute.MAX_HEALTH);
         if (max != null) {
            dummy.setHealth(max.getValue());
         }

         dummy.setFireTicks(0);
      }
   }

   private void record(LivingEntity dummy, double dealt) {
      long now = System.currentTimeMillis();
      DummyListener.Tally t = this.tally.computeIfAbsent(dummy.getUniqueId(), k -> new DummyListener.Tally());
      if (now - t.last > 5000L) {
         t.total = 0.0;
         t.hits = 0;
         t.start = now;
      }

      t.total += dealt;
      t.hits++;
      t.last = now;
      double seconds = Math.max(0.5, (now - t.start) / 1000.0);
      double dps = t.total / seconds;
      dummy.customName(
         ((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)Component.text("허수아비", NamedTextColor.GRAY)
                              .append(Component.text("  ▏  ", NamedTextColor.DARK_GRAY)))
                           .append(Component.text(round(t.total), NamedTextColor.RED)))
                        .append(Component.text(" 피해", NamedTextColor.DARK_GRAY)))
                     .append(Component.text("  ▏  ", NamedTextColor.DARK_GRAY)))
                  .append(Component.text(t.hits + "회", NamedTextColor.GRAY)))
               .append(Component.text("  ▏  ", NamedTextColor.DARK_GRAY)))
            .append(Component.text("초당 " + round(dps), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
      );
      dummy.setCustomNameVisible(true);
   }

   private static String round(double v) {
      return v >= 100.0 ? String.format("%,d", Math.round(v)) : String.format("%.1f", v);
   }

   public boolean isDummy(Entity entity) {
      String type = this.mythic.mobType(entity);
      if (type != null) {
         return this.types.contains(type.toLowerCase(Locale.ROOT));
      }

      Component name = entity.customName();
      return name == null ? false : PlainTextComponentSerializer.plainText().serialize(name).contains("허수아비");
   }

   private static final class Tally {
      double total;
      int hits;
      long start;
      long last;
   }
}
