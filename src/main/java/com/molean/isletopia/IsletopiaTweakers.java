package com.molean.isletopia;

import com.molean.isletopia.shared.ClassResolver;
import com.molean.isletopia.shared.message.RedisMessageListener;
import com.molean.isletopia.shared.platform.BukkitRelatedUtils;
import com.molean.isletopia.shared.utils.UUIDManager;
import com.molean.isletopia.task.Tasks;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class IsletopiaTweakers extends JavaPlugin {

    private static IsletopiaTweakers isletopiaTweakers;

    public static IsletopiaTweakers getPlugin() {
        return isletopiaTweakers;
    }

    private void setupWorldGameRule(World world) {
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, true);
        world.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.DO_LIMITED_CRAFTING, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, true);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.DO_TILE_DROPS, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, true);
        world.setGameRule(GameRule.MOB_GRIEFING, true);
        world.setGameRule(GameRule.NATURAL_REGENERATION, true);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, false);
        world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.DISABLE_RAIDS, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.DROWNING_DAMAGE, true);
        world.setGameRule(GameRule.FALL_DAMAGE, true);
        world.setGameRule(GameRule.FIRE_DAMAGE, true);
        world.setGameRule(GameRule.FREEZE_DAMAGE, true);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, true);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, true);
        world.setGameRule(GameRule.FORGIVE_DEAD_PLAYERS, true);
        world.setGameRule(GameRule.UNIVERSAL_ANGER, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24);
        world.setGameRule(GameRule.MAX_COMMAND_CHAIN_LENGTH, 65536);
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 0);
        world.setDifficulty(Difficulty.HARD);
    }

    @Override
    public void onEnable() {
        isletopiaTweakers = this;

        Tasks.INSTANCE.async(() -> {
            UUIDManager.INSTANCE.getSnapshot();
            Tasks.INSTANCE.sync(() -> {
                try {
                    ClassResolver.INSTANCE.loadClass();
                    ClassResolver.INSTANCE.resolveBean();
                    ClassResolver.INSTANCE.resolveFieldInject();
                   BukkitRelatedUtils.getInstance().setShutdown(false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            });
        });

        Tasks.INSTANCE.sync(() -> {
            for (World world : Bukkit.getWorlds()) {
                setupWorldGameRule(world);
            }
        });
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        return new EmptyChunkGenerator();
    }

    @Override
    public void onDisable() {
        BukkitRelatedUtils.getInstance().setShutdown(true);
        getLogger().info("Destroy redis listener..");
        getLogger().info("Close online player inventory..");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.closeInventory();
        }
        Tasks.INSTANCE.getShutdownMap().forEach((s, runnable) -> {
            getLogger().info("Running shutdown task: " + s);
            long l = System.currentTimeMillis();
            runnable.run();
            getLogger().info(s + " complete in " + (System.currentTimeMillis() - l) + "ms");
        });
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.setGameMode(GameMode.SPECTATOR);
        }
        HandlerList.unregisterAll(this);
    }
}
