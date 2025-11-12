package me.lubomirstankov.gotCraftKitPvp.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final GotCraftKitPvp plugin;

    public PlaceholderAPIHook(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kitpvp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lubomirstankov";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // Leaderboard placeholders (don't require player)
        if (params.toLowerCase().startsWith("killer_") ||
            params.toLowerCase().startsWith("kills_") ||
            params.toLowerCase().startsWith("deaths_") ||
            params.toLowerCase().startsWith("streak_") ||
            params.toLowerCase().startsWith("level_") ||
            params.toLowerCase().startsWith("kdr_")) {
            return handleLeaderboardPlaceholder(params);
        }

        // Player-specific placeholders (require player)
        if (player == null) {
            return "";
        }

        PlayerStats stats = plugin.getStatsManager().getStats(player);
        if (stats == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "kills":
                return String.valueOf(stats.getKills());
            case "deaths":
                return String.valueOf(stats.getDeaths());
            case "kdr":
                return stats.getFormattedKDR();
            case "streak":
            case "killstreak":
                return String.valueOf(stats.getCurrentStreak());
            case "best_streak":
            case "beststreak":
                return String.valueOf(stats.getBestStreak());
            case "level":
                return String.valueOf(stats.getLevel());
            case "xp":
                return String.valueOf(stats.getXp());
            case "required_xp":
            case "requiredxp":
                return String.valueOf(plugin.getStatsManager().getRequiredXP(stats.getLevel()));
            case "kit":
                String activeKit = plugin.getKitManager().getActiveKit(player);
                if (activeKit != null) {
                    var kit = plugin.getKitManager().getKit(activeKit);
                    return kit != null ? kit.getName() : "None";
                }
                return "None";
            case "balance":
            case "money":
                return String.format("%.2f", plugin.getEconomyManager().getBalance(player));
            default:
                return null;
        }
    }

    /**
     * Handle leaderboard placeholders
     * Examples:
     * - %kitpvp_killer_1% - Name of #1 killer
     * - %kitpvp_kills_1% - Kills of #1 player
     * - %kitpvp_deaths_1% - Deaths of #1 player
     * - %kitpvp_streak_1% - Best streak of #1 player
     * - %kitpvp_level_1% - Level of #1 player
     * - %kitpvp_kdr_1% - K/D ratio of #1 player
     */
    private String handleLeaderboardPlaceholder(String params) {
        String[] parts = params.toLowerCase().split("_");
        if (parts.length != 2) {
            return "";
        }

        String type = parts[0]; // killer, kills, deaths, streak, level, kdr
        int position;

        try {
            position = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return "";
        }

        if (position < 1 || position > 100) {
            return "";
        }

        // Get the appropriate leaderboard based on type
        java.util.List<PlayerStats> leaderboard;

        switch (type) {
            case "killer":
            case "kills":
                leaderboard = plugin.getLeaderboardManager().getTopKills();
                break;
            case "deaths":
                // For deaths, we use top kills leaderboard (contains all stats)
                leaderboard = plugin.getLeaderboardManager().getTopKills();
                break;
            case "streak":
                leaderboard = plugin.getLeaderboardManager().getTopStreaks();
                break;
            case "level":
                leaderboard = plugin.getLeaderboardManager().getTopLevels();
                break;
            case "kdr":
                leaderboard = plugin.getLeaderboardManager().getTopKills();
                break;
            default:
                return "";
        }

        if (leaderboard == null || leaderboard.isEmpty()) {
            return type.equals("killer") ? "None" : "0";
        }

        // Get the player at the specified position (position is 1-based)
        int index = position - 1;
        if (index >= leaderboard.size()) {
            return type.equals("killer") ? "None" : "0";
        }

        PlayerStats stats = leaderboard.get(index);
        if (stats == null) {
            return type.equals("killer") ? "None" : "0";
        }

        // Return the appropriate value based on type
        switch (type) {
            case "killer":
                // Return player name
                String name = stats.getName();
                return name != null ? name : "Unknown";
            case "kills":
                return String.valueOf(stats.getKills());
            case "deaths":
                return String.valueOf(stats.getDeaths());
            case "streak":
                return String.valueOf(stats.getBestStreak());
            case "level":
                return String.valueOf(stats.getLevel());
            case "kdr":
                return stats.getFormattedKDR();
            default:
                return "";
        }
    }
}

