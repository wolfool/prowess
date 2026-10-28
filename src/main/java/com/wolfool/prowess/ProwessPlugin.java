package com.wolfool.prowess;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.wolfool.prowess.accessory.Accessories;
import com.wolfool.prowess.accessory.AccessoryGui;
import com.wolfool.prowess.combat.DamageListener;
import com.wolfool.prowess.combat.DummyListener;
import com.wolfool.prowess.gear.Enhance;
import com.wolfool.prowess.gear.EnhanceListener;
import com.wolfool.prowess.gear.GearStats;
import com.wolfool.prowess.gear.WeaponGui;
import com.wolfool.prowess.integration.EpithetHook;
import com.wolfool.prowess.integration.MasteryHook;
import com.wolfool.prowess.integration.PapiExpansion;
import com.wolfool.prowess.party.PartyCommand;
import com.wolfool.prowess.party.PartyHud;
import com.wolfool.prowess.party.PartyService;
import com.wolfool.prowess.skill.SkillHud;
import com.wolfool.prowess.skill.SkillListener;
import com.wolfool.prowess.skill.SkillRegistry;
import com.wolfool.prowess.skill.SkillService;
import com.wolfool.prowess.skill.VfxProbe;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatGui;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.title.TitleGui;
import com.wolfool.prowess.ui.TabHud;
import com.wolfool.prowess.util.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProwessPlugin extends JavaPlugin implements Listener {
   private ProwessConfig config;
   private GearStats gearStats;
   private Accessories accessories;
   private MasteryHook mastery;
   private StatService stats;
   private Economy economy;
   private Enhance enhance;
   private SkillRegistry skills;
   private SkillService skillService;
   private StatGui statGui;
   private AccessoryGui accessoryGui;
   private WeaponGui weaponGui;
   private TitleGui titleGui;
   private EpithetHook epithet;
   private VfxProbe vfxProbe;
   private DummyListener dummies;
   private PartyService parties;
   private PartyHud partyHud;

   public VfxProbe vfxProbe() {
      return this.vfxProbe;
   }

   public DummyListener dummies() {
      return this.dummies;
   }

   public PartyService parties() {
      return this.parties;
   }

   public void onEnable() {
      this.saveDefaultConfig();
      this.config = new ProwessConfig(this);
      this.config.reload();
      ProwessApi.bind(this.config);
      this.gearStats = new GearStats(this);
      this.accessories = new Accessories(this, this.config);
      this.mastery = new MasteryHook(this);
      this.epithet = new EpithetHook(this);
      this.stats = new StatService(this, this.config, this.gearStats, this.accessories, this.mastery, this.epithet);
      this.economy = new Economy(this);
      this.economy.hookLater();
      this.enhance = new Enhance(this.config, this.gearStats, this.economy);
      this.skills = new SkillRegistry(this);
      this.gearStats.useSkills(this.skills);
      this.skills.reload();
      this.skillService = new SkillService(this, this.stats);
      this.statGui = new StatGui(this.stats, this.config);
      this.accessoryGui = new AccessoryGui(this, this.accessories, this.gearStats, this.stats);
      this.weaponGui = new WeaponGui(this.config, this.gearStats, this.skills);
      this.titleGui = new TitleGui(this.config, this.epithet, this.stats);
      this.parties = new PartyService(this, this.config);
      this.partyHud = new PartyHud(this, this.parties);
      PluginManager pm = this.getServer().getPluginManager();
      pm.registerEvents(this, this);
      DamageListener damage = new DamageListener(this.stats, this.config, this.skillService);
      damage.useParties(this.parties);
      pm.registerEvents(damage, this);
      this.dummies = new DummyListener(this, this.skillService.mythic(), this.config.dummyMobs());
      pm.registerEvents(this.dummies, this);
      pm.registerEvents(new EnhanceListener(this, this.enhance, this.stats), this);
      pm.registerEvents(new SkillListener(this.skills, this.skillService, this.gearStats), this);
      new SkillHud(this, this.skills, this.skillService, this.gearStats).start();
      new TabHud(this, this.stats, this.config, this.mastery, this.economy).start();
      this.vfxProbe = new VfxProbe(this);
      pm.registerEvents(this.statGui, this);
      pm.registerEvents(this.accessoryGui, this);
      pm.registerEvents(this.weaponGui, this);
      pm.registerEvents(this.titleGui, this);
      ProwessCommand command = new ProwessCommand(
         this,
         this.config,
         this.stats,
         this.enhance,
         this.accessories,
         this.skills,
         this.skillService,
         this.economy,
         this.statGui,
         this.accessoryGui,
         this.mastery,
         this.epithet,
         this.weaponGui,
         this.titleGui
      );

      for (String name : new String[]{"prowess", "stats", "enhance", "accessory"}) {
         PluginCommand cmd = this.getCommand(name);
         if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
         } else {
            this.getLogger().warning("plugin.yml 에 " + name + " 명령이 없습니다.");
         }
      }

      PluginCommand partyCmd = this.getCommand("party");
      if (partyCmd != null) {
         PartyCommand handler = new PartyCommand(this.parties, this.config);
         partyCmd.setExecutor(handler);
         partyCmd.setTabCompleter(handler);
      }

      if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
         try {
            new PapiExpansion(this, this.stats, this.skills, this.skillService, this.gearStats, this.economy).register();
            this.getLogger().info("플레이스홀더를 등록했습니다 — %prowess_…%");
         } catch (Throwable t) {
            this.getLogger().warning("플레이스홀더 등록 실패: " + t.getMessage());
         }
      }

      this.getServer().getOnlinePlayers().forEach(this.stats::refresh);
      this.getLogger().info("활성화 완료 — 능력치 " + Stat.values().length + "종 · 무기 스킬 " + this.skills.size() + "종 · 강화 " + this.config.maxLevel() + "단계");
   }

   public void onDisable() {
      ProwessApi.unbind();
      this.skillService.bleed().clear();
      if (this.partyHud != null) {
         this.partyHud.clear();
      }

      if (this.parties != null) {
         this.parties.clear();
      }

      this.getServer().getOnlinePlayers().forEach(p -> {
         this.stats.invalidate(p);
         this.stats.applyVanilla(p);
      });
   }

   public void reloadAll() {
      this.config.reload();
      this.skills.reload();
      this.mastery.reset();
      this.epithet.reset();
      this.stats.invalidateAll();
      this.getServer().getOnlinePlayers().forEach(this.stats::refresh);
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.stats.refreshLater(event.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.stats.invalidate(event.getPlayer());
      this.skillService.clear(event.getPlayer());
   }

   @EventHandler
   public void onRespawn(PlayerRespawnEvent event) {
      this.stats.refreshLater(event.getPlayer());
   }

   @EventHandler
   public void onHeld(PlayerItemHeldEvent event) {
      this.stats.refreshLater(event.getPlayer());
   }

   @EventHandler
   public void onArmorChange(PlayerArmorChangeEvent event) {
      this.stats.refreshLater(event.getPlayer());
   }

   @EventHandler
   public void onBreak(PlayerItemBreakEvent event) {
      this.stats.refreshLater(event.getPlayer());
   }

   @EventHandler
   public void onPickup(EntityPickupItemEvent event) {
      if (event.getEntity() instanceof Player p) {
         this.stats.refreshLater(p);
      }
   }

   @EventHandler
   public void onCloseInventory(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player p) {
         this.stats.refreshLater(p);
      }
   }

   public StatService stats() {
      return this.stats;
   }

   public GearStats gearStats() {
      return this.gearStats;
   }
}
