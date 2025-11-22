package me.lubomirstankov.gotCraftKitPvp.gui;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.kits.Kit;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GUIManager {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, String> openGUIs = new HashMap<>();
    private final Map<UUID, Integer> guiPages = new HashMap<>();

    public GUIManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    public void openKitSelector(Player player) {
        openKitSelector(player, 0);
    }

    public void openKitSelector(Player player, int page) {
        String title = plugin.getMessageManager().parseLegacy(plugin.getConfig().getString("gui.kit-selector.title", "<gold><bold>Select Your Kit</bold></gold>"));
        int size = plugin.getConfig().getInt("gui.kit-selector.size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill with glass panes if enabled
        if (plugin.getConfig().getBoolean("gui.kit-selector.fill-empty", true)) {
            Material fillMaterial = Material.valueOf(plugin.getConfig().getString("gui.kit-selector.fill-material", "GRAY_STAINED_GLASS_PANE"));
            String fillName = plugin.getConfig().getString("gui.kit-selector.fill-name", " ");
            ItemStack filler = createItem(fillMaterial, fillName, new ArrayList<>());

            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        // Get all kits
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());

        int kitsPerPage = 45; // 5 rows of 9
        int totalPages = (int) Math.ceil((double) kits.size() / kitsPerPage);
        int startIndex = page * kitsPerPage;
        int endIndex = Math.min(startIndex + kitsPerPage, kits.size());

        // Add kits
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Kit kit = kits.get(i);
            ItemStack kitIcon = kit.getIcon().toItemStack();

            // Update lore with status
            ItemMeta meta = kitIcon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) {
                    lore = new ArrayList<>();
                }

                lore.add("");

                // Check if player can use this kit
                if (!kit.getPermission().isEmpty() && !player.hasPermission(kit.getPermission())) {
                    lore.add(plugin.getMessageManager().parseLegacy("<red><bold>LOCKED</bold></red>"));
                } else if (!kit.isFree() && kit.getPrice() > 0) {
                    // Note: We can't check purchase status here as it would block the main thread
                    // The click handler will check if purchased
                    lore.add(plugin.getMessageManager().parseLegacy("<gray>Price: <yellow>$" + kit.getPrice()));
                    lore.add(plugin.getMessageManager().parseLegacy("<yellow>Click to purchase/select"));
                } else {
                    lore.add(plugin.getMessageManager().parseLegacy("<green><bold>FREE</bold></green>"));
                    lore.add(plugin.getMessageManager().parseLegacy("<yellow>Click to select"));
                }

                meta.setLore(lore);
                kitIcon.setItemMeta(meta);
            }

            inv.setItem(slot++, kitIcon);
        }

        // Add navigation buttons
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, plugin.getMessageManager().getMessage("gui-previous-page"), new ArrayList<>()));
        }

        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, plugin.getMessageManager().getMessage("gui-next-page"), new ArrayList<>()));
        }

        // Close button
        inv.setItem(49, createItem(Material.BARRIER, plugin.getMessageManager().getMessage("gui-close"), new ArrayList<>()));

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), "kit-selector");
        guiPages.put(player.getUniqueId(), page);
        plugin.getLogger().info("Opened kit-selector GUI for " + player.getName());
    }

    public void openStatsGUI(Player player, Player target) {
        String title = plugin.getConfig().getString("gui.stats-gui.title", "<yellow><bold>Your Statistics</bold></yellow>");
        // Replace %player% BEFORE parsing MiniMessage
        title = title.replace("%player%", target.getName());
        title = plugin.getMessageManager().parseLegacy(title);

        int size = plugin.getConfig().getInt("gui.stats-gui.size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);

        PlayerStats stats = plugin.getStatsManager().getStats(target);
        if (stats == null) {
            // Stats not loaded yet - send message and try to load
            player.sendMessage(plugin.getMessageManager().parseLegacy("<red>Stats not loaded yet! Please try again in a moment."));
            plugin.getLogger().warning("Stats for " + target.getName() + " not loaded when opening stats GUI");
            return;
        }

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            headMeta.setDisplayName(ChatColor.YELLOW + target.getName());
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // Kills
        inv.setItem(10, createItem(Material.DIAMOND_SWORD,
                ChatColor.GREEN + "Kills",
                Arrays.asList(ChatColor.GRAY + "Total: " + ChatColor.GREEN + stats.getKills())));

        // Deaths
        inv.setItem(12, createItem(Material.SKELETON_SKULL,
                ChatColor.RED + "Deaths",
                Arrays.asList(ChatColor.GRAY + "Total: " + ChatColor.RED + stats.getDeaths())));

        // K/D Ratio
        inv.setItem(14, createItem(Material.PAPER,
                ChatColor.YELLOW + "K/D Ratio",
                Arrays.asList(ChatColor.GRAY + "Ratio: " + ChatColor.YELLOW + stats.getFormattedKDR())));

        // Streak
        inv.setItem(16, createItem(Material.FIRE_CHARGE,
                ChatColor.GOLD + "Killstreak",
                Arrays.asList(
                        ChatColor.GRAY + "Current: " + ChatColor.GOLD + stats.getCurrentStreak(),
                        ChatColor.GRAY + "Best: " + ChatColor.GOLD + stats.getBestStreak()
                )));

        // Level
        int requiredXP = plugin.getStatsManager().getRequiredXP(stats.getLevel());
        inv.setItem(21, createItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.AQUA + "Level",
                Arrays.asList(
                        ChatColor.GRAY + "Level: " + ChatColor.AQUA + stats.getLevel(),
                        ChatColor.GRAY + "XP: " + ChatColor.AQUA + stats.getXp() + "/" + requiredXP
                )));

        // Last Kit
        if (stats.getLastKit() != null) {
            Kit lastKit = plugin.getKitManager().getKit(stats.getLastKit());
            if (lastKit != null) {
                inv.setItem(23, createItem(Material.CHEST,
                        ChatColor.YELLOW + "Last Kit",
                        Arrays.asList(ChatColor.GRAY + "Kit: " + lastKit.getName())));
            }
        }

        // Money (built-in economy)
        double balance = plugin.getEconomyManager().getBalance(target);
        inv.setItem(22, createItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Money",
                Arrays.asList(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", balance))));

        // Close button
        inv.setItem(26, createItem(Material.BARRIER, plugin.getMessageManager().getMessage("gui-close"), new ArrayList<>()));

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), "stats");
        plugin.getLogger().info("Opened stats GUI for " + player.getName());
    }

    public void openLeaderboardGUI(Player player) {
        String title = plugin.getMessageManager().parseLegacy(plugin.getConfig().getString("gui.leaderboard-gui.title", "<gold><bold>Top Players</bold></gold>"));
        int size = plugin.getConfig().getInt("gui.leaderboard-gui.size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Categories
        inv.setItem(10, createItem(Material.DIAMOND_SWORD,
                ChatColor.GREEN + "Top Kills",
                Arrays.asList(ChatColor.GRAY + "Click to view")));

        inv.setItem(13, createItem(Material.FIRE_CHARGE,
                ChatColor.GOLD + "Top Streaks",
                Arrays.asList(ChatColor.GRAY + "Click to view")));

        inv.setItem(16, createItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.AQUA + "Top Levels",
                Arrays.asList(ChatColor.GRAY + "Click to view")));

        // Close button
        inv.setItem(49, createItem(Material.BARRIER, plugin.getMessageManager().getMessage("gui-close"), new ArrayList<>()));

        player.openInventory(inv);
        plugin.getLogger().info("Opened leaderboard-main GUI for " + player.getName());
        openGUIs.put(player.getUniqueId(), "leaderboard-main");
    }

    public void openTopKillsGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', "&6&lTop Players - Kills");

        // Load async to avoid blocking
        plugin.getDatabaseManager().getTopKills(45).thenAccept(topPlayers -> {
            // Run on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 54, title);

                for (int i = 0; i < Math.min(topPlayers.size(), 45); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    ItemStack item = createItem(Material.PLAYER_HEAD,
                            ChatColor.YELLOW + "#" + (i + 1) + " " + stats.getName(),
                            Arrays.asList(
                                    ChatColor.GRAY + "Kills: " + ChatColor.GREEN + stats.getKills(),
                                    ChatColor.GRAY + "Deaths: " + ChatColor.RED + stats.getDeaths(),
                                    ChatColor.GRAY + "K/D: " + ChatColor.YELLOW + stats.getFormattedKDR()
                            ));

                    inv.setItem(i, item);
                }

                // Back button
                inv.setItem(49, createItem(Material.ARROW, plugin.getMessageManager().getMessage("gui-back"), new ArrayList<>()));

                player.openInventory(inv);
                plugin.getLogger().info("Opened leaderboard-kills GUI for " + player.getName());
                openGUIs.put(player.getUniqueId(), "leaderboard-kills");
            });
        });
    }

    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public String getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    public int getGUIPage(Player player) {
        return guiPages.getOrDefault(player.getUniqueId(), 0);
    }

    public void closeGUI(Player player) {
        String guiType = openGUIs.get(player.getUniqueId());
        if (guiType != null) {
            plugin.getLogger().info("Closed " + guiType + " GUI for " + player.getName());
        }
        openGUIs.remove(player.getUniqueId());
        guiPages.remove(player.getUniqueId());
    }
}

