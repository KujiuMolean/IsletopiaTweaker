package com.molean.isletopia.distribute.individual;

import com.molean.isletopia.IsletopiaTweakers;
import com.molean.isletopia.event.PlayerDataSyncCompleteEvent;
import com.molean.isletopia.shared.database.PlayerDataDao;
import com.molean.isletopia.shared.utils.RedisUtils;
import com.molean.isletopia.utils.MessageUtils;
import com.molean.isletopia.utils.PlayerSerializeUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class PlayerDataSync implements Listener {

    private final Map<UUID, String> passwdMap = new HashMap<>();

    public PlayerDataSync() {
        Bukkit.getPluginManager().registerEvents(this, IsletopiaTweakers.getPlugin());

        // check table
        try {
            PlayerDataDao.checkTable();
        } catch (SQLException e) {
            e.printStackTrace();
            //stop server if check has error
            Logger.getAnonymousLogger().severe("Database check error!");
            Bukkit.shutdown();
        }

        // add shutdown task

        IsletopiaTweakers.addDisableTask("Save player data to database", () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (passwdMap.containsKey(onlinePlayer.getUniqueId())) {
                    try {
                        byte[] serialize = PlayerSerializeUtils.serialize(onlinePlayer);
                        onLeft(onlinePlayer, serialize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // load data to current player
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onJoin(onlinePlayer);
        }


        //update one player data per second
        Queue<Player> queue = new ArrayDeque<>();
        Bukkit.getScheduler().runTaskTimerAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
            if (queue.isEmpty()) {
                queue.addAll(Bukkit.getOnlinePlayers());
                return;
            }

            Player player = queue.poll();

            if (player.isOnline() && passwdMap.containsKey(player.getUniqueId())) {

                Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> update(player));

            }
        }, 20, 20);

    }

    public void update(Player player) {
        try {
            byte[] serialize = PlayerSerializeUtils.serialize(player);
            Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
                try {
                    if (!PlayerDataDao.update(player.getUniqueId(), serialize, passwdMap.get(player.getUniqueId()))) {
                        throw new RuntimeException("Unexpected complete player data error!");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    MessageUtils.warn(player, "你的背包数据保存失败，请尽快联系管理员处理！");
                }
                RedisUtils.getCommand().set("GameMode:" + player.getName(), player.getGameMode().name());
            });
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtils.warn(player, "你的背包数据保存失败，请尽快联系管理员处理！");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void on(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        RedisUtils.getCommand().set(player.getName() + ":GameMode", event.getNewGameMode().getValue() + "");
    }

    public void onLeft(Player player, byte[] data) {
        try {
            if (!PlayerDataDao.complete(player.getUniqueId(), data, passwdMap.get(player.getUniqueId()))) {
                throw new RuntimeException("Unexpected complete player data error!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        passwdMap.remove(player.getUniqueId());
        //store game mode
        RedisUtils.getCommand().set("GameMode:" + player.getName(), player.getGameMode().name());
    }

    public void onJoin(Player player) {
        Location location = player.getLocation().clone();
        try {
            if (!PlayerDataDao.exist(player.getUniqueId())) {
                //插入数据
                byte[] serialize = PlayerSerializeUtils.serialize(player);
                PlayerDataDao.insert(player.getUniqueId(), serialize);
            }

            player.setGameMode(GameMode.SPECTATOR);
            //拿锁
            String passwd = PlayerDataDao.getLock(player.getUniqueId());
            if (passwd != null) {
                loadData(player, passwd, location);
                // end
            } else {
                //没拿到, 开始等锁
                Bukkit.getScheduler().runTaskTimer(IsletopiaTweakers.getPlugin(), new Consumer<>() {
                    private int times = 0;

                    @Override
                    public void accept(BukkitTask task) {
                        try {
                            //尝试拿锁
                            String lock = PlayerDataDao.getLock(player.getUniqueId());

                            if (lock != null) {
                                loadData(player, lock, location);
                                task.cancel();

                                //end
                                return;
                            }

                            times++;
                            if (times > 15) {
                                task.cancel();
                                //等待超时, 可能是上个服务器崩了, 强制拿锁
                                String lockForce = PlayerDataDao.getLockForce(player.getUniqueId());

                                if (lockForce == null) {
                                    //强制拿锁失败, 出大问题
                                    throw new RuntimeException("Unexpected error! Force get lock failed.");
                                    //end (failed)
                                }

                                loadData(player, lockForce, location);


                                //end (success)
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                task.cancel();
                                player.setGameMode(GameMode.SURVIVAL);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                player.kick(Component.text("#读取玩家数据出错，请联系管理员！"));
                            }
                        }
                    }
                }, 20, 20);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                player.setGameMode(GameMode.SURVIVAL);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                player.kick(Component.text("#读取玩家数据出错，请联系管理员！"));
            }
        }
    }

    private void loadData(Player player, String passwd, Location location) throws SQLException, IOException {
        passwdMap.put(player.getUniqueId(), passwd);
        //强制拿到锁了, 加载数据

        byte[] query = PlayerDataDao.query(player.getUniqueId(), passwd);


        if (query == null) {
            throw new RuntimeException("Unexpected get player data failed!");
            //end (failed)
        }

        PlayerSerializeUtils.deserialize(player, query);
        //deserialize player from db


        player.teleport(location);
        if (RedisUtils.getCommand().exists("GameMode:" + player.getName()) > 0) {

            String s = RedisUtils.getCommand().get("GameMode:" + player.getName());
            try {
                GameMode realGameMode = GameMode.valueOf(s);
                player.setGameMode(realGameMode);
            } catch (IllegalArgumentException ignored) {
            }
        }
        PlayerDataSyncCompleteEvent playerDataSyncCompleteEvent = new PlayerDataSyncCompleteEvent(player);
        Bukkit.getPluginManager().callEvent(playerDataSyncCompleteEvent);


    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerJoinEvent event) {
        onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerQuitEvent event) {
        if (passwdMap.containsKey(event.getPlayer().getUniqueId())) {
            try {
                byte[] serialize = PlayerSerializeUtils.serialize(event.getPlayer());
                Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> {
                    onLeft(event.getPlayer(), serialize);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
