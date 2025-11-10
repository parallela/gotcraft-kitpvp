package me.lubomirstankov.gotCraftKitPvp.commands;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.kits.Kit;
import me.lubomirstankov.gotCraftKitPvp.zones.Zone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class KitPvpCommand implements CommandExecutor, TabCompleter {

    private final GotCraftKitPvp plugin;

    public KitPvpCommand(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                return true;
            }

            Player player = (Player) sender;
            plugin.getGuiManager().openKitSelector(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;

            case "reload":
                if (!sender.hasPermission("kitpvp.command.reload")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getMessageManager().getReloadSuccess());
                return true;

            case "setspawn":
                if (!sender.hasPermission("kitpvp.command.setspawn")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                    return true;
                }
                Player player = (Player) sender;
                plugin.getZoneManager().setArenaSpawn(player.getLocation());
                plugin.getMessageManager().sendMessage(player, "spawn-set");
                return true;

            case "setzone":
                if (!sender.hasPermission("kitpvp.command.setzone")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("invalid-command"));
                    return true;
                }
                handleSetZone((Player) sender, args);
                return true;

            case "createkit":
                if (!sender.hasPermission("kitpvp.command.createkit")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessageComponent("invalid-command"));
                    return true;
                }
                plugin.getMessageManager().sendRawMessage((Player) sender, "<green>Kit creator GUI coming soon!");
                return true;

            case "editkit":
                if (!sender.hasPermission("kitpvp.command.editkit")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessageComponent("invalid-command"));
                    return true;
                }
                handleEditKit((Player) sender, args[1]);
                return true;

            case "savekit":
                if (!sender.hasPermission("kitpvp.command.editkit")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessageComponent("invalid-command"));
                    return true;
                }
                handleSaveKit((Player) sender, args[1]);
                return true;

            case "deletekit":
                if (!sender.hasPermission("kitpvp.command.deletekit")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageManager().getMessageComponent("invalid-command"));
                    return true;
                }
                String kitId = args[1];
                plugin.getKitManager().deleteKit(kitId);
                plugin.getMessageManager().sendRawMessage((Player) sender, "<green>Kit deleted!");
                return true;

            case "gui":
                if (!sender.hasPermission("kitpvp.command.gui")) {
                    sender.sendMessage(plugin.getMessageManager().getNoPermission());
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                    return true;
                }
                plugin.getMessageManager().sendRawMessage((Player) sender, "<green>Admin GUI coming soon!");
                return true;

            case "scoreboard":
            case "sb":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getPlayerOnly());
                    return true;
                }
                handleScoreboardToggle((Player) sender);
                return true;

            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("invalid-command"));
                return true;
        }
    }

    private void handleSetZone(Player player, String[] args) {
        String zoneType = args[1].toUpperCase();

        if (!zoneType.equals("SAFE") && !zoneType.equals("PVP")) {
            plugin.getMessageManager().sendRawMessage(player, "<red>Invalid zone type! Use SAFE or PVP");
            return;
        }

        // Check if this is creating the zone or giving the wand
        if (args.length == 2) {
            // Give zone selection wand
            giveZoneWand(player, zoneType);
            return;
        }

        // If they have specified a zone name, create it
        if (args.length >= 3) {
            String zoneName = args[2];

            // Check if player has made a selection
            if (!plugin.getZoneManager().getZoneSelection().hasSelection(player.getUniqueId())) {
                plugin.getMessageManager().sendRawMessage(player, "<red>You must select two positions first! Use the zone wand.");
                return;
            }

            // Get the selection
            var pos1 = plugin.getZoneManager().getZoneSelection().getPos1(player.getUniqueId());
            var pos2 = plugin.getZoneManager().getZoneSelection().getPos2(player.getUniqueId());

            // Create the zone
            Zone.ZoneType type = Zone.ZoneType.valueOf(zoneType);
            Zone zone = new Zone(zoneName, type, player.getWorld(), pos1, pos2);
            plugin.getZoneManager().saveZone(zone);

            // Clear selection
            plugin.getZoneManager().getZoneSelection().clearSelection(player.getUniqueId());

            // Send success message
            Map<String, String> placeholders = Map.of("%zone%", zoneName);
            plugin.getMessageManager().sendMessage(player, "zone-created", placeholders);
        }
    }

    private void giveZoneWand(Player player, String zoneType) {
        org.bukkit.inventory.ItemStack wand = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK);
        org.bukkit.inventory.meta.ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getMessageManager().parseLegacy("<gradient:#00ffff:#00ff00><bold>Zone Selection Wand</bold></gradient>"));
            meta.setLore(java.util.Arrays.asList(
                    plugin.getMessageManager().parseLegacy("<gray>Left-click: Set position 1"),
                    plugin.getMessageManager().parseLegacy("<gray>Right-click: Set position 2"),
                    plugin.getMessageManager().parseLegacy("<aqua>Creating: <yellow>" + zoneType + " Zone"),
                    plugin.getMessageManager().parseLegacy(""),
                    plugin.getMessageManager().parseLegacy("<gray>After selecting both positions,"),
                    plugin.getMessageManager().parseLegacy("<gray>use <aqua>/kitpvp setzone " + zoneType.toLowerCase() + " <name>")
            ));
            wand.setItemMeta(meta);
        }

        player.getInventory().addItem(wand);
        plugin.getMessageManager().sendMessage(player, "zone-wand-given");
        plugin.getMessageManager().sendMessage(player, "zone-selection-start");
    }

    private void handleEditKit(Player player, String kitId) {
        Kit kit = plugin.getKitManager().getKit(kitId);
        if (kit == null) {
            plugin.getMessageManager().sendMessage(player, "kit-not-found");
            return;
        }

        // Clear player inventory
        player.getInventory().clear();

        // Give the player the kit's current items
        plugin.getKitManager().giveKit(player, kit);

        // Store that they're editing this kit
        plugin.getKitManager().setEditingKit(player.getUniqueId(), kitId);

        // Send message
        Map<String, String> placeholders = Map.of("%kit%", kitId);
        plugin.getMessageManager().sendMessage(player, "kit-editor-enter", placeholders);
    }

    private void handleSaveKit(Player player, String kitId) {
        // Check if player is editing this kit
        String editingKit = plugin.getKitManager().getEditingKit(player.getUniqueId());
        if (editingKit == null || !editingKit.equals(kitId)) {
            plugin.getMessageManager().sendRawMessage(player, "<red>You are not editing this kit! Use <yellow>/kitpvp editkit " + kitId + "</yellow> first.");
            return;
        }

        Kit kit = plugin.getKitManager().getKit(kitId);
        if (kit == null) {
            plugin.getMessageManager().sendMessage(player, "kit-not-found");
            return;
        }

        // Save the kit from player's inventory
        plugin.getKitManager().saveKitFromInventory(player, kit);

        // Clear editing state
        plugin.getKitManager().clearEditingKit(player.getUniqueId());

        // Send success message
        Map<String, String> placeholders = Map.of("%kit%", kitId);
        plugin.getMessageManager().sendMessage(player, "kit-saved", placeholders);

        // Clear inventory
        player.getInventory().clear();
    }

    private void handleScoreboardToggle(Player player) {
        if (plugin.getScoreboardManager() == null) {
            plugin.getMessageManager().sendRawMessage(player, "<red>Scoreboard is disabled!");
            return;
        }

        boolean visible = plugin.getScoreboardManager().toggleScoreboard(player);

        if (visible) {
            plugin.getMessageManager().sendMessage(player, "scoreboard-shown");
        } else {
            plugin.getMessageManager().sendMessage(player, "scoreboard-hidden");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessageComponent("help-header"));

        // Player commands
        for (String line : plugin.getMessageManager().getMessageList("help-commands")) {
            sender.sendMessage(line);
        }

        // Admin commands if they have permission
        if (sender.hasPermission("kitpvp.admin")) {
            sender.sendMessage(""); // Empty line
            for (String line : plugin.getMessageManager().getMessageList("help-admin-commands")) {
                sender.sendMessage(line);
            }
        }

        sender.sendMessage(plugin.getMessageManager().getMessageComponent("help-footer"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "reload", "setspawn", "setzone", "createkit", "editkit", "savekit", "deletekit", "gui", "scoreboard", "sb");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setzone")) {
                return Arrays.asList("safe", "pvp");
            }

            if (args[0].equalsIgnoreCase("editkit") || args[0].equalsIgnoreCase("savekit") || args[0].equalsIgnoreCase("deletekit")) {
                return plugin.getKitManager().getAllKits().stream()
                        .map(Kit::getId)
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}

