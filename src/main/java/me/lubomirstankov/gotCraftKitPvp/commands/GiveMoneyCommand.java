package me.lubomirstankov.gotCraftKitPvp.commands;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GiveMoneyCommand implements CommandExecutor, TabCompleter {

    private final GotCraftKitPvp plugin;

    public GiveMoneyCommand(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Console is always allowed (for automated rewards)
        if (sender instanceof Player && !sender.hasPermission("kitpvp.command.givemoney")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /givemoney <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[1]);
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cAmount must be positive!");
            return true;
        }

        try {
            boolean success = plugin.getEconomyManager().deposit(target, amount);

            if (success) {
                String successMsg = String.format("§aGave §e$%.2f §ato §b%s", amount, target.getName());
                sender.sendMessage(successMsg);

                String receivedMsg = String.format("§aYou received §e$%.2f§a!", amount);
                target.sendMessage(receivedMsg);

                plugin.getLogger().info("GiveMoney: " + sender.getName() + " gave $" + amount + " to " + target.getName());
            } else {
                sender.sendMessage("§cFailed to give money!");
            }
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while giving money!");
            plugin.getLogger().severe("Error in GiveMoney command: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return List.of("100", "500", "1000", "5000", "10000");
        }

        return completions;
    }
}

