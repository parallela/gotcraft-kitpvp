package me.lubomirstankov.gotCraftKitPvp.config;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {

    private final GotCraftKitPvp plugin;
    private FileConfiguration config;
    private FileConfiguration zonesConfig;
    private FileConfiguration abilitiesConfig;

    public ConfigManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        // Save default configs
        plugin.saveDefaultConfig();
        saveResource("messages.yml");
        saveResource("zones.yml");
        saveResource("abilities.yml");
        saveResource("kits/warrior.yml");
        saveResource("kits/archer.yml");
        saveResource("kits/tank.yml");

        // Load configs
        config = plugin.getConfig();
        zonesConfig = loadConfig("zones.yml");
        abilitiesConfig = loadConfig("abilities.yml");
    }

    private void saveResource(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(resourcePath, false);
        }
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        zonesConfig = loadConfig("zones.yml");
        abilitiesConfig = loadConfig("abilities.yml");
    }

    public void saveZonesConfig() {
        try {
            File file = new File(plugin.getDataFolder(), "zones.yml");
            zonesConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save zones.yml", e);
        }
    }

    // Getters
    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getZonesConfig() {
        return zonesConfig;
    }

    public FileConfiguration getAbilitiesConfig() {
        return abilitiesConfig;
    }

    // Helper methods for common config values
    public boolean isBungeeMode() {
        return config.getBoolean("general.bungee-mode", false);
    }

    public boolean isLegacyCombat() {
        return config.getBoolean("combat.legacy-combat", true);
    }

    public int getHitDelay() {
        return config.getInt("combat.hit-delay", 10);
    }

    public boolean isDamageIndicators() {
        return config.getBoolean("combat.damage-indicators", true);
    }

    public boolean isHitSounds() {
        return config.getBoolean("combat.hit-sounds", true);
    }

    public int getAntiCleanupDuration() {
        return config.getInt("combat.anti-cleanup-duration", 5);
    }

    public boolean isKillStreaksEnabled() {
        return config.getBoolean("kill-streaks.enabled", true);
    }

    public boolean isLevelingEnabled() {
        return config.getBoolean("leveling.enabled", true);
    }

    public int getXpPerKill() {
        return config.getInt("leveling.xp-per-kill", 50);
    }

    public int getXpPerDeath() {
        return config.getInt("leveling.xp-per-death", 10);
    }

    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", true);
    }

    public double getMoneyPerKill() {
        return config.getDouble("economy.money-per-kill", 10);
    }

    public double getMoneyPerDeath() {
        return config.getDouble("economy.money-per-death", 5);
    }

    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", true);
    }

    public int getScoreboardUpdateInterval() {
        return config.getInt("scoreboard.update-interval", 20);
    }

    public boolean isLeaderboardEnabled() {
        return config.getBoolean("leaderboards.enabled", true);
    }

    public int getLeaderboardRefreshInterval() {
        return config.getInt("leaderboards.refresh-interval", 60);
    }

    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE");
    }

    public boolean isAbilitiesEnabled() {
        return config.getBoolean("abilities.enabled", true);
    }
}

