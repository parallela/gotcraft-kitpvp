package me.lubomirstankov.gotCraftKitPvp.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubCommand implements CommandExecutor {

    private final GotCraftKitPvp plugin;

    public HubCommand(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
            return true;
        }

        Player player = (Player) sender;

        // Check if hub command is enabled
        if (!plugin.getConfig().getBoolean("hub.enabled", true)) {
            plugin.getMessageManager().sendRawMessage(player, "<red>Hub command is disabled!");
            return true;
        }

        // Get hub mode
        String mode = plugin.getConfig().getString("hub.mode", "server");

        // Send hub message
        String message = plugin.getConfig().getString("hub.message", "<green>Sending you to the hub...</green>");
        plugin.getMessageManager().sendRawMessage(player, message);

        // Handle based on mode
        if (mode.equalsIgnoreCase("server")) {
            // BungeeCord/Velocity mode - send player to hub server
            sendToServer(player);
        } else if (mode.equalsIgnoreCase("world")) {
            // World mode - teleport to configured location
            teleportToHub(player);
        } else {
            plugin.getLogger().warning("Invalid hub mode: " + mode + ". Use 'server' or 'world'");
            plugin.getMessageManager().sendRawMessage(player, "<red>Hub is misconfigured! Contact an administrator.");
        }

        return true;
    }

    /**
     * Send player to hub server via BungeeCord/Velocity
     */
    private void sendToServer(Player player) {
        String serverName = plugin.getConfig().getString("hub.server-name", "hub");

        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send player to hub server: " + e.getMessage());
            plugin.getMessageManager().sendRawMessage(player, "<red>Failed to connect to hub! Contact an administrator.");
        }
    }

    /**
     * Teleport player to hub location in world
     */
    private void teleportToHub(Player player) {
        try {
            // Get hub location from config
            String worldName = plugin.getConfig().getString("hub.location.world", "world");
            double x = plugin.getConfig().getDouble("hub.location.x", 0);
            double y = plugin.getConfig().getDouble("hub.location.y", 100);
            double z = plugin.getConfig().getDouble("hub.location.z", 0);
            float yaw = (float) plugin.getConfig().getDouble("hub.location.yaw", 0);
            float pitch = (float) plugin.getConfig().getDouble("hub.location.pitch", 0);

            // Get world
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Hub world not found: " + worldName);
                plugin.getMessageManager().sendRawMessage(player, "<red>Hub location is misconfigured! Contact an administrator.");
                return;
            }

            // Create location and teleport
            Location hubLocation = new Location(world, x, y, z, yaw, pitch);
            player.teleport(hubLocation);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to teleport player to hub: " + e.getMessage());
            plugin.getMessageManager().sendRawMessage(player, "<red>Failed to teleport to hub! Contact an administrator.");
        }
    }
}

