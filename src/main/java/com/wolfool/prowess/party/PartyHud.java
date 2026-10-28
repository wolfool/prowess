package com.wolfool.prowess.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class PartyHud {
   private static final String[] KEYS = new String[]{"§0§r", "§1§r", "§2§r", "§3§r", "§4§r", "§5§r", "§6§r", "§7§r"};
   private final Plugin plugin;
   private final PartyService parties;
   private final Map<UUID, Scoreboard> boards = new HashMap<>();

   public PartyHud(Plugin plugin, PartyService parties) {
      this.plugin = plugin;
      this.parties = parties;
      Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 10L);
   }

   private void tick() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         Party party = this.parties.of(p);
         if (party != null && party.size() >= 2) {
            this.show(p, party);
         } else {
            this.hide(p);
         }
      }
   }

   private void show(Player viewer, Party party) {
      Scoreboard board = this.boards.get(viewer.getUniqueId());
      if (board == null) {
         board = Bukkit.getScoreboardManager().getNewScoreboard();
         Objective obj = board.registerNewObjective("prowess_party", Criteria.DUMMY, Component.text("파 티", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
         obj.setDisplaySlot(DisplaySlot.SIDEBAR);
         this.boards.put(viewer.getUniqueId(), board);
      }

      Objective obj = board.getObjective("prowess_party");
      if (obj != null) {
         List<UUID> order = new ArrayList<>(party.ordered());
         order.remove(viewer.getUniqueId());
         order.add(0, viewer.getUniqueId());
         int line = order.size();

         for (int i = 0; i < order.size() && i < KEYS.length; i++) {
            UUID who = order.get(i);
            String key = KEYS[i];
            Team team = board.getTeam("p" + i);
            if (team == null) {
               team = board.registerNewTeam("p" + i);
            }

            if (!team.hasEntry(key)) {
               team.addEntry(key);
            }

            team.prefix(this.lineFor(who, party, who.equals(viewer.getUniqueId())));
            obj.getScore(key).setScore(line--);
         }

         for (int i = order.size(); i < KEYS.length; i++) {
            board.resetScores(KEYS[i]);
         }

         if (viewer.getScoreboard() != board) {
            viewer.setScoreboard(board);
         }
      }
   }

   private Component lineFor(UUID who, Party party, boolean self) {
      Player p = Bukkit.getPlayer(who);
      boolean leader = party.isLeader(who);
      Component mark = Component.text(leader ? " ★ " : "   ", leader ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY);
      Component name = Component.text(self ? "나" : this.parties.nameOf(who), self ? NamedTextColor.WHITE : NamedTextColor.GRAY);
      if (p != null && p.isOnline()) {
         int hp = (int)Math.ceil(p.getHealth());
         AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
         int hpMax = max == null ? 20 : (int)Math.round(max.getValue());
         double ratio = hpMax <= 0 ? 0.0 : (double)hp / hpMax;
         NamedTextColor hpColor = ratio <= 0.25 ? NamedTextColor.RED : (ratio <= 0.5 ? NamedTextColor.YELLOW : NamedTextColor.GREEN);
         return mark.append(name).append(Component.text("  " + hp, hpColor)).append(Component.text(" / " + hpMax, NamedTextColor.DARK_GRAY));
      } else {
         return mark.append(name).append(Component.text("  접속 끊김", NamedTextColor.DARK_GRAY));
      }
   }

   private void hide(Player viewer) {
      if (this.boards.remove(viewer.getUniqueId()) != null) {
         viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
      }
   }

   public void forget(Player player) {
      this.boards.remove(player.getUniqueId());
   }

   public void clear() {
      for (UUID who : List.copyOf(this.boards.keySet())) {
         Player p = Bukkit.getPlayer(who);
         if (p != null) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
         }
      }

      this.boards.clear();
   }
}
