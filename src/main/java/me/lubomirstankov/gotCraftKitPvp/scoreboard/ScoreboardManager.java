package me.lubomirstankov.gotCraftKitPvp.scoreboard;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Scoreboard Manager using PacketEvents API 2.10.1
 * Completely hides red numbers on the right side
 */
public class ScoreboardManager {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, PacketScoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, Boolean> hiddenScoreboards = new HashMap<>();
    private int taskId;

    public ScoreboardManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Using PacketEvents API for scoreboard (numbers completely hidden)");

        if (plugin.getConfigManager().isScoreboardEnabled()) {
            startUpdateTask();
        }
    }

    private void startUpdateTask() {
        int interval = plugin.getConfigManager().getScoreboardUpdateInterval();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 0L, interval).getTaskId();
    }

    public void createScoreboard(Player player) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            return;
        }

        // Check if player has scoreboard hidden
        if (hiddenScoreboards.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        try {
            PacketScoreboard scoreboard = new PacketScoreboard(plugin, player);
            scoreboard.create(getTitle());
            playerScoreboards.put(player.getUniqueId(), scoreboard);

            // Initial update
            updateScoreboard(player);

            plugin.getLogger().info("Created PacketEvents scoreboard for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create scoreboard for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateScoreboard(Player player) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            return;
        }

        // Check if player has scoreboard hidden
        if (hiddenScoreboards.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        PacketScoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            return;
        }

        try {
            // Get and format lines
            List<String> lines = getFormattedLines(player);

            // Don't update if stats aren't loaded yet (empty list)
            if (lines.isEmpty()) {
                return; // Wait until next update when stats are loaded
            }

            // Update title and lines - PacketScoreboard has its own anti-flicker
            scoreboard.updateTitle(getTitle());
            scoreboard.updateLines(lines);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update scoreboard for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void removeScoreboard(Player player) {
        PacketScoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.destroy();
        }
        hiddenScoreboards.remove(player.getUniqueId());
    }

    public boolean toggleScoreboard(Player player) {
        boolean currentlyHidden = hiddenScoreboards.getOrDefault(player.getUniqueId(), false);
        boolean newState = !currentlyHidden;
        hiddenScoreboards.put(player.getUniqueId(), newState);

        if (newState) {
            // Hide scoreboard
            PacketScoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
            if (scoreboard != null) {
                scoreboard.destroy();
            }
        } else {
            // Show scoreboard
            createScoreboard(player);
        }

        return !newState; // Return true if now visible
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTask(taskId);

        // Remove all scoreboards
        for (PacketScoreboard scoreboard : playerScoreboards.values()) {
            scoreboard.destroy();
        }
        playerScoreboards.clear();
        hiddenScoreboards.clear();
    }

    private String getTitle() {
        return plugin.getConfigManager().getScoreboardTitle();
    }

    private List<String> getFormattedLines(Player player) {
        List<String> configLines = plugin.getConfigManager().getScoreboardLines();
        List<String> formatted = new ArrayList<>();

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) {
            // Stats not loaded yet - return empty to prevent showing wrong placeholders
            return new ArrayList<>();
        }

        for (String line : configLines) {
            formatted.add(formatLine(line, player, stats));
        }

        return formatted;
    }

    private String formatLine(String line, Player player, PlayerStats stats) {
        // Get money from EconomyManager instead of PlayerStats
        double money = plugin.getEconomyManager().getBalance(player);

        // Replace internal placeholders
        String formatted = line
            .replace("%player%", player.getName())
            .replace("%kit%", getKitName(player))
            .replace("%kills%", String.valueOf(stats.getKills()))
            .replace("%deaths%", String.valueOf(stats.getDeaths()))
            .replace("%kdr%", String.format("%.2f", stats.getKDR()))
            .replace("%streak%", String.valueOf(stats.getCurrentStreak()))
            .replace("%best_streak%", String.valueOf(stats.getBestStreak()))
            .replace("%level%", String.valueOf(stats.getLevel()))
            .replace("%xp%", String.valueOf(stats.getXp()))
            .replace("%money%", String.format("%.0f", money));

        // Parse PlaceholderAPI placeholders if available
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formatted);
        }

        // Parse MiniMessage formatting
        return plugin.getMessageManager().parseLegacy(formatted);
    }

    private String getKitName(Player player) {
        String kitId = plugin.getKitManager().getActiveKit(player);
        if (kitId != null) {
            var kit = plugin.getKitManager().getKit(kitId);
            if (kit != null) {
                return kit.getName();
            }
        }
        return "None";
    }

    public boolean isScoreboardHidden(Player player) {
        return hiddenScoreboards.getOrDefault(player.getUniqueId(), false);
    }
}

