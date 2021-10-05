package com.molean.isletopia.menu.recipe;

import com.molean.isletopia.IsletopiaTweakers;
import com.molean.isletopia.menu.ItemStackSheet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.function.Consumer;

public class RecipeListMenu implements Listener {
    private final Player player;
    private final Inventory inventory;
    private boolean stop = false;
    private final String fatherCommand;

    public RecipeListMenu(Player player, String fatherCommand) {
        this.player = player;
        this.fatherCommand = fatherCommand;
        inventory = Bukkit.createInventory(player, 54, Component.text("扩展合成表"));
        Bukkit.getPluginManager().registerEvents(this, IsletopiaTweakers.getPlugin());
    }

    @SuppressWarnings("all")
    public void open() {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStackSheet itemStackSheet = new ItemStackSheet(Material.GRAY_STAINED_GLASS_PANE, " ");
            inventory.setItem(i, itemStackSheet.build());
        }
        ItemStackSheet father = new ItemStackSheet(Material.BARRIER, "§f返回");
        inventory.setItem(inventory.getSize() - 1, father.build());
        Bukkit.getScheduler().runTaskTimerAsynchronously(IsletopiaTweakers.getPlugin(), new Consumer<BukkitTask>() {
            int cnt = 0;
            @Override
            public void accept(BukkitTask task) {
                if (stop) {
                    task.cancel();
                }
                for (int i = 0; i < LocalRecipe.localRecipeList.size(); i++) {
                    List<ItemStack> icons = LocalRecipe.localRecipeList.get(i).icons;
                    inventory.setItem(i, icons.get(cnt % icons.size()));
                }
                cnt++;

            }
        }, 0, 20);

        Bukkit.getScheduler().runTask(IsletopiaTweakers.getPlugin(), () -> player.openInventory(inventory));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }
        event.setCancelled(true);
        if (!event.getClick().equals(ClickType.LEFT)) {
            return;
        }
        int slot = event.getSlot();
        if (slot == inventory.getSize() - 1) {
            player.performCommand(fatherCommand);
            return;
        }
        if (slot >= LocalRecipe.localRecipeList.size() || slot < 0) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(IsletopiaTweakers.getPlugin(), () -> new CraftRecipeMenu(player, LocalRecipe.localRecipeList.get(slot), fatherCommand).open());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }
        stop = true;
        event.getHandlers().unregister(this);
    }
}
