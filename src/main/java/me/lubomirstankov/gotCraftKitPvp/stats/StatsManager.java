package me.lubomirstankov.gotCraftKitPvp.stats;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();

    public StatsManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getDatabaseManager().loadPlayerStats(uuid, player.getName())
                .thenAccept(stats -> {
                    statsCache.put(uuid, stats);
                    plugin.getLogger().info("Loaded stats for " + player.getName());

                    // Force scoreboard update now that stats are loaded
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getScoreboardManager().updateScoreboard(player);
                        }
                    });
                });
    }

    public void savePlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = statsCache.get(uuid);
        if (stats != null) {
            plugin.getDatabaseManager().savePlayerStats(stats);
        }
    }

    public void saveAllStats() {
        plugin.getLogger().info("Saving all player stats...");

        // Collect all save futures
        java.util.List<java.util.concurrent.CompletableFuture<Void>> saveFutures = new java.util.ArrayList<>();
        for (PlayerStats stats : statsCache.values()) {
            saveFutures.add(plugin.getDatabaseManager().savePlayerStats(stats));
        }

        // Wait for all saves to complete
        if (!saveFutures.isEmpty()) {
            try {
                java.util.concurrent.CompletableFuture.allOf(saveFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
                        .get(10, java.util.concurrent.TimeUnit.SECONDS); // 10 second timeout
                plugin.getLogger().info("All stats saved successfully!");
            } catch (java.util.concurrent.TimeoutException e) {
                plugin.getLogger().warning("Timed out waiting for stats to save!");
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error saving stats!", e);
            }
        } else {
            plugin.getLogger().info("No stats to save.");
        }
    }

    public PlayerStats getStats(Player player) {
        return statsCache.get(player.getUniqueId());
    }

    public PlayerStats getStats(UUID uuid) {
        return statsCache.get(uuid);
    }

    public void handleKill(Player killer, Player victim) {
        PlayerStats killerStats = getStats(killer);
        PlayerStats victimStats = getStats(victim);

        if (killerStats != null) {
            killerStats.addKill();

            // Add XP
            if (plugin.getConfigManager().isLevelingEnabled()) {
                int xpGained = plugin.getConfigManager().getXpPerKill();
                killerStats.addXP(xpGained);

                // Check for level up
                int requiredXP = getRequiredXP(killerStats.getLevel());
                if (killerStats.getXp() >= requiredXP) {
                    killerStats.levelUp();
                    handleLevelUp(killer, killerStats.getLevel());
                }
            }

            // Handle kill streak
            if (plugin.getConfigManager().isKillStreaksEnabled()) {
                handleKillStreak(killer, killerStats.getCurrentStreak());
            }

            // Save stats
            plugin.getDatabaseManager().savePlayerStats(killerStats);
        }

        if (victimStats != null) {
            victimStats.addDeath();

            // Remove XP
            if (plugin.getConfigManager().isLevelingEnabled()) {
                int xpLost = plugin.getConfigManager().getXpPerDeath();
                victimStats.removeXP(xpLost);
            }


            // Save stats
            plugin.getDatabaseManager().savePlayerStats(victimStats);
        }
    }

    private void handleKillStreak(Player player, int streak) {
        String path = "kill-streaks.rewards." + streak;
        if (plugin.getConfig().contains(path) && plugin.getConfig().getBoolean(path + ".enabled", true)) {
            // Broadcast message
            if (plugin.getConfig().getBoolean(path + ".broadcast", false)) {
                String message = plugin.getConfig().getString(path + ".message", "")
                        .replace("%player%", player.getName());

                // Broadcast to all online players using MiniMessage
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    plugin.getMessageManager().sendRawMessage(onlinePlayer, message);
                }
            }

            // Execute commands
            if (plugin.getConfig().contains(path + ".commands")) {
                for (String command : plugin.getConfig().getStringList(path + ".commands")) {
                    command = command.replace("%player%", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                }
            }

            // Apply effects
            if (plugin.getConfig().contains(path + ".effects")) {
                for (String effectString : plugin.getConfig().getStringList(path + ".effects")) {
                    applyPotionEffect(player, effectString);
                }
            }
        }
    }

    private void handleLevelUp(Player player, int level) {
        // Send message
        Map<String, String> placeholders = Map.of("%level%", String.valueOf(level));
        plugin.getMessageManager().sendMessage(player, "level-up", placeholders);

        // Execute reward commands
        String path = "leveling.level-rewards." + level;
        if (plugin.getConfig().contains(path)) {
            for (String command : plugin.getConfig().getStringList(path)) {
                command = command.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            }
        }
    }

    public int getRequiredXP(int level) {
        int base = plugin.getConfig().getInt("leveling.xp-formula.base", 100);
        double multiplier = plugin.getConfig().getDouble("leveling.xp-formula.multiplier", 1.5);
        return (int) (base * Math.pow(multiplier, level - 1));
    }

    private void applyPotionEffect(Player player, String effectString) {
        try {
            String[] parts = effectString.split(":");
            org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(parts[0]);
            int amplifier = Integer.parseInt(parts[1]);
            int duration = Integer.parseInt(parts[2]);

            if (type != null) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amplifier));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid potion effect format: " + effectString);
        }
    }

    public void removeStats(UUID uuid) {
        statsCache.remove(uuid);
    }
}

