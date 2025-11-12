package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Prevents players without permission from breaking or placing blocks
 */
public class BlockProtectionListener implements Listener {

    private final GotCraftKitPvp plugin;

    public BlockProtectionListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Allow if player is OP
        if (player.isOp()) {
            return;
        }

        // Allow if player has permission
        if (player.hasPermission("kitpvp.build")) {
            return;
        }

        // Cancel the event - player cannot break blocks
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Allow if player is OP
        if (player.isOp()) {
            return;
        }

        // Allow if player has permission
        if (player.hasPermission("kitpvp.build")) {
            return;
        }

        // Cancel the event - player cannot place blocks
        event.setCancelled(true);
    }
}

