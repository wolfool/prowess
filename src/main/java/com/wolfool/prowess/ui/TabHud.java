package com.wolfool.prowess.ui;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.combat.Formula;
import com.wolfool.prowess.integration.MasteryHook;
import com.wolfool.prowess.stat.Stat;
import com.wolfool.prowess.stat.StatMap;
import com.wolfool.prowess.stat.StatService;
import com.wolfool.prowess.util.Economy;
import com.wolfool.prowess.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class TabHud {
   private static final long PERIOD = 20L;
   private final Plugin plugin;
   private final StatService stats;
   private final ProwessConfig config;
   private final MasteryHook mastery;
   private final Economy economy;

   public TabHud(Plugin plugin, StatService stats, ProwessConfig config, MasteryHook mastery, Economy economy) {
      this.plugin = plugin;
      this.stats = stats;
      this.config = config;
      this.mastery = mastery;
      this.economy = economy;
   }

   public void start() {
      (new BukkitRunnable() {
         public void run() {
            for (Player p : Bukkit.getOnlinePlayers()) {
               p.sendPlayerListHeaderAndFooter(TabHud.this.header(p), TabHud.this.footer(p));
            }
         }
      }).runTaskTimer(this.plugin, 20L, 20L);
   }

   private Component header(Player p) {
      StringBuilder sb = new StringBuilder("\n&8━━━━━━━━  &f&l기 량  &8━━━━━━━━\n\n  ");
      sb.append("&6").append(String.format("%,d", this.economy.balance(p))).append(this.economy.unit());
      sb.append("   &8·   ").append(this.job(p));
      sb.append('\n');
      return Msg.of(sb.toString());
   }

   private Component footer(Player p) {
      StatMap s = this.stats.of(p);
      double defense = s.get(Stat.DEFENSE);
      double cut = Formula.reduction(defense, 0.0, this.config.defenseK()) * 100.0;
      return Msg.of(
         String.format(
            "\n  &c공격 %.0f    &9방어 %.0f &8(-%.0f%%)    &e치명타 %.0f%%    &d치명 피해 %.0f%%\n",
            s.get(Stat.ATTACK),
            defense,
            cut,
            s.get(Stat.CRIT_CHANCE),
            s.get(Stat.CRIT_DAMAGE)
         )
      );
   }

   private String job(Player p) {
      if (!this.mastery.connected()) {
         return "&7무직";
      }

      String name = this.mastery.jobName(p);
      return name.isEmpty() ? "&7무직 &8(/직업)" : "&a" + name + " Lv." + this.mastery.levelOf(p, name);
   }
}
