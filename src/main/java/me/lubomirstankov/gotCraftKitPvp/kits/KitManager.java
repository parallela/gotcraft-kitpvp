package me.lubomirstankov.gotCraftKitPvp.kits;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KitManager {

    private final GotCraftKitPvp plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private final Map<UUID, String> activeKits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> kitCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingKits = new ConcurrentHashMap<>();

    public KitManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        loadKits();
    }

    public void loadKits() {
        kits.clear();

        File kitsFolder = new File(plugin.getDataFolder(), "kits");
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs();
        }

        File[] kitFiles = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (kitFiles != null) {
            for (File kitFile : kitFiles) {
                String kitId = kitFile.getName().replace(".yml", "");
                FileConfiguration kitConfig = YamlConfiguration.loadConfiguration(kitFile);

                Kit kit = new Kit(kitId);
                kit.loadFromConfig(kitConfig);
                kits.put(kitId, kit);

                plugin.getLogger().info("Loaded kit: " + kitId);
            }
        }

        plugin.getLogger().info("Loaded " + kits.size() + " kits!");
    }

    public void reload() {
        loadKits();
    }

    public Kit getKit(String id) {
        return kits.get(id);
    }

    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    public void giveKit(Player player, Kit kit) {
        // Clear inventory and armor
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Remove active potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Apply 1.8 combat mechanics if enabled
        if (plugin.getConfig().getBoolean("combat.legacy-combat", true)) {
            try {
                var attackSpeed = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
                if (attackSpeed != null) {
                    attackSpeed.setBaseValue(16.0);
                }
            } catch (Exception e) {
                // Ignore if attribute doesn't exist
            }
        }

        // Give items first (main inventory slots 0-35)
        player.getInventory().setContents(kit.getInventoryContents());

        // Give armor AFTER items to ensure it's not overwritten
        player.getInventory().setArmorContents(kit.getArmorContents());

        // Apply potion effects
        for (String effectString : kit.getEffects()) {
            applyPotionEffect(player, effectString);
        }

        // Set active kit
        activeKits.put(player.getUniqueId(), kit.getId());

        // Update last kit in stats
        var stats = plugin.getStatsManager().getStats(player);
        if (stats != null) {
            stats.setLastKit(kit.getId());
        }

        // Set cooldown
        if (kit.getCooldown() > 0) {
            setKitCooldown(player, kit.getId(), kit.getCooldown());
        }

        // Send message
        Map<String, String> placeholders = Map.of("%kit%", kit.getName());
        plugin.getMessageManager().sendMessage(player, "kit-selected", placeholders);
    }

    public boolean canUseKit(Player player, Kit kit) {
        // Check permission
        if (!kit.getPermission().isEmpty() && !player.hasPermission(kit.getPermission())) {
            return false;
        }

        // Check cooldown
        if (hasKitCooldown(player, kit.getId())) {
            return false;
        }

        // Check if purchased (if not free)
        if (!kit.isFree() && kit.getPrice() > 0) {
            return plugin.getDatabaseManager().hasKitPurchased(player.getUniqueId(), kit.getId()).join();
        }

        return true;
    }

    public boolean purchaseKit(Player player, Kit kit) {
        if (kit.isFree() || kit.getPrice() == 0) {
            return true;
        }

        // Check if already purchased
        if (plugin.getDatabaseManager().hasKitPurchased(player.getUniqueId(), kit.getId()).join()) {
            return true;
        }

        // Check if player has enough money
        double balance = plugin.getEconomyManager().getBalance(player);
        if (balance < kit.getPrice()) {
            Map<String, String> placeholders = Map.of(
                "%price%", String.valueOf(kit.getPrice()),
                "%balance%", String.format("%.2f", balance)
            );
            plugin.getMessageManager().sendMessage(player, "kit-insufficient-funds", placeholders);
            return false;
        }

        // Withdraw money
        boolean success = plugin.getEconomyManager().withdraw(player, kit.getPrice());

        if (!success) {
            player.sendMessage("§cFailed to process payment!");
            return false;
        }

        // Save purchase
        plugin.getDatabaseManager().purchaseKit(player.getUniqueId(), kit.getId());

        // Send message
        Map<String, String> placeholders = Map.of(
            "%kit%", kit.getName(),
            "%price%", String.format("%.2f", kit.getPrice())
        );
        plugin.getMessageManager().sendMessage(player, "kit-purchased", placeholders);

        return true;
    }

    public String getActiveKit(Player player) {
        return activeKits.get(player.getUniqueId());
    }

    public void setKitCooldown(Player player, String kitId, int seconds) {
        Map<String, Long> cooldowns = kitCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        cooldowns.put(kitId, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean hasKitCooldown(Player player, String kitId) {
        Map<String, Long> cooldowns = kitCooldowns.get(player.getUniqueId());
        if (cooldowns == null) {
            return false;
        }

        Long cooldownEnd = cooldowns.get(kitId);
        if (cooldownEnd == null) {
            return false;
        }

        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getKitCooldownRemaining(Player player, String kitId) {
        Map<String, Long> cooldowns = kitCooldowns.get(player.getUniqueId());
        if (cooldowns == null) {
            return 0;
        }

        Long cooldownEnd = cooldowns.get(kitId);
        if (cooldownEnd == null) {
            return 0;
        }

        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void clearActiveKit(UUID uuid) {
        activeKits.remove(uuid);
    }

    private void applyPotionEffect(Player player, String effectString) {
        try {
            String[] parts = effectString.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0; // -1 because level 1 = amplifier 0

            if (type != null) {
                // Permanent effect (very long duration)
                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid potion effect format: " + effectString);
        }
    }

    public void saveKit(Kit kit) {
        try {
            File kitFile = new File(plugin.getDataFolder(), "kits/" + kit.getId() + ".yml");
            if (!kitFile.getParentFile().exists()) {
                kitFile.getParentFile().mkdirs();
            }

            FileConfiguration config = new YamlConfiguration();
            kit.saveToConfig(config);
            config.save(kitFile);

            // Update in memory
            kits.put(kit.getId(), kit);

            plugin.getLogger().info("Saved kit: " + kit.getId());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save kit " + kit.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteKit(String kitId) {
        kits.remove(kitId);
        File kitFile = new File(plugin.getDataFolder(), "kits/" + kitId + ".yml");
        if (kitFile.exists()) {
            kitFile.delete();
        }
    }

    // Kit Editor Methods

    public void setEditingKit(UUID playerUUID, String kitId) {
        editingKits.put(playerUUID, kitId);
    }

    public String getEditingKit(UUID playerUUID) {
        return editingKits.get(playerUUID);
    }

    public void clearEditingKit(UUID playerUUID) {
        editingKits.remove(playerUUID);
    }

    public void saveKitFromInventory(Player player, Kit kit) {
        // Save armor
        kit.setArmorContents(player.getInventory().getArmorContents());

        // Save inventory contents
        kit.setInventoryContents(player.getInventory().getContents());

        // Save the kit to disk
        saveKit(kit);
    }
}

