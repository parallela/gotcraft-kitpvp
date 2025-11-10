package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Suppresses vanilla join/quit/advancement messages
 */
public class MessageSuppressionListener implements Listener {

    private final GotCraftKitPvp plugin;

    public MessageSuppressionListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Disable join message
        event.joinMessage(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Disable quit message
        event.quitMessage(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        // Disable advancement messages
        event.message(null);
    }
}

