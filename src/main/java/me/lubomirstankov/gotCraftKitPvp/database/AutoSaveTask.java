package me.lubomirstankov.gotCraftKitPvp.database;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Automatic periodic save task to prevent data loss
 *
 * CRITICAL SAFETY FEATURE:
 * - Saves all player data every X minutes
 * - Prevents data loss from server crashes
 * - Runs independently of player quit events
 */
public class AutoSaveTask extends BukkitRunnable {

    private final GotCraftKitPvp plugin;
    private int saveCount = 0;

    public AutoSaveTask(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        saveCount++;

        long startTime = System.currentTimeMillis();
        plugin.getLogger().info("==============================================");
        plugin.getLogger().info("AutoSave #" + saveCount + " started...");

        try {
            // Save all stats
            if (plugin.getStatsManager() != null) {
                plugin.getStatsManager().saveAllStats();
            }

            // Save all economy data
            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().saveAll();
            }

            // Flush database writes
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().flushPendingWrites();
            }

            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("AutoSave #" + saveCount + " completed in " + duration + "ms");
            plugin.getLogger().info("==============================================");

        } catch (Exception e) {
            plugin.getLogger().severe("CRITICAL: AutoSave #" + saveCount + " FAILED!");
            plugin.getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
            plugin.getLogger().severe("==============================================");
        }
    }
}

