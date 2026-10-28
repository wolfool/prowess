package com.wolfool.prowess.party;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.util.Msg;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class PartyService {
   private static final long INVITE_MS = 60000L;
   private final Plugin plugin;
   private final ProwessConfig config;
   private final Map<UUID, Party> byMember = new HashMap<>();
   private final Map<UUID, PartyService.Invite> invites = new HashMap<>();

   public PartyService(Plugin plugin, ProwessConfig config) {
      this.plugin = plugin;
      this.config = config;
   }

   @Nullable
   public Party of(Player player) {
      return player == null ? null : this.byMember.get(player.getUniqueId());
   }

   @Nullable
   public Party of(UUID who) {
      return who == null ? null : this.byMember.get(who);
   }

   public boolean together(UUID a, UUID b) {
      if (a != null && b != null && !a.equals(b)) {
         Party p = this.byMember.get(a);
         return p != null && p.has(b);
      } else {
         return false;
      }
   }

   public void invite(Player from, Player to) {
      if (from.equals(to)) {
         from.sendMessage(Msg.of("&c자기 자신은 부를 수 없습니다."));
      } else {
         Party mine = this.of(from);
         if (mine == null) {
            mine = this.create(from);
         } else if (!mine.isLeader(from.getUniqueId())) {
            from.sendMessage(Msg.of("&c파티장만 부를 수 있습니다."));
            return;
         }

         int max = this.config.partyMax();
         if (mine.size() >= max) {
            from.sendMessage(Msg.of("&c파티가 꽉 찼습니다. &7(최대 " + max + "명)"));
         } else if (this.of(to) != null) {
            from.sendMessage(Msg.of("&c" + to.getName() + " 님은 이미 파티에 있습니다."));
         } else {
            this.invites.put(to.getUniqueId(), new PartyService.Invite(from.getUniqueId(), System.currentTimeMillis()));
            from.sendMessage(Msg.of("&a[파티] &f" + to.getName() + " &7님을 불렀습니다. &81분 안에 받아야 합니다"));
            to.sendMessage(Msg.of("&a[파티] &f" + from.getName() + " &7님이 파티에 불렀습니다."));
            to.sendMessage(Msg.of("&7  받으려면 &f/파티 수락&7, 안 받으려면 &f/파티 거절"));
         }
      }
   }

   public void accept(Player who) {
      PartyService.Invite invite = this.invites.remove(who.getUniqueId());
      if (invite != null && !invite.expired()) {
         Player from = this.plugin.getServer().getPlayer(invite.from());
         if (from == null) {
            who.sendMessage(Msg.of("&c부른 사람이 접속 중이 아닙니다."));
         } else {
            Party party = this.of(from);
            if (party == null) {
               party = this.create(from);
            }

            int max = this.config.partyMax();
            if (party.size() >= max) {
               who.sendMessage(Msg.of("&c그 사이에 파티가 꽉 찼습니다. &7(최대 " + max + "명)"));
            } else if (this.of(who) != null) {
               who.sendMessage(Msg.of("&c이미 파티에 있습니다. &7/파티 나가기"));
            } else {
               party.add(who.getUniqueId());
               this.byMember.put(who.getUniqueId(), party);
               this.announce(party, "&a[파티] &f" + who.getName() + " &7님이 들어왔습니다. &8(" + party.size() + "/" + max + ")");
            }
         }
      } else {
         who.sendMessage(Msg.of("&c받을 초대가 없습니다."));
      }
   }

   public void deny(Player who) {
      PartyService.Invite invite = this.invites.remove(who.getUniqueId());
      if (invite == null) {
         who.sendMessage(Msg.of("&c받을 초대가 없습니다."));
      } else {
         who.sendMessage(Msg.of("&7[파티] 거절했습니다."));
         Player from = this.plugin.getServer().getPlayer(invite.from());
         if (from != null) {
            from.sendMessage(Msg.of("&7[파티] &f" + who.getName() + " &7님이 거절했습니다."));
         }
      }
   }

   public void leave(Player who) {
      Party party = this.of(who);
      if (party == null) {
         who.sendMessage(Msg.of("&c파티에 있지 않습니다."));
      } else {
         this.remove(party, who.getUniqueId());
         who.sendMessage(Msg.of("&7[파티] 나왔습니다."));
         this.announce(party, "&7[파티] &f" + who.getName() + " &7님이 나갔습니다.");
      }
   }

   public void kick(Player leader, Player target) {
      Party party = this.of(leader);
      if (party == null || !party.isLeader(leader.getUniqueId())) {
         leader.sendMessage(Msg.of("&c파티장만 내보낼 수 있습니다."));
      } else if (!party.has(target.getUniqueId())) {
         leader.sendMessage(Msg.of("&c같은 파티가 아닙니다."));
      } else if (target.equals(leader)) {
         leader.sendMessage(Msg.of("&c자기 자신은 못 내보냅니다. &7/파티 나가기"));
      } else {
         this.remove(party, target.getUniqueId());
         target.sendMessage(Msg.of("&c[파티] 내보내졌습니다."));
         this.announce(party, "&7[파티] &f" + target.getName() + " &7님을 내보냈습니다.");
      }
   }

   private void remove(Party party, UUID who) {
      party.remove(who);
      this.byMember.remove(who);
      if (party.size() > 1) {
         if (party.isLeader(who)) {
            UUID next = party.ordered().get(0);
            party.leader(next);
            this.announce(party, "&e[파티] &f" + this.nameOf(next) + " &7님이 파티장이 되었습니다.");
         }
      } else {
         for (UUID left : party.members()) {
            this.byMember.remove(left);
         }
      }
   }

   private Party create(Player leader) {
      Party party = new Party(leader.getUniqueId());
      this.byMember.put(leader.getUniqueId(), party);
      return party;
   }

   public void announce(Party party, String message) {
      if (party != null) {
         for (Player p : party.online()) {
            p.sendMessage(Msg.of(message));
         }
      }
   }

   public String nameOf(UUID who) {
      Player p = this.plugin.getServer().getPlayer(who);
      if (p != null) {
         return p.getName();
      }

      OfflinePlayer off = this.plugin.getServer().getOfflinePlayer(who);
      return off.getName() == null ? "?" : off.getName();
   }

   public void clear() {
      this.byMember.clear();
      this.invites.clear();
   }

   private record Invite(UUID from, long at) {
      boolean expired() {
         return System.currentTimeMillis() - this.at > 60000L;
      }
   }
}
