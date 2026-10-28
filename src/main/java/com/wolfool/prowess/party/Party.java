package com.wolfool.prowess.party;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Party {
   private final UUID id = UUID.randomUUID();
   private UUID leader;
   private final Set<UUID> members = new LinkedHashSet<>();

   Party(UUID leader) {
      this.leader = leader;
      this.members.add(leader);
   }

   public UUID id() {
      return this.id;
   }

   public UUID leader() {
      return this.leader;
   }

   void leader(UUID value) {
      this.leader = value;
   }

   public boolean isLeader(UUID who) {
      return this.leader.equals(who);
   }

   public Set<UUID> members() {
      return Set.copyOf(this.members);
   }

   public List<UUID> ordered() {
      return new ArrayList<>(this.members);
   }

   public int size() {
      return this.members.size();
   }

   public boolean has(UUID who) {
      return this.members.contains(who);
   }

   boolean add(UUID who) {
      return this.members.add(who);
   }

   boolean remove(UUID who) {
      return this.members.remove(who);
   }

   public List<Player> online() {
      List<Player> out = new ArrayList<>(this.members.size());

      for (UUID who : this.members) {
         Player p = Bukkit.getPlayer(who);
         if (p != null && p.isOnline()) {
            out.add(p);
         }
      }

      return out;
   }
}
