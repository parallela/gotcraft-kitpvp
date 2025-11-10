package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ZoneSelectionListener implements Listener {

    private final GotCraftKitPvp plugin;

    public ZoneSelectionListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if holding zone selection wand
        if (item == null || item.getType() != Material.STICK) {
            return;
        }

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        // Check for "Zone" and "Selection" or "Wand" in the name
        if (!displayName.toLowerCase().contains("zone") ||
            !(displayName.toLowerCase().contains("selection") || displayName.toLowerCase().contains("wand"))) {
            return;
        }

        // This is the zone wand - cancel the event
        event.setCancelled(true);

        if (!player.hasPermission("kitpvp.admin.setzone")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() == null) return;

            // Position 1
            Location clickedBlock = event.getClickedBlock().getLocation();
            plugin.getZoneManager().getZoneSelection().setPos1(player.getUniqueId(), clickedBlock);

            Map<String, String> placeholders = Map.of(
                    "%x%", String.valueOf(clickedBlock.getBlockX()),
                    "%y%", String.valueOf(clickedBlock.getBlockY()),
                    "%z%", String.valueOf(clickedBlock.getBlockZ())
            );
            plugin.getMessageManager().sendMessage(player, "zone-pos1-set", placeholders);

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() == null) return;

            // Position 2
            Location clickedBlock = event.getClickedBlock().getLocation();
            plugin.getZoneManager().getZoneSelection().setPos2(player.getUniqueId(), clickedBlock);

            Map<String, String> placeholders = Map.of(
                    "%x%", String.valueOf(clickedBlock.getBlockX()),
                    "%y%", String.valueOf(clickedBlock.getBlockY()),
                    "%z%", String.valueOf(clickedBlock.getBlockZ())
            );
            plugin.getMessageManager().sendMessage(player, "zone-pos2-set", placeholders);

            // Check if both positions are now set and show confirmation
            if (plugin.getZoneManager().getZoneSelection().hasSelection(player.getUniqueId())) {
                // Extract zone type from wand lore
                String zoneType = "safe"; // default
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    var lore = item.getItemMeta().getLore();
                    if (lore != null) {
                        for (String line : lore) {
                            String stripped = ChatColor.stripColor(line).toLowerCase();
                            if (stripped.contains("creating:")) {
                                if (stripped.contains("pvp")) {
                                    zoneType = "pvp";
                                } else if (stripped.contains("safe")) {
                                    zoneType = "safe";
                                }
                                break;
                            }
                        }
                    }
                }

                Map<String, String> confirmPlaceholders = Map.of("%type%", zoneType);
                plugin.getMessageManager().sendMessage(player, "zone-selection-complete", confirmPlaceholders);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Prevent breaking blocks with zone wand
        if (item != null && item.getType() == Material.STICK && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
            if (displayName.contains("zone") && (displayName.contains("selection") || displayName.contains("wand"))) {
                event.setCancelled(true);
            }
        }
    }
}

