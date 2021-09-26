package com.molean.isletopia.statistics.vanilla;

import com.molean.isletopia.IsletopiaTweakers;
import com.molean.isletopia.database.VanillaStatisticsDao;
import com.molean.isletopia.event.PlayerDataSyncCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class VanillaStatistic implements Listener {
    private static final Map<String, Boolean> complete = new HashMap<>();

    public VanillaStatistic() {
        Bukkit.getPluginManager().registerEvents(this, IsletopiaTweakers.getPlugin());
        VanillaStatisticsDao.checkTable();

        Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                complete.put(player.getName(), false);
                Stats stats = VanillaStatisticsDao.getStatistics(player.getName());
                if (stats != null) {
                    stats.apply(player);
                } else {
                    VanillaStatisticsDao.setStatistics(player.getName(), Stats.fromPlayer(player));
                }
                complete.put(player.getName(), true);
            }
        });


        Runnable runnable = () -> {
            try {
                Bukkit.getLogger().info("Saving statistics for online players...");
                int cnt = 0;
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!complete.getOrDefault(onlinePlayer.getName(), false)) {
                        continue;
                    }
                    cnt++;
                    VanillaStatisticsDao.setStatistics(onlinePlayer.getName(), Stats.fromPlayer(onlinePlayer));
                }
                Bukkit.getLogger().info("Save " + cnt + " player(s) complete.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(IsletopiaTweakers.getPlugin(), runnable, 20 * 60 * 2, 20 * 60 * 2);
        IsletopiaTweakers.addDisableTask("Stop update player stats data", runnable);
        IsletopiaTweakers.addDisableTask("Stop update player stats data", bukkitTask::cancel);
    }

    @EventHandler
    public void on(PlayerDataSyncCompleteEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
            complete.put(player.getName(), false);
            Stats stats = VanillaStatisticsDao.getStatistics(player.getName());
            if (stats != null) {
                stats.apply(player);
            } else {
                VanillaStatisticsDao.setStatistics(player.getName(), Stats.fromPlayer(player));
            }
            complete.put(player.getName(), true);
        });
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        if (!complete.getOrDefault(event.getPlayer().getName(), false)) {
            return;
        }
        complete.remove(event.getPlayer().getName());
        Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
            VanillaStatisticsDao.setStatistics(event.getPlayer().getName(), Stats.fromPlayer(event.getPlayer()));
        });

    }
}
