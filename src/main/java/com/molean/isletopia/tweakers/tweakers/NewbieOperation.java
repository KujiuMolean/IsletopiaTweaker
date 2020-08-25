package com.molean.isletopia.tweakers.tweakers;

import com.molean.isletopia.database.PlotDao;
import com.molean.isletopia.parameter.UniversalParameter;
import com.molean.isletopia.tweakers.IsletopiaTweakers;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import net.craftersland.data.bridge.api.events.SyncCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

public class NewbieOperation implements Listener {

    public NewbieOperation() {
        Bukkit.getPluginManager().registerEvents(this, IsletopiaTweakers.getPlugin());
    }

    public void checkNewbie(Player player) {
        Bukkit.getLogger().info("Check newbie operation for " + player.getName());

        if (!player.isOnline()) {
            Bukkit.getLogger().info("Player is not online, stop check newbie.");
            return;
        }

        Set<Plot> plots = PlotSquared.get().getPlots(PlotPlayer.wrap(player));
        if (plots.size() != 0) {
            return;
        }

        List<String> servers = IsletopiaTweakers.getServers();
        for (String server : servers) {
            if (server.equalsIgnoreCase("dispatcher"))
                continue;
            Integer plotID = PlotDao.getPlotID(server, player.getName());
            if (plotID != null) {
                Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> {
                    player.kickPlayer("严重错误, 在其他服务器拥有岛屿, 但位置与数据库不匹配.");
                });
                return;
            }
        }

        Bukkit.getLogger().info("Plot size is 0, try to operate...");
        Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 4));
            player.performCommand("plot auto");
            placeItem(player.getInventory());
        });


    }

    @EventHandler
    public void onSync(SyncCompleteEvent event) {


        Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {

            if (!event.getPlayer().getInventory().contains(Material.CLOCK)) {
                Bukkit.getLogger().info("No clock in inventory, try to give...");
                event.getPlayer().getInventory().addItem(newUnbreakableItem(Material.CLOCK, "§f[§d主菜单§f]§r",
                        List.of("§f[§f西弗特左键单击§f]§r §f回到§r §f主岛屿§r", "§f[§7右键单击§f]§r §f打开§r §f主菜单§r")));
            }

            String server = UniversalParameter.getParameter(event.getPlayer().getName(), "server");

            if (server == null) {
                Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> {
                    event.getPlayer().kickPlayer("严重错误, 已加入服务器, 但未被分配岛屿.");
                });
                return;
            }

            if (server.equalsIgnoreCase(IsletopiaTweakers.getServerName())) {
                Bukkit.getLogger().info("Server matched, then start newbiew check.");
                checkNewbie(event.getPlayer());
            } else {
                Bukkit.getLogger().info("Server not match, skip newbie check.");
            }
        });

        //check plot number
        Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
            if (event.getPlayer().isOp()) {
                return;
            }
            int cnt = 0;
            for (String server : IsletopiaTweakers.getServers()) {
                if (server.equalsIgnoreCase("dispatcher"))
                    continue;
                Integer plotID = PlotDao.getPlotID(server, event.getPlayer().getName());
                if (plotID != null) {
                    cnt++;
                }
            }
            if (cnt > 1) {
                Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> {
                    event.getPlayer().kickPlayer("发生错误, 非管理员但拥有多个岛屿.");
                });

            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
    }

    @EventHandler
    public void onLeft(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        Bukkit.getLogger().info("Player quit, update lastServer.");
        UniversalParameter.setParameter(player.getName(), "lastServer", IsletopiaTweakers.getServerName());
    }

    public void placeItem(PlayerInventory inventory) {
        ItemStack helmet = newUnbreakableItem(Material.LEATHER_HELMET, "§f[§d新手帽子§f]§r", List.of());
        ItemStack chestPlate = newUnbreakableItem(Material.LEATHER_CHESTPLATE, "§f[§d新手上衣§f]§r", List.of());
        ItemStack leggings = newUnbreakableItem(Material.LEATHER_LEGGINGS, "§f[§d新手裤子§f]§r", List.of());
        ItemStack boots = newUnbreakableItem(Material.LEATHER_BOOTS, "§f[§d新手靴子§f]§r", List.of());
        ItemStack sword = newUnbreakableItem(Material.WOODEN_SWORD, "§f[§d新手木剑§f]§r", List.of());
        ItemStack shovel = newUnbreakableItem(Material.WOODEN_SHOVEL, "§f[§d新手木锹§f]§r", List.of());
        ItemStack pickAxe = newUnbreakableItem(Material.WOODEN_PICKAXE, "§f[§d新手木镐§f]§r", List.of());
        ItemStack axe = newUnbreakableItem(Material.WOODEN_AXE, "§f[§d新手木斧§f]§r", List.of());
        ItemStack hoe = newUnbreakableItem(Material.WOODEN_HOE, "§f[§d新手木锄§f]§r", List.of());

        ItemStack food = new ItemStack(Material.APPLE, 32);
        ItemStack lavaBucket = new ItemStack(Material.LAVA_BUCKET);
        ItemStack ice = new ItemStack(Material.ICE, 2);

        if (inventory.getHelmet() == null) {
            inventory.setHelmet(helmet);
        }
        if (inventory.getChestplate() == null) {
            inventory.setChestplate(chestPlate);
        }
        if (inventory.getLeggings() == null) {
            inventory.setLeggings(leggings);
        }
        if (inventory.getBoots() == null) {
            inventory.setBoots(boots);
        }

        inventory.addItem(food, sword, axe, pickAxe, hoe, shovel, lavaBucket, ice);
    }

    public ItemStack newUnbreakableItem(Material material, String name, List<String> lores) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setUnbreakable(true);
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lores);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
