package me.lubomirstankov.gotCraftKitPvp.commands;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor {

    private final GotCraftKitPvp plugin;

    public StatsCommand(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
            return true;
        }

        Player player = (Player) sender;
        Player target = player;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getMessageManager().getPlayerNotFound());
                return true;
            }
        }

        plugin.getGuiManager().openStatsGUI(player, target);
        return true;
    }
}

