package com.wolfool.prowess.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Configs {
   private Configs() {
   }

   public static boolean upgrade(JavaPlugin plugin, String name) {
      File live = new File(plugin.getDataFolder(), name);
      if (!live.isFile()) {
         plugin.saveResource(name, false);
         return true;
      }

      try (InputStream in = plugin.getResource(name)) {
         if (in == null) {
            return false;
         } else {
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            int newVer = bundled.getInt("config-version", 0);
            int oldVer = YamlConfiguration.loadConfiguration(live).getInt("config-version", 0);
            if (newVer <= oldVer) {
               return false;
            } else {
               File backupDir = new File(plugin.getDataFolder(), "_backup");
               if (!backupDir.isDirectory() && !backupDir.mkdirs()) {
                  plugin.getLogger().warning("백업 폴더를 못 만들었습니다: " + backupDir);
                  return false;
               } else {
                  String stamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
                  File backup = new File(backupDir, name.replace(".yml", "") + "_v" + oldVer + "_" + stamp + ".yml");
                  Files.copy(live.toPath(), backup.toPath());
                  if (!live.delete()) {
                     plugin.getLogger().warning("낡은 " + name + " 를 못 지웠습니다");
                     return false;
                  } else {
                     plugin.saveResource(name, false);
                     plugin.getLogger().info(name + " 를 v" + oldVer + " → v" + newVer + " 로 올렸습니다. 이전 파일은 _backup/" + backup.getName());
                     return true;
                  }
               }
            }
         }
      } catch (Exception e) {
         plugin.getLogger().log(Level.WARNING, name + " 갱신 실패", e);
         return false;
      }
   }
}
