package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.kits.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final GotCraftKitPvp plugin;

    public GUIListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String openGUI = plugin.getGuiManager().getOpenGUI(player);

        // DEBUG: Log what's happening
        if (openGUI != null) {
            plugin.getLogger().info("GUI Click detected: " + openGUI + " at slot " + event.getSlot());
        }

        if (openGUI == null) {
            // Not our GUI, don't interfere at all
            return;
        }

        // This IS our GUI - cancel the event to prevent item movement
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        switch (openGUI) {
            case "kit-selector":
                handleKitSelector(player, clicked, event.getSlot());
                break;
            case "stats":
                handleStatsGUI(player, clicked, event.getSlot());
                break;
            case "leaderboard-main":
                handleLeaderboardMain(player, clicked, event.getSlot());
                break;
            case "leaderboard-kills":
            case "leaderboard-streaks":
            case "leaderboard-levels":
                handleLeaderboardCategory(player, clicked, event.getSlot());
                break;
        }
    }

    private void handleKitSelector(Player player, ItemStack clicked, int slot) {
        // Check for navigation buttons
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            // Previous page
            int currentPage = plugin.getGuiManager().getGUIPage(player);
            if (currentPage > 0) {
                plugin.getGuiManager().openKitSelector(player, currentPage - 1);
            }
            return;
        }

        if (slot == 53 && clicked.getType() == Material.ARROW) {
            // Next page
            int currentPage = plugin.getGuiManager().getGUIPage(player);
            plugin.getGuiManager().openKitSelector(player, currentPage + 1);
            return;
        }

        if (slot == 49 && clicked.getType() == Material.BARRIER) {
            // Close
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player);
            return;
        }

        // Check if it's a kit icon
        if (slot < 45) {
            // Find kit by icon
            for (Kit kit : plugin.getKitManager().getAllKits()) {
                if (kit.getIcon().getMaterial() == clicked.getType()) {
                    // Check if player can use this kit
                    if (!kit.getPermission().isEmpty() && !player.hasPermission(kit.getPermission())) {
                        plugin.getMessageManager().sendMessage(player, "kit-no-permission");
                        return;
                    }

                    // Check if kit needs to be purchased
                    if (!kit.isFree() && kit.getPrice() > 0) {
                        boolean purchased = plugin.getDatabaseManager().hasKitPurchased(player.getUniqueId(), kit.getId()).join();
                        if (!purchased) {
                            // Attempt to purchase
                            if (plugin.getKitManager().purchaseKit(player, kit)) {
                                // Purchased successfully, give kit
                                plugin.getKitManager().giveKit(player, kit);
                                player.closeInventory();
                                plugin.getGuiManager().closeGUI(player);
                            }
                            return;
                        }
                    }

                    // Give kit
                    plugin.getKitManager().giveKit(player, kit);
                    player.closeInventory();
                    plugin.getGuiManager().closeGUI(player);
                    return;
                }
            }
        }
    }

    private void handleStatsGUI(Player player, ItemStack clicked, int slot) {
        if (slot == 26 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player);
        }
    }

    private void handleLeaderboardMain(Player player, ItemStack clicked, int slot) {
        if (slot == 10 && clicked.getType() == Material.DIAMOND_SWORD) {
            plugin.getGuiManager().openTopKillsGUI(player);
        } else if (slot == 13 && clicked.getType() == Material.FIRE_CHARGE) {
            // Top Streaks - could add openTopStreaksGUI if implemented
            player.sendMessage(plugin.getMessageManager().parseLegacy("<yellow>Coming soon!"));
        } else if (slot == 16 && clicked.getType() == Material.EXPERIENCE_BOTTLE) {
            // Top Levels - could add openTopLevelsGUI if implemented
            player.sendMessage(plugin.getMessageManager().parseLegacy("<yellow>Coming soon!"));
        } else if (slot == 49 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player);
        }
    }

    private void handleLeaderboardCategory(Player player, ItemStack clicked, int slot) {
        if (slot == 49 && clicked.getType() == Material.ARROW) {
            plugin.getGuiManager().openLeaderboardGUI(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String openGUI = plugin.getGuiManager().getOpenGUI(player);
        if (openGUI != null) {
            // Cancel drag events in custom GUIs
            event.setCancelled(true);
        }
    }
}

