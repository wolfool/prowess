package com.wolfool.prowess.party;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PartyCommand implements CommandExecutor, TabCompleter {
   private final PartyService parties;
   private final ProwessConfig config;

   public PartyCommand(PartyService parties, ProwessConfig config) {
      this.parties = parties;
      this.config = config;
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
      if (sender instanceof Player player) {
         if (args.length == 0) {
            this.info(player, label);
            return true;
         }

         switch (args[0].toLowerCase(Locale.ROOT)) {
            case "초대":
            case "invite":
            case "부르기":
               Player tox = this.find(player, args);
               if (tox != null) {
                  this.parties.invite(player, tox);
               }
               break;
            case "수락":
            case "accept":
            case "ㅇ":
               this.parties.accept(player);
               break;
            case "거절":
            case "deny":
            case "ㄴ":
               this.parties.deny(player);
               break;
            case "나가기":
            case "탈퇴":
            case "leave":
               this.parties.leave(player);
               break;
            case "추방":
            case "내보내기":
            case "kick":
               Player to = this.find(player, args);
               if (to != null) {
                  this.parties.kick(player, to);
               }
               break;
            default:
               this.info(player, label);
         }

         return true;
      } else {
         sender.sendMessage(Msg.of("&8게임 안에서 써 주십시오."));
         return true;
      }
   }

   @Nullable
   private Player find(Player from, String[] args) {
      if (args.length < 2) {
         from.sendMessage(Msg.of("&c누구인지 적어 주세요. &8예: /파티 초대 아무개"));
         return null;
      }

      Player to = from.getServer().getPlayerExact(args[1]);
      if (to == null) {
         from.sendMessage(Msg.of("&c'" + args[1] + "' 은(는) 접속 중이 아닙니다."));
      }

      return to;
   }

   private void info(Player player, String label) {
      Party party = this.parties.of(player);
      player.sendMessage(Msg.of("&6파티 &7— 같이 다니는 사람들"));
      if (party == null) {
         player.sendMessage(Msg.of("&8지금은 파티에 있지 않습니다. &7최대 &f" + this.config.partyMax() + "&7명까지 묶입니다"));
      } else {
         player.sendMessage(Msg.of("&7지금 &f" + party.size() + "&7/&f" + this.config.partyMax() + "&7명"));

         for (UUID who : party.ordered()) {
            player.sendMessage(Msg.of((party.isLeader(who) ? " &6★ " : " &8· &7") + this.parties.nameOf(who)));
         }

         player.sendMessage(Msg.of("&8파티끼리는 서로 때려도 아프지 않습니다."));
      }

      player.sendMessage(Msg.of("&e/" + label + " 초대 <사람>"));
      player.sendMessage(Msg.of("&e/" + label + " 수락 &8· &e거절 &8· &e나가기"));
      player.sendMessage(Msg.of("&e/" + label + " 추방 <사람> &8(파티장만)"));
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
      List<String> out = new ArrayList<>();
      if (args.length == 1) {
         for (String s : List.of("초대", "수락", "거절", "나가기", "추방")) {
            if (s.startsWith(args[0])) {
               out.add(s);
            }
         }

         return out;
      } else {
         if (args.length == 2 && (args[0].equals("초대") || args[0].equals("추방"))) {
            String p = args[1].toLowerCase(Locale.ROOT);

            for (Player who : sender.getServer().getOnlinePlayers()) {
               if (who.getName().toLowerCase(Locale.ROOT).startsWith(p)) {
                  out.add(who.getName());
               }
            }
         }

         return out;
      }
   }
}
