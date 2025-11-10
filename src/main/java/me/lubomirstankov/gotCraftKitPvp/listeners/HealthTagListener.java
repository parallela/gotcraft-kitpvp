package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Displays player health next to their nametag
 */
public class HealthTagListener implements Listener {

    private final GotCraftKitPvp plugin;

    public HealthTagListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Small delay to ensure scoreboard is set up
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateHealthDisplay(player);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Update health display after damage is applied
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateHealthDisplay(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Update health display after health is regained
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            updateHealthDisplay(player);
        });
    }

    /**
     * Update the health display for a player's nametag
     */
    public void updateHealthDisplay(Player player) {
        if (!plugin.getConfig().getBoolean("health-display.enabled", true)) {
            return;
        }

        // Update health display for all online players' view of this player
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            updateHealthDisplayForViewer(viewer, player);
        }
    }

    /**
     * Update how a viewer sees a player's health
     */
    private void updateHealthDisplayForViewer(Player viewer, Player player) {
        Scoreboard scoreboard = viewer.getScoreboard();
        if (scoreboard == null) {
            scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
            viewer.setScoreboard(scoreboard);
        }

        String teamName = "hp_" + player.getName().substring(0, Math.min(12, player.getName().length()));
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Add player to team if not already in it
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        // Get health info
        double health = player.getHealth();
        @SuppressWarnings("deprecation")
        double maxHealth = player.getMaxHealth();
        int healthPercent = (int) ((health / maxHealth) * 100);

        // Determine color based on health percentage
        ChatColor healthColor;
        if (healthPercent >= 75) {
            healthColor = ChatColor.GREEN;
        } else if (healthPercent >= 50) {
            healthColor = ChatColor.YELLOW;
        } else if (healthPercent >= 25) {
            healthColor = ChatColor.GOLD;
        } else {
            healthColor = ChatColor.RED;
        }

        // Get display format from config
        String format = plugin.getConfig().getString("health-display.format", "hearts");
        String healthText;

        if (format.equalsIgnoreCase("hearts")) {
            // Display as hearts (half hearts)
            double hearts = health / 2.0;
            healthText = String.format("%.1f❤", hearts);
        } else if (format.equalsIgnoreCase("percentage")) {
            // Display as percentage
            healthText = healthPercent + "%";
        } else {
            // Display as number
            healthText = String.format("%.0f", health);
        }

        // Set prefix or suffix based on config
        String position = plugin.getConfig().getString("health-display.position", "suffix");

        if (position.equalsIgnoreCase("prefix")) {
            team.setPrefix(healthColor + "[" + healthText + "] " + ChatColor.RESET);
            team.setSuffix("");
        } else {
            team.setPrefix("");
            team.setSuffix(" " + healthColor + "[" + healthText + "]");
        }
    }

    /**
     * Update health display for all online players
     */
    public void updateAllHealthDisplays() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updateHealthDisplay(player);
        }
    }
}

