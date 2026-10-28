package com.wolfool.prowess.gear;

import com.wolfool.prowess.skill.Bind;
import com.wolfool.prowess.skill.SkillRegistry;
import com.wolfool.prowess.skill.WeaponSkills;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatMap;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class GearStats {
   private static final String MARK = "§8§m                    ";
   private final NamespacedKey statsKey;
   private final NamespacedKey levelKey;
   private @Nullable SkillRegistry skills;

   public GearStats(Plugin plugin) {
      this.statsKey = new NamespacedKey(plugin, "stats");
      this.levelKey = new NamespacedKey(plugin, "level");
   }

   public void useSkills(SkillRegistry registry) {
      this.skills = registry;
   }

   public StatMap raw(@Nullable ItemStack item) {
      StatMap map = StatMap.empty();
      if (item != null && !item.getType().isAir()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return map;
         }

         String packed = (String)meta.getPersistentDataContainer().get(this.statsKey, PersistentDataType.STRING);
         if (packed != null && !packed.isBlank()) {
            for (String part : packed.split(";")) {
               int eq = part.indexOf(61);
               if (eq > 0) {
                  Stat stat = Stat.byName(part.substring(0, eq));
                  if (stat != null) {
                     try {
                        map.add(stat, Double.parseDouble(part.substring(eq + 1)));
                     } catch (NumberFormatException var12) {
                     }
                  }
               }
            }

            return map;
         } else {
            return map;
         }
      } else {
         return map;
      }
   }

   public int level(@Nullable ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return 0;
         }

         Integer v = (Integer)meta.getPersistentDataContainer().get(this.levelKey, PersistentDataType.INTEGER);
         return v == null ? 0 : v;
      } else {
         return 0;
      }
   }

   public boolean isGear(@Nullable ItemStack item) {
      return !this.raw(item).isEmpty();
   }

   public StatMap effective(@Nullable ItemStack item, double perLevel) {
      StatMap base = this.raw(item);
      if (base.isEmpty()) {
         return base;
      }

      StatMap out = StatMap.empty().addAll(base);
      out.addScaled(base, this.level(item) * perLevel);
      return out;
   }

   public ItemStack write(ItemStack item, StatMap stats, int level, double perLevel) {
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      PersistentDataContainer pdc = meta.getPersistentDataContainer();
      StringBuilder sb = new StringBuilder();
      stats.asMap().forEach((s, v) -> {
         if (v != 0.0) {
            if (sb.length() > 0) {
               sb.append(';');
            }

            sb.append(s.name()).append('=').append(v);
         }
      });
      if (sb.length() == 0) {
         pdc.remove(this.statsKey);
      } else {
         pdc.set(this.statsKey, PersistentDataType.STRING, sb.toString());
      }

      if (level > 0) {
         pdc.set(this.levelKey, PersistentDataType.INTEGER, level);
      } else {
         pdc.remove(this.levelKey);
      }

      item.setItemMeta(meta);
      return this.relore(item, perLevel);
   }

   public ItemStack setLevel(ItemStack item, int level, double perLevel) {
      return this.write(item, this.raw(item), level, perLevel);
   }

   public ItemStack relore(ItemStack item, double perLevel) {
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      List<String> kept = new ArrayList<>();
      List<String> old = meta.getLore();
      if (old != null) {
         for (String line : old) {
            if (line.startsWith("§8§m                    ")) {
               break;
            }

            kept.add(line);
         }
      }

      StatMap eff = this.effective(item, perLevel);
      int level = this.level(item);
      if (!eff.isEmpty()) {
         kept.add("§8§m                    ");

         for (String line : eff.lines("")) {
            kept.add(color(line));
         }

         if (level > 0) {
            kept.add(color("&8단계 &e+" + level));
         }
      }

      this.appendSkills(kept, item, level);
      meta.setLore(kept);
      meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
      String frame = frameFor(level);
      if (frame != null) {
         meta.setTooltipStyle(NamespacedKey.minecraft(frame));
      }

      item.setItemMeta(meta);
      return item;
   }

   private void appendSkills(List<String> lore, ItemStack item, int level) {
      if (this.skills != null) {
         WeaponSkills weapon = this.skills.forItem(item);
         if (weapon != null) {
            boolean first = true;

            for (Bind bind : Bind.values()) {
               WeaponSkills.Slot slot = weapon.at(bind);
               if (slot != null) {
                  if (first) {
                     lore.add(color("&8"));
                     lore.add(color("&8▸ 스킬"));
                     first = false;
                  }

                  if (level < slot.unlock()) {
                     lore.add(color(" &8" + bind.tag() + "  \ud83d\udd12 &7+" + slot.unlock() + " &8부터"));
                  } else {
                     lore.add(color(" " + bind.color() + bind.tag() + "  &f" + slot.skill().name()));

                     for (String line : slot.skill().lore()) {
                        lore.add(color("   " + line));
                     }
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static String frameFor(int level) {
      if (level <= 3) {
         return null;
      } else if (level <= 6) {
         return "uncommon";
      } else if (level <= 9) {
         return "rare";
      } else {
         return level <= 12 ? "epic" : "legend";
      }
   }

   public Component nameWithLevel(ItemStack item) {
      int level = this.level(item);
      ItemMeta meta = item.getItemMeta();
      Component name = (Component)(meta != null && meta.hasDisplayName() ? meta.displayName() : Component.translatable(item.getType().translationKey()));
      if (name == null) {
         name = Component.text(item.getType().name());
      }

      return level <= 0 ? name : name.append(Component.text(" +" + level).color(NamedTextColor.YELLOW));
   }

   private static String color(String legacy) {
      return ChatColor.translateAlternateColorCodes('&', legacy);
   }
}
