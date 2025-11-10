package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final GotCraftKitPvp plugin;

    public PlayerQuitListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save stats
        plugin.getStatsManager().savePlayerStats(event.getPlayer());

        // Remove scoreboard
        plugin.getScoreboardManager().removeScoreboard(event.getPlayer());

        // Clear active kit
        plugin.getKitManager().clearActiveKit(event.getPlayer().getUniqueId());

        // Clear zone data
        plugin.getZoneManager().clearPlayerZone(event.getPlayer().getUniqueId());

        // Close GUI
        plugin.getGuiManager().closeGUI(event.getPlayer());

        // Remove from stats cache after a delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getStatsManager().removeStats(event.getPlayer().getUniqueId());
        }, 100L);
    }
}

