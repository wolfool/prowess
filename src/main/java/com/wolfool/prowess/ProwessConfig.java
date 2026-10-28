package com.wolfool.prowess;

import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.util.Configs;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class ProwessConfig {
   private final JavaPlugin plugin;
   private double defenseK = 100.0;
   private double defensePerArmorPoint = 4.0;
   private double defensePerToughness = 6.0;
   private double enhancePerLevel = 0.1;
   private double mobDefensePerArmor = 3.0;
   private double pvpFactor = 0.5;
   private int partyMax = 3;
   private boolean partyFriendlyFire = false;
   private List<String> dummyMobs = List.of("Target_Dummy");
   private final Map<String, StatMap> jobPerLevel = new LinkedHashMap<>();
   private int maxLevel = 15;
   private int safeLevel = 3;
   private boolean downgradeOnFail = true;
   private final List<ProwessConfig.Step> steps = new ArrayList<>();
   private final Map<String, ProwessConfig.GearEntry> byCraftEngineId = new LinkedHashMap<>();
   private final Map<Material, ProwessConfig.GearEntry> byMaterial = new LinkedHashMap<>();
   private final Map<String, String> accessoryKinds = new LinkedHashMap<>();
   private final Map<String, StatMap> titleStats = new LinkedHashMap<>();

   public ProwessConfig(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public void reload() {
      Configs.upgrade(this.plugin, "config.yml");
      this.plugin.reloadConfig();
      FileConfiguration cfg = this.plugin.getConfig();
      this.defenseK = cfg.getDouble("combat.defense-k", 100.0);
      this.defensePerArmorPoint = cfg.getDouble("combat.defense-per-armor-point", 4.0);
      this.defensePerToughness = cfg.getDouble("combat.defense-per-toughness", 6.0);
      this.mobDefensePerArmor = cfg.getDouble("combat.mob-defense-per-armor", 3.0);
      this.pvpFactor = cfg.getDouble("combat.pvp-factor", 0.5);
      this.partyMax = Math.max(2, cfg.getInt("party.max-size", 3));
      this.partyFriendlyFire = cfg.getBoolean("party.friendly-fire", false);
      this.dummyMobs = cfg.getStringList("combat.dummy-mobs");
      if (this.dummyMobs.isEmpty()) {
         this.dummyMobs = List.of("Target_Dummy");
      }

      this.jobPerLevel.clear();
      ConfigurationSection jobs = cfg.getConfigurationSection("jobs");
      if (jobs != null) {
         for (String job : jobs.getKeys(false)) {
            ConfigurationSection sec = jobs.getConfigurationSection(job);
            if (sec != null) {
               this.jobPerLevel.put(job, this.readStats(sec, "jobs." + job));
            }
         }
      }

      this.titleStats.clear();
      ConfigurationSection titles = cfg.getConfigurationSection("titles");
      if (titles != null) {
         for (String id : titles.getKeys(false)) {
            ConfigurationSection sec = titles.getConfigurationSection(id);
            if (sec != null) {
               this.titleStats.put(id, this.readStats(sec, "titles." + id));
            }
         }
      }

      this.loadGear();
      this.plugin
         .getLogger()
         .info(
            "능력치 표 로드 — 아이템 "
               + (this.byCraftEngineId.size() + this.byMaterial.size())
               + "종 · 장신구 "
               + this.accessoryKinds.size()
               + "종 · 강화 "
               + this.steps.size()
               + "단계 · 직업 "
               + this.jobPerLevel.size()
               + "종 · 칭호 "
               + this.titleStats.size()
               + "종"
         );
   }

   private void loadGear() {
      Configs.upgrade(this.plugin, "gear.yml");
      File file = new File(this.plugin.getDataFolder(), "gear.yml");
      YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
      this.maxLevel = y.getInt("enhance.max-level", 15);
      this.safeLevel = y.getInt("enhance.safe-level", 3);
      this.enhancePerLevel = y.getDouble("enhance.per-level", 0.1);
      this.downgradeOnFail = y.getBoolean("enhance.downgrade-on-fail", true);
      this.steps.clear();
      ConfigurationSection st = y.getConfigurationSection("enhance.steps");
      if (st != null) {
         for (String key : st.getKeys(false)) {
            ConfigurationSection s = st.getConfigurationSection(key);
            if (s != null) {
               int level;
               try {
                  level = Integer.parseInt(key);
               } catch (NumberFormatException e) {
                  this.plugin.getLogger().warning("강화 단계 '" + key + "' 는 숫자가 아닙니다.");
                  continue;
               }

               Material fallback = Material.matchMaterial(s.getString("material-fallback", "PAPER").toUpperCase(Locale.ROOT));
               this.steps
                  .add(
                     new ProwessConfig.Step(
                        level,
                        s.getDouble("chance", 1.0),
                        s.getLong("cost", 0L),
                        s.getString("material", ""),
                        fallback == null ? Material.PAPER : fallback,
                        s.getInt("material-amount", 1)
                     )
                  );
            }
         }

         this.steps.sort(Comparator.comparingInt(ProwessConfig.Step::level));
      }

      this.byCraftEngineId.clear();
      this.byMaterial.clear();
      this.accessoryKinds.clear();
      ConfigurationSection acc = y.getConfigurationSection("accessories");
      if (acc != null) {
         for (String key : acc.getKeys(false)) {
            String kind = acc.getString(key);
            if (kind != null && !kind.isBlank()) {
               this.accessoryKinds.put(key.toLowerCase(Locale.ROOT), kind.trim().toLowerCase(Locale.ROOT));
            }
         }
      }

      ConfigurationSection items = y.getConfigurationSection("items");
      if (items != null) {
         for (String key : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(key);
            if (s != null) {
               StatMap stats = this.readStats(s, "gear.yml items." + key);
               boolean weapon = s.getBoolean(
                  "weapon", key.contains("sword") || key.contains("axe") || key.contains("bow") || key.contains("trident") || key.contains("mace")
               );
               ProwessConfig.GearEntry entry = new ProwessConfig.GearEntry(stats, weapon);
               if (key.indexOf(58) >= 0) {
                  this.byCraftEngineId.put(key.toLowerCase(Locale.ROOT), entry);
               } else {
                  Material m = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                  if (m == null) {
                     this.plugin.getLogger().warning("gear.yml 의 '" + key + "' 는 아이템 아이디도 재질도 아닙니다.");
                  } else {
                     this.byMaterial.put(m, entry);
                  }
               }
            }
         }
      }
   }

   private StatMap readStats(ConfigurationSection parent, String where) {
      StatMap map = StatMap.empty();
      ConfigurationSection sec = parent.getConfigurationSection("stats");
      if (sec == null) {
         return map;
      }

      for (String key : sec.getKeys(false)) {
         Stat stat = Stat.byName(key);
         if (stat == null) {
            this.plugin.getLogger().warning(where + " 의 '" + key + "' 는 없는 능력치입니다.");
         } else {
            map.add(stat, sec.getDouble(key));
         }
      }

      return map;
   }

   public double defenseK() {
      return this.defenseK;
   }

   public double defensePerArmorPoint() {
      return this.defensePerArmorPoint;
   }

   public double defensePerToughness() {
      return this.defensePerToughness;
   }

   public double mobDefensePerArmor() {
      return this.mobDefensePerArmor;
   }

   public double pvpFactor() {
      return this.pvpFactor;
   }

   public int partyMax() {
      return this.partyMax;
   }

   public boolean partyFriendlyFire() {
      return this.partyFriendlyFire;
   }

   public List<String> dummyMobs() {
      return this.dummyMobs;
   }

   public double enhancePerLevel() {
      return this.enhancePerLevel;
   }

   public int maxLevel() {
      return this.maxLevel;
   }

   public int safeLevel() {
      return this.safeLevel;
   }

   public boolean downgradeOnFail() {
      return this.downgradeOnFail;
   }

   public Map<String, StatMap> jobPerLevel() {
      return this.jobPerLevel;
   }

   @Nullable
   public ProwessConfig.Step step(int toLevel) {
      ProwessConfig.Step best = null;

      for (ProwessConfig.Step s : this.steps) {
         if (s.level() <= toLevel) {
            best = s;
         }
      }

      return best;
   }

   public List<ProwessConfig.Step> steps() {
      return this.steps;
   }

   @Nullable
   public String accessoryKind(@Nullable String craftEngineId) {
      return craftEngineId == null ? null : this.accessoryKinds.get(craftEngineId.toLowerCase(Locale.ROOT));
   }

   @Nullable
   public StatMap titleStats(@Nullable String titleId) {
      return titleId == null ? null : this.titleStats.get(titleId);
   }

   public Map<String, StatMap> allTitleStats() {
      return this.titleStats;
   }

   public void setTitleStat(String titleId, Stat stat, double value) {
      StatMap map = this.titleStats.computeIfAbsent(titleId, k -> StatMap.empty());
      map.add(stat, value - map.get(stat));
      this.plugin.getConfig().set("titles." + titleId + ".stats." + stat.korean(), value == 0.0 ? null : value);
      this.plugin.saveConfig();
   }

   public List<String> allGearKeys() {
      List<String> out = new ArrayList<>(this.byCraftEngineId.keySet());

      for (Material m : this.byMaterial.keySet()) {
         out.add(m.name());
      }

      return out;
   }

   public int accessoryCount() {
      return this.accessoryKinds.size();
   }

   @Nullable
   public ProwessConfig.GearEntry entryFor(@Nullable String craftEngineId, Material material) {
      if (craftEngineId != null) {
         ProwessConfig.GearEntry e = this.byCraftEngineId.get(craftEngineId.toLowerCase(Locale.ROOT));
         if (e != null) {
            return e;
         }
      }

      return this.byMaterial.get(material);
   }

   public record GearEntry(StatMap stats, boolean weapon) {
   }

   public record Step(int level, double chance, long cost, String materialId, Material materialFallback, int materialAmount) {
   }
}
