package me.lubomirstankov.gotCraftKitPvp.economy;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in economy system for GotCraftKitPvp
 * No external economy plugin required!
 */
public class EconomyManager {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final double startingBalance;

    public EconomyManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        this.startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 0.0);
    }

    /**
     * Get a player's balance
     */
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, startingBalance);
    }

    /**
     * Get a player's balance
     */
    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    /**
     * Set a player's balance
     */
    public void setBalance(UUID uuid, double amount) {
        if (amount < 0) {
            amount = 0;
        }
        balances.put(uuid, amount);
        saveBalanceAsync(uuid, amount);
    }

    /**
     * Set a player's balance
     */
    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }

    /**
     * Add money to a player's balance
     */
    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        double currentBalance = getBalance(uuid);
        double newBalance = currentBalance + amount;
        setBalance(uuid, newBalance);
        return true;
    }

    /**
     * Add money to a player's balance
     */
    public boolean deposit(Player player, double amount) {
        return deposit(player.getUniqueId(), amount);
    }

    /**
     * Remove money from a player's balance
     */
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) {
            return false;
        }
        double currentBalance = getBalance(uuid);
        if (currentBalance < amount) {
            return false; // Not enough money
        }
        double newBalance = currentBalance - amount;
        setBalance(uuid, newBalance);
        return true;
    }

    /**
     * Remove money from a player's balance
     */
    public boolean withdraw(Player player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    /**
     * Check if a player has enough money
     */
    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    /**
     * Check if a player has enough money
     */
    public boolean has(Player player, double amount) {
        return has(player.getUniqueId(), amount);
    }

    /**
     * Load a player's balance from database
     * FIXED: Properly gives starting balance to new players
     */
    public void loadBalance(UUID uuid) {
        // Track if this is first load for this player
        boolean isNewPlayer = !balances.containsKey(uuid);

        // Immediately set starting balance to prevent showing 0 or placeholders
        if (isNewPlayer) {
            balances.put(uuid, startingBalance);
        }

        // Then load actual balance from database and update
        plugin.getDatabaseManager().getPlayerMoney(uuid).thenAccept(balance -> {
            // Run on main thread to ensure thread safety
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (balance != null && balance > 0.01) {
                    // Player has actual saved balance - use it
                    balances.put(uuid, balance);
                    plugin.getLogger().info("Loaded balance for " + uuid + ": $" + String.format("%.2f", balance));
                } else if (isNewPlayer) {
                    // New player (was not in cache) and database has 0 or null
                    // Give starting balance and save it
                    plugin.getLogger().info("New player " + uuid + ", giving starting balance: $" + String.format("%.2f", startingBalance));
                    saveBalanceAsync(uuid, startingBalance);
                } else {
                    // Existing player with 0 balance (spent all money)
                    balances.put(uuid, balance != null ? balance : 0.0);
                    plugin.getLogger().info("Loaded balance for " + uuid + ": $" + String.format("%.2f", balance != null ? balance : 0.0));
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to load balance for " + uuid + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Save a player's balance to database (async)
     */
    private void saveBalanceAsync(UUID uuid, double balance) {
        plugin.getDatabaseManager().savePlayerMoney(uuid, balance);
    }

    /**
     * Save a player's current balance (used when player quits)
     */
    public void savePlayerBalance(Player player) {
        UUID uuid = player.getUniqueId();
        Double balance = balances.get(uuid);
        if (balance != null) {
            saveBalanceAsync(uuid, balance);
            plugin.getLogger().info("Saved balance for " + player.getName() + ": $" + balance);
        }
    }

    /**
     * Save a player's current balance (used when player quits)
     */
    public void savePlayerBalance(UUID uuid) {
        Double balance = balances.get(uuid);
        if (balance != null) {
            saveBalanceAsync(uuid, balance);
            plugin.getLogger().info("Saved balance for " + uuid + ": $" + balance);
        }
    }

    /**
     * Save all balances to database
     * CRITICAL: Now waits for all saves to complete (max 3 seconds to avoid watchdog)
     */
    public void saveAll() {
        if (balances.isEmpty()) {
            plugin.getLogger().info("No player balances to save");
            return;
        }

        plugin.getLogger().info("Saving " + balances.size() + " player balances...");

        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            futures.add(plugin.getDatabaseManager().savePlayerMoney(entry.getKey(), entry.getValue()));
        }

        // Wait for all saves to complete (reduced timeout to avoid watchdog)
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(3, java.util.concurrent.TimeUnit.SECONDS);
            plugin.getLogger().info("Saved " + balances.size() + " player balances successfully!");
        } catch (java.util.concurrent.TimeoutException e) {
            plugin.getLogger().warning("Save timeout - data will be flushed by database manager");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "CRITICAL: Error saving player balances!", e);
        }
    }

    /**
     * Remove player from cache (after data is saved)
     */
    public void removeFromCache(UUID uuid) {
        balances.remove(uuid);
        plugin.getLogger().fine("Removed balance cache for " + uuid);
    }

    /**
     * Format money amount for display
     */
    public String format(double amount) {
        return String.format("$%.2f", amount);
    }
}
