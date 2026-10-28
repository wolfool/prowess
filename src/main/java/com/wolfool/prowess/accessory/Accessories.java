package com.wolfool.prowess.accessory;

import com.wolfool.prowess.ProwessConfig;
import com.wolfool.prowess.integration.CraftEngineHook;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;
// Base64Coder 는 snakeyaml 1.x 전용이라 Paper 1.21.4(snakeyaml 2.2) 에서는 없습니다.
// java.util.Base64 로 바꿉니다 — 옛 데이터(MIME 줄바꿈)도 MimeDecoder 가 그대로 읽습니다.
import java.util.Base64;

public final class Accessories {
   private final NamespacedKey dataKey;
   private final NamespacedKey kindKey;
   private final ProwessConfig config;

   public Accessories(Plugin plugin, ProwessConfig config) {
      this.dataKey = new NamespacedKey(plugin, "accessories");
      this.kindKey = new NamespacedKey(plugin, "accessory_kind");
      this.config = config;
   }

   @Nullable
   public String kindOf(@Nullable ItemStack item) {
      if (item != null && !item.getType().isAir()) {
         String byConfig = this.config.accessoryKind(CraftEngineHook.idOf(item));
         if (byConfig != null) {
            return byConfig;
         }

         ItemMeta meta = item.getItemMeta();
         return meta == null ? null : (String)meta.getPersistentDataContainer().get(this.kindKey, PersistentDataType.STRING);
      } else {
         return null;
      }
   }

   public ItemStack mark(ItemStack item, String kind) {
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      meta.getPersistentDataContainer().set(this.kindKey, PersistentDataType.STRING, kind);
      item.setItemMeta(meta);
      return item;
   }

   public boolean fits(Accessories.Slot slot, @Nullable ItemStack item) {
      String kind = this.kindOf(item);
      return kind != null && kind.equalsIgnoreCase(slot.kind());
   }

   public ItemStack[] all(Player player) {
      String raw = (String)player.getPersistentDataContainer().get(this.dataKey, PersistentDataType.STRING);
      ItemStack[] out = new ItemStack[Accessories.Slot.values().length];
      if (raw != null && !raw.isBlank()) {
         try {
            BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getMimeDecoder().decode(raw)));

            try {
               int n = in.readInt();

               for (int i = 0; i < n && i < out.length; i++) {
                  out[i] = (ItemStack)in.readObject();
               }
            } catch (Throwable var8) {
               try {
                  in.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            in.close();
            return out;
         } catch (Exception e) {
            return new ItemStack[Accessories.Slot.values().length];
         }
      } else {
         return out;
      }
   }

   @Nullable
   public ItemStack get(Player player, Accessories.Slot slot) {
      return this.all(player)[slot.ordinal()];
   }

   public void set(Player player, Accessories.Slot slot, @Nullable ItemStack item) {
      ItemStack[] worn = this.all(player);
      worn[slot.ordinal()] = item != null && !item.getType().isAir() ? item.clone() : null;
      this.save(player, worn);
   }

   public void save(Player player, ItemStack[] worn) {
      try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
         BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes);

         try {
            out.writeInt(worn.length);

            for (ItemStack it : worn) {
               out.writeObject(it);
            }

            out.flush();
            player.getPersistentDataContainer().set(this.dataKey, PersistentDataType.STRING, Base64.getEncoder().encodeToString(bytes.toByteArray()));
         } catch (Throwable var11) {
            try {
               out.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }

            throw var11;
         }

         out.close();
      } catch (Exception e) {
         player.sendMessage("§c[장신구] 저장하지 못했습니다. 관리자에게 알려 주세요.");
      }
   }

   public List<ItemStack> worn(Player player) {
      List<ItemStack> out = new ArrayList<>();

      for (ItemStack it : this.all(player)) {
         if (it != null && !it.getType().isAir()) {
            out.add(it);
         }
      }

      return out;
   }

   public enum Slot {
      RING1("반지 I", "ring"),
      RING2("반지 II", "ring"),
      NECKLACE("목걸이", "necklace"),
      EARRING("귀걸이", "earring");

      private final String korean;
      private final String kind;

      Slot(String korean, String kind) {
         this.korean = korean;
         this.kind = kind;
      }

      public String korean() {
         return this.korean;
      }

      public String kind() {
         return this.kind;
      }
   }
}
