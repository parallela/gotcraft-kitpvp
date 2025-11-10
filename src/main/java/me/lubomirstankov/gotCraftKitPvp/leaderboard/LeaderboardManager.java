package me.lubomirstankov.gotCraftKitPvp.leaderboard;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardManager {

    private final GotCraftKitPvp plugin;
    private List<PlayerStats> topKills = new ArrayList<>();
    private List<PlayerStats> topStreaks = new ArrayList<>();
    private List<PlayerStats> topLevels = new ArrayList<>();
    private int taskId;

    public LeaderboardManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;

        if (plugin.getConfigManager().isLeaderboardEnabled()) {
            refreshLeaderboards();
            startRefreshTask();
        }
    }

    private void startRefreshTask() {
        int interval = plugin.getConfigManager().getLeaderboardRefreshInterval();

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::refreshLeaderboards,
                interval * 20L,
                interval * 20L
        ).getTaskId();
    }

    public void refreshLeaderboards() {
        int entries = plugin.getConfig().getInt("leaderboards.entries", 10);

        // Load top players
        plugin.getDatabaseManager().getTopKills(entries).thenAccept(stats -> topKills = stats);
        plugin.getDatabaseManager().getTopStreaks(entries).thenAccept(stats -> topStreaks = stats);
        plugin.getDatabaseManager().getTopLevels(entries).thenAccept(stats -> topLevels = stats);

        plugin.getLogger().info("Leaderboards refreshed!");
    }

    public void reload() {
        if (taskId != 0) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (plugin.getConfigManager().isLeaderboardEnabled()) {
            refreshLeaderboards();
            startRefreshTask();
        }
    }

    public List<PlayerStats> getTopKills() {
        return topKills;
    }

    public List<PlayerStats> getTopStreaks() {
        return topStreaks;
    }

    public List<PlayerStats> getTopLevels() {
        return topLevels;
    }

    public void shutdown() {
        if (taskId != 0) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}

