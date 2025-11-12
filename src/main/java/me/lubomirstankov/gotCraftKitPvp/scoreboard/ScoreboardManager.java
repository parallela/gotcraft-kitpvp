package me.lubomirstankov.gotCraftKitPvp.scoreboard;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, ProtocolScoreboard> protocolScoreboards = new HashMap<>();
    private final Map<UUID, Boolean> hiddenScoreboards = new HashMap<>();
    private final Map<UUID, Map<Integer, String>> playerLines = new HashMap<>();
    private final Map<UUID, String> playerTitles = new HashMap<>();
    private final boolean useProtocolLib;
    private int titleIndex = 0;
    private int taskId;

    public ScoreboardManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;

        // Use ProtocolLib if available for number-free scoreboards
        this.useProtocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;

        if (useProtocolLib) {
            plugin.getLogger().info("ProtocolLib detected! Using packet-listener scoreboard (numbers completely hidden)");
        } else {
            plugin.getLogger().info("ProtocolLib not found. Using standard scoreboard");
        }

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

        if (useProtocolLib) {
            ProtocolScoreboard protocolBoard = new ProtocolScoreboard(plugin, player);
            protocolBoard.create(getTitle());
            protocolScoreboards.put(player.getUniqueId(), protocolBoard);
            plugin.getLogger().info("Created scoreboard (protocolLib) for player " + player.getName());
        } else {
            org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) return;

            Scoreboard scoreboard = manager.getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("kitpvp", Criteria.DUMMY, getTitle());
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            player.setScoreboard(scoreboard);
            playerScoreboards.put(player.getUniqueId(), scoreboard);
            playerTitles.put(player.getUniqueId(), getTitle());
            plugin.getLogger().info("Created scoreboard for player " + player.getName());
        }
    }

    public void updateScoreboard(Player player) {
        PlayerStats stats = plugin.getStatsManager().getStats(player);
        if (stats == null) return;

        List<String> configLines = plugin.getConfig().getStringList("scoreboard.lines");
        List<String> formattedLines = new ArrayList<>();
        for (String line : configLines) {
            formattedLines.add(formatLine(player, stats, line));
        }

        String title = getTitle();

        if (useProtocolLib) {
            ProtocolScoreboard protocolBoard = protocolScoreboards.get(player.getUniqueId());
            if (protocolBoard == null) {
                createScoreboard(player);
                return;
            }
            protocolBoard.updateTitle(title);
            protocolBoard.updateLines(formattedLines);
        } else {
            Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
            if (scoreboard == null) {
                createScoreboard(player);
                return;
            }

            Objective objective = scoreboard.getObjective("kitpvp");
            if (objective == null) return;

            UUID uuid = player.getUniqueId();

            // Update title only if changed
            String oldTitle = playerTitles.get(uuid);
            if (!title.equals(oldTitle)) {
                objective.setDisplayName(title);
                playerTitles.put(uuid, title);
            }

            // Get previous lines
            Map<Integer, String> previousLines = playerLines.computeIfAbsent(uuid, k -> new HashMap<>());

            // Check if we need to recreate (line count changed)
            boolean needsRecreate = previousLines.size() != formattedLines.size();

            if (needsRecreate) {
                // Full recreate when line count changes
                recreateStandardScoreboard(scoreboard, objective, formattedLines, uuid);
            } else {
                // Smart update - only change what's different
                updateStandardScoreboardLines(scoreboard, formattedLines, previousLines, uuid);
            }
        }
    }

    private void updateStandardScoreboardLines(Scoreboard scoreboard, List<String> newLines, Map<Integer, String> previousLines, UUID uuid) {
        // Update only lines that have changed
        for (int i = 0; i < newLines.size(); i++) {
            String newLine = newLines.get(i);
            String oldLine = previousLines.get(i);

            if (newLine.equals(oldLine)) {
                continue; // Skip unchanged lines
            }

            Team team = scoreboard.getTeam("line_" + i);
            if (team == null) {
                // Team doesn't exist, recreate all
                recreateStandardScoreboard(scoreboard, scoreboard.getObjective("kitpvp"), newLines, uuid);
                return;
            }

            // Update the team's prefix/suffix
            if (newLine.length() <= 64) {
                team.setPrefix(newLine);
                team.setSuffix(""); // Clear suffix
            } else {
                team.setPrefix(newLine.substring(0, 64));
                String suffix = newLine.substring(64);
                if (suffix.length() > 64) {
                    suffix = suffix.substring(0, 64);
                }
                team.setSuffix(suffix);
            }

            previousLines.put(i, newLine);
        }
    }

    private void recreateStandardScoreboard(Scoreboard scoreboard, Objective objective, List<String> formattedLines, UUID uuid) {
        // Clear existing teams and entries
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        Map<Integer, String> lineMap = new HashMap<>();

        int score = formattedLines.size();
        for (int i = 0; i < formattedLines.size(); i++) {
            String formatted = formattedLines.get(i);

            // Create unique invisible entry using spaces with different lengths
            String entry = " ".repeat(i + 1);

            Team team = scoreboard.registerNewTeam("line_" + i);

            // CRITICAL: Set option to hide the entry name completely
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

            team.addEntry(entry);

            if (formatted.length() <= 64) {
                team.setPrefix(formatted);
            } else {
                team.setPrefix(formatted.substring(0, 64));
                String suffix = formatted.substring(64);
                if (suffix.length() > 64) {
                    suffix = suffix.substring(0, 64);
                }
                team.setSuffix(suffix);
            }

            objective.getScore(entry).setScore(score--);
            lineMap.put(i, formatted);
        }

        playerLines.put(uuid, lineMap);
    }

    private String getTitle() {
        List<String> titles = plugin.getConfig().getStringList("scoreboard.title");
        if (titles.isEmpty()) {
            String title = plugin.getConfig().getString("scoreboard.title");
            if (title == null || title.isEmpty()) {
                return ChatColor.GOLD + "" + ChatColor.BOLD + "KITPVP";
            }
            return plugin.getMessageManager().parseLegacy(title);
        }

        if (titleIndex >= titles.size()) {
            titleIndex = 0;
        }

        String title = titles.get(titleIndex);
        return plugin.getMessageManager().parseLegacy(title);
    }

    private String formatLine(Player player, PlayerStats stats, String line) {
        String formatted = line;

        formatted = formatted.replace("%player%", player.getName());
        formatted = formatted.replace("%kills%", String.valueOf(stats.getKills()));
        formatted = formatted.replace("%deaths%", String.valueOf(stats.getDeaths()));
        formatted = formatted.replace("%kdr%", stats.getFormattedKDR());
        formatted = formatted.replace("%streak%", String.valueOf(stats.getCurrentStreak()));
        formatted = formatted.replace("%best_streak%", String.valueOf(stats.getBestStreak()));
        formatted = formatted.replace("%level%", String.valueOf(stats.getLevel()));
        formatted = formatted.replace("%xp%", String.valueOf(stats.getXp()));
        formatted = formatted.replace("%required_xp%", String.valueOf(plugin.getStatsManager().getRequiredXP(stats.getLevel())));

        // Money placeholder (built-in economy)
        double balance = plugin.getEconomyManager().getBalance(player);
        formatted = formatted.replace("%money%", String.format("%.2f", balance));
        formatted = formatted.replace("%balance%", String.format("%.2f", balance));

        String activeKit = plugin.getKitManager().getActiveKit(player);
        if (activeKit != null) {
            var kit = plugin.getKitManager().getKit(activeKit);
            formatted = formatted.replace("%kit%", kit != null ? ChatColor.stripColor(kit.getName()) : "None");
        } else {
            formatted = formatted.replace("%kit%", "None");
        }

        if (plugin.getPlaceholderAPIHook() != null) {
            try {
                formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formatted);
            } catch (Exception e) {
                // PlaceholderAPI not available or error
            }
        }

        formatted = plugin.getMessageManager().parseLegacy(formatted);
        return formatted;
    }

    public void removeScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        if (useProtocolLib) {
            ProtocolScoreboard protocolBoard = protocolScoreboards.remove(uuid);
            if (protocolBoard != null) {
                protocolBoard.destroy();
            }
        } else {
            playerScoreboards.remove(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        // Clean up tracking maps
        playerLines.remove(uuid);
        playerTitles.remove(uuid);
    }

    /**
     * Toggle scoreboard visibility for a player
     * @param player The player to toggle scoreboard for
     * @return true if scoreboard is now visible, false if hidden
     */
    public boolean toggleScoreboard(Player player) {
        boolean currentlyHidden = hiddenScoreboards.getOrDefault(player.getUniqueId(), false);
        boolean newHiddenState = !currentlyHidden;

        if (newHiddenState) {
            hideScoreboard(player);
        } else {
            showScoreboard(player);
        }

        return !newHiddenState; // Return true if now visible
    }

    /**
     * Hide the scoreboard for a player
     */
    public void hideScoreboard(Player player) {
        hiddenScoreboards.put(player.getUniqueId(), true);
        removeScoreboard(player);
    }

    /**
     * Show the scoreboard for a player
     */
    public void showScoreboard(Player player) {
        hiddenScoreboards.put(player.getUniqueId(), false);
        createScoreboard(player);
        updateScoreboard(player);
    }

    /**
     * Check if a player has their scoreboard hidden
     */
    public boolean isScoreboardHidden(Player player) {
        return hiddenScoreboards.getOrDefault(player.getUniqueId(), false);
    }


    public void shutdown() {
        if (taskId != 0) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (useProtocolLib) {
            for (ProtocolScoreboard board : protocolScoreboards.values()) {
                board.destroy();
            }
            protocolScoreboards.clear();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
        }

        playerScoreboards.clear();
        playerLines.clear();
        playerTitles.clear();
    }
}

