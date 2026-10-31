package com.wolfool.prowess.skill;

import com.wolfool.prowess.integration.CraftEngineHook;
import com.wolfool.prowess.util.Configs;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class SkillRegistry {
   private final JavaPlugin plugin;
   private final Map<String, Skill> byId = new LinkedHashMap<>();
   private final Map<String, WeaponSkills> byWeapon = new LinkedHashMap<>();

   public SkillRegistry(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public void reload() {
      this.byId.clear();
      this.byWeapon.clear();
      Configs.upgrade(this.plugin, "skills.yml");
      File file = new File(this.plugin.getDataFolder(), "skills.yml");
      YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
      ConfigurationSection root = y.getConfigurationSection("skills");
      if (root == null) {
         this.plugin.getLogger().warning("skills.yml 에 skills 가 없습니다. 무기 스킬 없이 돕니다.");
      } else {
         for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s != null) {
               SkillType type = SkillType.byName(s.getString("type"));
               if (type == null) {
                  this.plugin.getLogger().warning("스킬 '" + id + "' 의 type 이 올바르지 않습니다: " + s.getString("type"));
               } else {
                  this.byId
                     .put(
                        id,
                        new Skill(
                           id,
                           s.getString("name", id),
                           type,
                           s.getDouble("cooldown", 8.0),
                           s.getDouble("power", 1.5),
                           s.getDouble("range", 4.0),
                           s.getDouble("duration", 5.0),
                           new ArrayList<>(s.getStringList("lore")),
                           s.getString("mythic-skill", ""),
                           s.getDouble("bleed", 3.0)
                        )
                     );
               }
            }
         }

         Map<String, String> abilityMap = new LinkedHashMap<>();
         ConfigurationSection mapSec = y.getConfigurationSection("ability-map");
         if (mapSec != null) {
            for (String alias : mapSec.getKeys(false)) {
               abilityMap.put(alias.toLowerCase(Locale.ROOT), mapSec.getString(alias));
            }
         }

         ConfigurationSection weapons = y.getConfigurationSection("weapons");
         if (weapons != null) {
            for (String weapon : weapons.getKeys(false)) {
               ConfigurationSection w = weapons.getConfigurationSection(weapon);
               if (w != null) {
                  WeaponSkills ws = new WeaponSkills(weapon);

                  for (String bindKey : w.getKeys(false)) {
                     if (bindKey.toLowerCase(Locale.ROOT).startsWith("ability")) {
                        // MMOItems 형식: ability0: { type: SK_C_LC, mode: LEFT_CLICK, cooldown: 0.1, damage: 5 }
                        ConfigurationSection ab = w.getConfigurationSection(bindKey);
                        if (ab != null) {
                           String type = ab.getString("type");
                           String mapped = type == null ? null : abilityMap.getOrDefault(type.toLowerCase(Locale.ROOT), type);
                           Skill abSkill = mapped == null ? null : this.byId.get(mapped);
                           Bind abBind = Bind.byName(ab.getString("mode"));
                           if (abSkill == null) {
                              this.plugin.getLogger().warning("무기 '" + weapon + "' 의 '" + bindKey + "' 스킬 '" + type + "' 을 못 찾았습니다 (ability-map).");
                           } else if (abBind == null) {
                              this.plugin.getLogger().warning("무기 '" + weapon + "' 의 '" + bindKey + "' mode '" + ab.getString("mode") + "' 가 올바르지 않습니다.");
                           } else {
                              double cd = ab.getDouble("cooldown", -1.0);
                              Skill use = cd < 0.0 ? abSkill : new Skill(abSkill.id(), abSkill.name(), abSkill.type(), cd, abSkill.power(), abSkill.range(), abSkill.duration(), abSkill.lore(), abSkill.mythicSkill(), abSkill.bleed());
                              ws.put(abBind, use, ab.getInt("unlock", 0));
                           }
                        }
                        continue;
                     }

                     Bind bind = Bind.byName(bindKey);
                     if (bind == null) {
                        this.plugin.getLogger().warning("무기 '" + weapon + "' 의 칸 '" + bindKey + "' 이 올바르지 않습니다 (우클릭 · 웅크리기+우클릭 · 좌클릭 · 웅크리기+좌클릭).");
                     } else {
                        ConfigurationSection b = w.getConfigurationSection(bindKey);
                        String skillId = b != null ? b.getString("skill") : w.getString(bindKey);
                        Skill skill = skillId == null ? null : this.byId.get(skillId);
                        if (skill == null) {
                           this.plugin.getLogger().warning("무기 '" + weapon + "' 의 '" + bindKey + "' 에 걸린 스킬 '" + skillId + "' 이 없습니다.");
                        } else {
                           int unlock = b != null ? b.getInt("unlock", bind.defaultUnlock()) : bind.defaultUnlock();
                           ws.put(bind, skill, unlock);
                        }
                     }
                  }

                  if (!ws.all().isEmpty()) {
                     this.byWeapon.put(weapon.toLowerCase(Locale.ROOT), ws);
                  }
               }
            }
         }

         int bound = this.byWeapon.values().stream().mapToInt(wx -> wx.all().size()).sum();
         this.plugin.getLogger().info("무기 스킬 " + this.byId.size() + "종 · 무기 " + this.byWeapon.size() + "개 · 걸린 칸 " + bound + "개");
      }
   }

   @Nullable
   public WeaponSkills forItem(@Nullable ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         String id = CraftEngineHook.idOf(item);
         if (id != null) {
            WeaponSkills w = this.byWeapon.get(id.toLowerCase(Locale.ROOT));
            if (w != null) {
               return w;
            }
         }

         return this.byWeapon.get(item.getType().name().toLowerCase(Locale.ROOT));
      } else {
         return null;
      }
   }

   @Nullable
   public Skill byId(String id) {
      return this.byId.get(id);
   }

   public Collection<Skill> all() {
      return this.byId.values();
   }

   public Map<String, WeaponSkills> weapons() {
      return this.byWeapon;
   }

   public int size() {
      return this.byId.size();
   }
}
