package me.lubomirstankov.gotCraftKitPvp.scoreboard;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Simple scoreboard implementation that uses teams to hide numbers
 * Optimized to prevent flickering by only updating changed content
 */
public class ProtocolScoreboard {

    private final GotCraftKitPvp plugin;
    private final Player player;
    private Scoreboard scoreboard;
    private Objective objective;
    private boolean created = false;
    private final Map<Integer, String> currentLines = new HashMap<>();
    private String currentTitle = "";

    public ProtocolScoreboard(GotCraftKitPvp plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void create(String title) {
        if (created) {
            return;
        }

        try {
            org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) return;

            scoreboard = manager.getNewScoreboard();

            // Parse the title with colors (MiniMessage or legacy)
            String parsedTitle = parseTitle(title);
            currentTitle = parsedTitle;

            objective = scoreboard.registerNewObjective("kitpvp", Criteria.DUMMY, parsedTitle);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            player.setScoreboard(scoreboard);
            created = true;

            plugin.getLogger().info("Successfully created scoreboard for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create scoreboard: " + e.getMessage());
        }
    }

    public void updateTitle(String title) {
        if (!created || objective == null) {
            return;
        }

        try {
            // Parse the title with colors
            String parsedTitle = parseTitle(title);

            // Only update if title actually changed
            if (!parsedTitle.equals(currentTitle)) {
                objective.setDisplayName(parsedTitle);
                currentTitle = parsedTitle;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update scoreboard title: " + e.getMessage());
        }
    }

    /**
     * Parses title supporting both MiniMessage and legacy color codes
     */
    private String parseTitle(String title) {
        try {
            // First translate legacy codes (&) to section symbols (§)
            String withLegacy = ChatColor.translateAlternateColorCodes('&', title);

            // Then parse MiniMessage tags (like <gradient>, <red>, etc.)
            net.kyori.adventure.text.Component component =
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(withLegacy);

            // Convert to legacy string for scoreboard display
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .serialize(component);
        } catch (Exception e) {
            // Fallback: just use legacy color code translation
            return ChatColor.translateAlternateColorCodes('&', title);
        }
    }

    public void updateLines(List<String> lines) {
        if (!created || scoreboard == null || objective == null) {
            return;
        }

        try {
            // If line count changed, we need to recreate
            boolean needsRecreate = currentLines.size() != lines.size();

            if (!needsRecreate) {
                // Check if any lines actually changed
                for (int i = 0; i < lines.size(); i++) {
                    String newLine = lines.get(i);
                    String oldLine = currentLines.get(i);
                    if (oldLine == null || !newLine.equals(oldLine)) {
                        // At least one line changed, we can update
                        break;
                    }
                }
            }

            if (needsRecreate || currentLines.isEmpty()) {
                // Full recreate when line count changes or first time
                recreateLines(lines);
            } else {
                // Smart update - only change what's different
                updateExistingLines(lines);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update scoreboard lines: " + e.getMessage());
        }
    }

    private void updateExistingLines(List<String> lines) {
        // Update only lines that have changed
        for (int i = 0; i < lines.size(); i++) {
            String newLine = lines.get(i);
            String oldLine = currentLines.get(i);

            if (newLine.equals(oldLine)) {
                continue; // Skip unchanged lines
            }

            Team team = scoreboard.getTeam("line_" + i);
            if (team == null) {
                // Team doesn't exist, recreate all
                recreateLines(lines);
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

            currentLines.put(i, newLine);
        }
    }

    private void recreateLines(List<String> lines) {
        // Clear existing entries and teams
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        for (Team team : new HashSet<>(scoreboard.getTeams())) {
            team.unregister();
        }

        currentLines.clear();

        // Add new lines with scores (higher score = higher on board)
        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Create unique invisible entry using spaces with different lengths
            String entry = " ".repeat(i + 1);

            // Create team to hold the text
            Team team = scoreboard.registerNewTeam("line_" + i);

            // CRITICAL: Set option to hide the entry name completely
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

            // Set the prefix (visible text)
            if (line.length() <= 64) {
                team.setPrefix(line);
            } else {
                team.setPrefix(line.substring(0, 64));
                String suffix = line.substring(64);
                if (suffix.length() > 64) {
                    suffix = suffix.substring(0, 64);
                }
                team.setSuffix(suffix);
            }

            // Add entry to team
            team.addEntry(entry);

            // Set the score (determines position on board)
            objective.getScore(entry).setScore(score--);

            currentLines.put(i, line);
        }
    }

    public void destroy() {
        if (!created) {
            return;
        }

        try {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            created = false;
            currentLines.clear();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to destroy scoreboard: " + e.getMessage());
        }
    }
}

