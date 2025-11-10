package me.lubomirstankov.gotCraftKitPvp.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Custom chat formatting with level display and configurable styling
 */
public class ChatListener implements Listener {

    private final GotCraftKitPvp plugin;

    public ChatListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.custom-format.enabled", true)) {
            return; // Let default chat handle it
        }

        event.setCancelled(true); // We'll handle the formatting

        Player player = event.getPlayer();

        // Get the plain text message
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Get player stats for level
        PlayerStats stats = plugin.getStatsManager().getStats(player);
        int level = stats != null ? stats.getLevel() : 1;

        // Get player balance
        double balance = plugin.getEconomyManager().getBalance(player);

        // Build the formatted chat message
        String formattedMessage = buildChatMessage(player, level, balance, message);

        // Parse MiniMessage format to legacy color codes first, then to Component
        String parsed = plugin.getMessageManager().parseLegacy(formattedMessage);
        Component finalMessage = Component.text(parsed);

        // Send to all players
        plugin.getServer().broadcast(finalMessage);

        // Log to console
        plugin.getLogger().info("[CHAT] " + player.getName() + ": " + message);
    }

    private String buildChatMessage(Player player, int level, double balance, String message) {
        String format = plugin.getConfig().getString("chat.custom-format.format",
                "<dark_gray>[</dark_gray><gradient:#00ffff:#00ff00>Level %level%</gradient><dark_gray>]</dark_gray> <yellow>%player%</yellow><dark_gray>:</dark_gray> <white>%message%</white>");

        // Replace placeholders
        format = format.replace("%player%", player.getName());
        format = format.replace("%level%", String.valueOf(level));
        format = format.replace("%message%", message);
        format = format.replace("%balance%", String.format("%.0f", balance));
        format = format.replace("%money%", String.format("$%.0f", balance));

        // Get player's current stats for additional placeholders
        PlayerStats stats = plugin.getStatsManager().getStats(player);
        if (stats != null) {
            format = format.replace("%kills%", String.valueOf(stats.getKills()));
            format = format.replace("%deaths%", String.valueOf(stats.getDeaths()));
            format = format.replace("%kdr%", stats.getFormattedKDR());
            format = format.replace("%streak%", String.valueOf(stats.getCurrentStreak()));
        }

        // Check for PlaceholderAPI
        if (plugin.getPlaceholderAPIHook() != null) {
            try {
                format = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
            } catch (Exception e) {
                // PlaceholderAPI not available or error
            }
        }

        return format;
    }
}

