package com.wolfool.prowess.util;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class Economy {
   private final Plugin plugin;
   private net.milkbowl.vault.economy.Economy vault;

   public Economy(Plugin plugin) {
      this.plugin = plugin;
   }

   public void hookLater() {
      this.plugin.getServer().getScheduler().runTask(this.plugin, this::hook);
   }

   public void hook() {
      if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
         this.plugin.getLogger().info("Vault 가 없습니다. 강화에 돈을 안 받습니다.");
      } else {
         RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServicesManager()
            .getRegistration(net.milkbowl.vault.economy.Economy.class);
         if (rsp == null) {
            this.plugin.getLogger().warning("Vault 는 있는데 경제 공급자가 없습니다. 강화에 돈을 안 받습니다.");
         } else {
            this.vault = (net.milkbowl.vault.economy.Economy)rsp.getProvider();
            this.plugin.getLogger().info("경제 연결 완료 — " + this.vault.getName());
         }
      }
   }

   public boolean connected() {
      return this.vault != null;
   }

   public boolean has(Player player, long amount) {
      return this.vault != null && amount > 0L ? this.vault.has(player, amount) : true;
   }

   public long balance(Player player) {
      return this.vault == null ? 0L : (long)this.vault.getBalance(player);
   }

   public boolean take(Player player, long amount) {
      if (this.vault != null && amount > 0L) {
         EconomyResponse r = this.vault.withdrawPlayer(player, amount);
         return r.transactionSuccess();
      } else {
         return true;
      }
   }

   public String unit() {
      return "원";
   }
}
