package me.lubomirstankov.gotCraftKitPvp.hooks;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final GotCraftKitPvp plugin;
    private Economy economy;

    public VaultHook(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy provider found! Make sure you have an economy plugin (EssentialsX, CMI, etc.)");
            return false;
        }

        economy = rsp.getProvider();
        if (economy != null) {
            plugin.getLogger().info("Successfully hooked into economy: " + economy.getName());
            return true;
        } else {
            plugin.getLogger().warning("Economy provider is null!");
            return false;
        }
    }

    public double getBalance(Player player) {
        if (economy == null) {
            return 0;
        }
        return economy.getBalance(player);
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean depositMoney(Player player, double amount) {
        if (economy == null) {
            plugin.getLogger().severe("Cannot deposit money: Economy is null!");
            return false;
        }

        try {
            plugin.getLogger().info("Attempting to deposit $" + amount + " to " + player.getName());
            plugin.getLogger().info("Current balance: $" + economy.getBalance(player));

            var response = economy.depositPlayer(player, amount);

            plugin.getLogger().info("Deposit response: " + response.type + " - " + response.errorMessage);
            plugin.getLogger().info("New balance: $" + economy.getBalance(player));

            if (!response.transactionSuccess()) {
                plugin.getLogger().severe("Deposit failed! Error: " + response.errorMessage);
            }

            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().severe("Exception during deposit: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasMoney(Player player, double amount) {
        if (economy == null) {
            return false;
        }
        return economy.has(player, amount);
    }
}

