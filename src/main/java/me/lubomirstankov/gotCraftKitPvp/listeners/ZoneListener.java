package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ZoneListener implements Listener {

    private final GotCraftKitPvp plugin;

    public ZoneListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getZoneManager().updatePlayerZone(player);
    }
}

