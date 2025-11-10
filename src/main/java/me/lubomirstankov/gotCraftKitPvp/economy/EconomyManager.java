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
     */
    public void loadBalance(UUID uuid) {
        plugin.getDatabaseManager().getPlayerMoney(uuid).thenAccept(balance -> {
            if (balance != null) {
                balances.put(uuid, balance);
                plugin.getLogger().info("Loaded balance for " + uuid + ": $" + balance);
            } else {
                // New player - set starting balance
                balances.put(uuid, startingBalance);
                saveBalanceAsync(uuid, startingBalance);
            }
        });
    }

    /**
     * Save a player's balance to database (async)
     */
    private void saveBalanceAsync(UUID uuid, double balance) {
        plugin.getDatabaseManager().savePlayerMoney(uuid, balance);
    }

    /**
     * Save all balances to database
     */
    public void saveAll() {
        plugin.getLogger().info("Saving all player balances...");
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            saveBalanceAsync(entry.getKey(), entry.getValue());
        }
        plugin.getLogger().info("Saved " + balances.size() + " player balances!");
    }

    /**
     * Format money amount for display
     */
    public String format(double amount) {
        return String.format("$%.2f", amount);
    }
}

