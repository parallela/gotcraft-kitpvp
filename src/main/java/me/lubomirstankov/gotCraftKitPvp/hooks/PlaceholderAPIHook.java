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
            default:
                return null;
        }
    }
}

