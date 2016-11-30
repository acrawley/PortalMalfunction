package net.andrewcr.minecraft.plugin.PortalMalfunction.internal.listeners;

import net.andrewcr.minecraft.plugin.BasePluginLib.util.ChestUtil;
import net.andrewcr.minecraft.plugin.BasePluginLib.util.StringUtil;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.ConfigStore;
import net.andrewcr.minecraft.plugin.PortalMalfunction.internal.model.PlayerConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestListener implements Listener {
    private final static String CUSTOM_INVENTORY_TITLE = "Lost and Found";

    //region Event Handlers

    @EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        Chest chest = this.getChestForEvent(event);
        if (chest == null) {
            return;
        }

        // Cancel the chest open - we're going to use a custom inventory instead
        event.setCancelled(true);

        // Create a new inventory to hold the player's stored items
        Inventory inventory = Bukkit.createInventory(player, 54, CUSTOM_INVENTORY_TITLE);

        PlayerConfig config = ConfigStore.getInstance().getPlayerConfig(player);
        if (config == null) {
            // Chest appears empty to players without stored items
            return;
        }

        // Show as many of the player's stored items as will fit in the chest
        int itemCount = Math.min(inventory.getSize(), config.getInventory().size());
        for (int i = 0; i < itemCount; i++) {
            // Remove items from stored inventory and place in chest
            inventory.addItem(config.getInventory().remove(0));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        PlayerConfig config = ConfigStore.getInstance().getPlayerConfig(player);
        if (config == null) {
            return;
        }

        if (!isCustomInventory(event.getInventory())) {
            return;
        }

        // Place any items remaining in the chest back into the stored inventory
        for (int i = 0; i < event.getInventory().getSize(); i++) {
            ItemStack stack = event.getInventory().getItem(i);
            if (stack != null) {
                config.getInventory().add(event.getInventory().getItem(i));
            }
        }

        config.notifyInventoryChanged();
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !isCustomInventory(clickedInventory)) {
            return;
        }

        boolean isMoveToChest = false;
        switch (event.getAction()) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
                isMoveToChest = isCustomInventory(clickedInventory);
                break;

            case SWAP_WITH_CURSOR:
            case MOVE_TO_OTHER_INVENTORY:
                isMoveToChest = !isCustomInventory(clickedInventory);
        }

        if (isMoveToChest) {
            this.blockChestAdd(event, player);
        }
    }

    @EventHandler
    private void onInventoryDragEvent(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (!isCustomInventory(event.getInventory())) {
            return;
        }

        InventoryView view = event.getView();
        if (view.getTopInventory() == null) {
            return;
        }

        // Cancel the item drag if any of the new items are in the portal chest
        int topInventorySize = view.getTopInventory().getSize();
        for (int slot : event.getNewItems().keySet()) {
            if (slot < topInventorySize) {
                this.blockChestAdd(event, player);
                break;
            }
        }
    }

    @EventHandler
    private void onBlockBreakEvent(BlockBreakEvent event) {
        Chest chest = this.getChestFromLocation(event.getBlock().getLocation());
        if (chest == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getPlayer() != null) {
            event.getPlayer().sendMessage(ChatColor.RED + "This chest cannot be removed.");
        }
    }

    @EventHandler
    private void onBlockExplodeEvent(BlockExplodeEvent event) {
        this.unexplodeChests(event.blockList());
    }

    @EventHandler
    private void onEntityExplodeEvent(EntityExplodeEvent event) {
        this.unexplodeChests(event.blockList());
    }

    private void blockChestAdd(InventoryInteractEvent event, Player player) {
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Items cannot be placed in this chest!");
    }

    //endregion

    private static boolean isCustomInventory(Inventory inv) {
        return StringUtil.equals(inv.getTitle(), "Lost and Found");
    }

    private Chest getChestForEvent(InventoryEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        Chest targetChest = null;
        if (holder instanceof DoubleChest) {
            targetChest = ChestUtil.getChest(((Chest) ((DoubleChest) holder).getLeftSide()).getLocation());
        } else if (holder instanceof Chest) {
            targetChest = (Chest) holder;
        }

        if (targetChest == null) {
            // Inventory isn't a chest
            return null;
        }

        if (!targetChest.getLocation().equals(ConfigStore.getInstance().getChestLocation())) {
            // Inventory was a chest, but not our magic chest
            return null;
        }

        return targetChest;
    }

    private void unexplodeChests(List<Block> explodedBlocks) {
        // Exploded block was a portal chest - remove it from the list
        new ArrayList<>(explodedBlocks).stream()
            .filter(block -> this.getChestFromLocation(block.getLocation()) != null)
            .forEach(explodedBlocks::remove);
    }

    private Chest getChestFromLocation(Location location) {
        Chest targetChest = ChestUtil.getChest(location);
        if (targetChest == null) {
            return null;
        }

        if (!targetChest.getLocation().equals(ConfigStore.getInstance().getChestLocation())) {
            // Inventory was a chest, but not our magic chest
            return null;
        }

        return targetChest;
    }

    static void addPlayerInventory(Player player) {
        PlayerConfig config = ConfigStore.getInstance().getPlayerConfig(player);

        // Put player items into chest
        for (ItemStack stack : player.getInventory().getContents()) {
            ChestListener.addItemStack(config, stack);
        }
        player.getInventory().clear();

        // Put player armor into chest
        for (ItemStack armorStack : player.getInventory().getArmorContents()) {
            ChestListener.addItemStack(config, armorStack);
        }
        player.getInventory().setArmorContents(null);

        if (player.getInventory().getItemInOffHand() != null) {
            ChestListener.addItemStack(config, player.getInventory().getItemInOffHand());
        }

        config.notifyInventoryChanged();
    }

    private static void addItemStack(PlayerConfig config, ItemStack stack) {
        if (stack != null && stack.getAmount() > 0) {
            int itemsLeft = stack.getAmount();
            for (ItemStack storedStack : config.getInventory()) {
                if (stack.isSimilar(storedStack)) {
                    // Add stack to existing stack of the same item
                    int storeAmount = Math.min(itemsLeft, storedStack.getMaxStackSize() - storedStack.getAmount());
                    if (storeAmount > 0) {
                        storedStack.setAmount(storedStack.getAmount() + storeAmount);
                        itemsLeft -= storeAmount;
                    }
                }

                if (itemsLeft == 0) {
                    // Stored everything in existing stacks, we're done
                    break;
                }
            }

            if (itemsLeft != 0) {
                // Add any leftover items as a new stack
                stack.setAmount(itemsLeft);
                config.getInventory().add(stack);
            }
        }
    }
}
