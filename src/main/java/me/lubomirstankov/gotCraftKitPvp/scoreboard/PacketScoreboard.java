package me.lubomirstankov.gotCraftKitPvp.scoreboard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Modern packet-based scoreboard using PacketEvents API 2.10.1
 * Completely hides red numbers on the right side
 */
public class PacketScoreboard {

    private final GotCraftKitPvp plugin;
    private final Player player;
    private final String objectiveName = "kitpvp";

    private boolean created = false;
    private final Map<Integer, String> currentLines = new HashMap<>();
    private Component currentTitle = Component.empty();

    public PacketScoreboard(GotCraftKitPvp plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Create the scoreboard for the player
     */
    public void create(String titleText) {
        if (created) {
            return;
        }

        try {
            // Parse title from config (MiniMessage or legacy)
            currentTitle = parseComponent(titleText);

            // Create objective packet with BLANK score format to hide numbers
            WrapperPlayServerScoreboardObjective objectivePacket = new WrapperPlayServerScoreboardObjective(
                objectiveName,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
                currentTitle,
                WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                    ScoreFormat.blankScore()
            );

            // Send create objective packet
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, objectivePacket);

            // Display on sidebar
            WrapperPlayServerDisplayScoreboard displayPacket = new WrapperPlayServerDisplayScoreboard(
                1, // SIDEBAR position
                objectiveName
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, displayPacket);

            created = true;
            plugin.getLogger().info("Created PacketEvents scoreboard for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create PacketEvents scoreboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update the scoreboard title
     */
    public void updateTitle(String titleText) {
        if (!created) {
            return;
        }

        try {
            Component newTitle = parseComponent(titleText);

            // Only update if title changed
            if (!newTitle.equals(currentTitle)) {
                currentTitle = newTitle;

                WrapperPlayServerScoreboardObjective objectivePacket = new WrapperPlayServerScoreboardObjective(
                    objectiveName,
                    WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
                    currentTitle,
                    WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                    ScoreFormat.blankScore() // Keep numbers hidden
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, objectivePacket);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update scoreboard title: " + e.getMessage());
        }
    }

    /**
     * Update scoreboard lines with anti-flicker optimization
     * Only sends packets for lines that actually changed
     */
    public void updateLines(List<String> lines) {
        if (!created) {
            return;
        }

        try {
            // Quick check: if nothing changed at all, skip entirely
            if (currentLines.size() == lines.size()) {
                boolean anyChanged = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).equals(currentLines.get(i))) {
                        anyChanged = true;
                        break;
                    }
                }
                if (!anyChanged) {
                    return; // Nothing changed, no packets needed!
                }
            }

            // Handle size changes (lines added/removed)
            if (currentLines.size() != lines.size()) {
                // Remove extra lines if we have fewer now
                if (lines.size() < currentLines.size()) {
                    for (int i = lines.size(); i < currentLines.size(); i++) {
                        String entry = getEntry(i);
                        WrapperPlayServerResetScore resetPacket = new WrapperPlayServerResetScore(
                            entry,
                            objectiveName
                        );
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, resetPacket);
                        currentLines.remove(i);
                    }
                }
            }

            // Update only changed lines (anti-flicker!)
            int score = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                String lineText = lines.get(i);
                String oldLine = currentLines.get(i);

                // Skip if line hasn't changed (ANTI-FLICKER!)
                if (lineText.equals(oldLine)) {
                    score--;
                    continue;
                }

                String entry = getEntry(i);
                Component lineComponent = parseComponent(lineText);

                // Only update this specific line
                WrapperPlayServerUpdateScore scorePacket = new WrapperPlayServerUpdateScore(
                    entry,
                    WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                    objectiveName,
                    score--,
                    lineComponent,
                    ScoreFormat.blankScore() // BLANK format = no red numbers!
                );

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, scorePacket);
                currentLines.put(i, lineText);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update scoreboard lines: " + e.getMessage());
        }
    }

    /**
     * Destroy the scoreboard
     */
    public void destroy() {
        if (!created) {
            return;
        }

        try {
            // Remove objective
            WrapperPlayServerScoreboardObjective objectivePacket = new WrapperPlayServerScoreboardObjective(
                objectiveName,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
                currentTitle,
                WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                null
            );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, objectivePacket);

            created = false;
            currentLines.clear();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to destroy scoreboard: " + e.getMessage());
        }
    }

    /**
     * Generate unique entry for each line using invisible characters
     */
    private String getEntry(int index) {
        // Use repeated spaces to create unique entries
        return " ".repeat(index + 1);
    }

    /**
     * Parse text to Component (supports MiniMessage and legacy codes)
     */
    private Component parseComponent(String text) {
        try {
            // Use the plugin's message manager to parse
            String parsed = plugin.getMessageManager().parseLegacy(text);

            // Convert legacy string back to Component
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(parsed);
        } catch (Exception e) {
            // Fallback: return plain text
            return Component.text(text);
        }
    }
}

