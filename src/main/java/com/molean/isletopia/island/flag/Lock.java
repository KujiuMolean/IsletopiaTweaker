package com.molean.isletopia.island.flag;

import com.molean.isletopia.IsletopiaTweakers;
import com.molean.isletopia.event.PlayerIslandChangeEvent;
import com.molean.isletopia.island.IslandManager;
import com.molean.isletopia.island.LocalIsland;
import com.molean.isletopia.shared.model.Island;
import com.molean.isletopia.utils.IsletopiaTweakersUtils;
import com.molean.isletopia.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Lock implements IslandFlagHandler, Listener {
    public Lock() {
        Bukkit.getPluginManager().registerEvents(this, IsletopiaTweakers.getPlugin());
    }

    @EventHandler
    public void on(PlayerIslandChangeEvent event) {
        LocalIsland to = event.getTo();
        Player player = event.getPlayer();
        if (event.getPlayer().isOp()) {
            return;
        }
        if (to == null || (!to.hasPermission(player) && to.containsFlag("Lock"))) {
            if (event.getPlayer().isOp()) {
                MessageUtils.info(event.getPlayer(), "You are by passing a lock flag!");
                return;
            }
            event.setCancelled(true);
        }
    }

    @Override
    public void onFlagAdd(LocalIsland island, String... data) {
        for (Player player : island.getPlayersInIsland()) {
            if (!island.hasPermission(player)) {
                MessageUtils.strong(player, "你已被踢出此岛.");
                Island playerFirstIsland = IslandManager.INSTANCE.getPlayerFirstIsland(player.getUniqueId());
                if (playerFirstIsland == null) {
                    Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> {
                        player.kick(Component.text("#严重错误，没有岛屿。"));
                    });
                    return;
                }
                IsletopiaTweakersUtils.universalPlotVisitByMessage(player,playerFirstIsland.getIslandId());
            }
        }

    }
}
