package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoinListener implements Listener {

    private final GotCraftKitPvp plugin;

    public PlayerJoinListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player stats
        plugin.getStatsManager().loadPlayerStats(player);

        // Load player balance
        plugin.getEconomyManager().loadBalance(player.getUniqueId());

        // Create scoreboard
        plugin.getScoreboardManager().createScoreboard(player);

        // Apply 1.8 combat mechanics (no cooldown)
        if (plugin.getConfig().getBoolean("combat.legacy-combat", true)) {
            // Set attack speed to 16.0 to remove cooldown (1.8 PvP standard)
            try {
                var attackSpeed = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
                if (attackSpeed != null) {
                    attackSpeed.setBaseValue(16.0);
                }
            } catch (Exception e) {
                // Ignore if attribute doesn't exist
            }
        }

        // Always teleport to spawn on join (delayed to allow world to load)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getZoneManager().teleportToSpawn(player);

            // Give spawn items if bungee mode
            if (plugin.getConfigManager().isBungeeMode()) {
                giveSpawnItems(player);
            }

            // Give default kit if needed
            giveDefaultKitIfNeeded(player);
        }, 5L);
    }

    private void giveDefaultKitIfNeeded(Player player) {
        // Get default kit
        var defaultKit = plugin.getKitManager().getKit("default");
        if (defaultKit == null) {
            plugin.getLogger().warning("Default kit not found! Create a 'default.yml' file in the kits folder.");
            return;
        }

        // Check if player already has a kit active
        String activeKit = plugin.getKitManager().getActiveKit(player);
        if (activeKit != null) {
            return; // Player already has a kit
        }

        // Give default kit automatically to new players
        plugin.getKitManager().giveKit(player, defaultKit);
    }

    private void giveSpawnItems(Player player) {
        if (!plugin.getConfig().getBoolean("arena.clear-inventory", true)) {
            return;
        }

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        // Kit selector
        if (plugin.getConfig().getBoolean("arena.spawn-items.kit-selector.enabled", true)) {
            int slot = plugin.getConfig().getInt("arena.spawn-items.kit-selector.slot", 0);
            Material material = Material.valueOf(plugin.getConfig().getString("arena.spawn-items.kit-selector.material", "CHEST"));
            String name = plugin.getConfig().getString("arena.spawn-items.kit-selector.name", "<gold>Select Kit</gold>");

            ItemStack item = new ItemStack(material);
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getMessageManager().parseLegacy(name));
                var lore = plugin.getConfig().getStringList("arena.spawn-items.kit-selector.lore");
                if (!lore.isEmpty()) {
                    meta.setLore(lore.stream().map(plugin.getMessageManager()::parseLegacy).toList());
                }
                item.setItemMeta(meta);
            }

            player.getInventory().setItem(slot, item);
        }

        // Stats item
        if (plugin.getConfig().getBoolean("arena.spawn-items.stats.enabled", true)) {
            int slot = plugin.getConfig().getInt("arena.spawn-items.stats.slot", 8);
            Material material = Material.valueOf(plugin.getConfig().getString("arena.spawn-items.stats.material", "PLAYER_HEAD"));
            String name = plugin.getConfig().getString("arena.spawn-items.stats.name", "<yellow>Your Stats</yellow>");

            ItemStack item = new ItemStack(material);
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getMessageManager().parseLegacy(name));
                var lore = plugin.getConfig().getStringList("arena.spawn-items.stats.lore");
                if (!lore.isEmpty()) {
                    meta.setLore(lore.stream().map(plugin.getMessageManager()::parseLegacy).toList());
                }
                item.setItemMeta(meta);
            }

            player.getInventory().setItem(slot, item);
        }
    }
}

