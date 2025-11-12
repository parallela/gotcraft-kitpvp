package me.lubomirstankov.gotCraftKitPvp;

import me.lubomirstankov.gotCraftKitPvp.abilities.AbilityManager;
import me.lubomirstankov.gotCraftKitPvp.commands.*;
import me.lubomirstankov.gotCraftKitPvp.config.ConfigManager;
import me.lubomirstankov.gotCraftKitPvp.config.MessageManager;
import me.lubomirstankov.gotCraftKitPvp.database.DatabaseManager;
import me.lubomirstankov.gotCraftKitPvp.gui.GUIManager;
import me.lubomirstankov.gotCraftKitPvp.hooks.PlaceholderAPIHook;
import me.lubomirstankov.gotCraftKitPvp.hooks.VaultHook;
import me.lubomirstankov.gotCraftKitPvp.kits.KitManager;
import me.lubomirstankov.gotCraftKitPvp.leaderboard.LeaderboardManager;
import me.lubomirstankov.gotCraftKitPvp.listeners.*;
import me.lubomirstankov.gotCraftKitPvp.scoreboard.ScoreboardManager;
import me.lubomirstankov.gotCraftKitPvp.stats.StatsManager;
import me.lubomirstankov.gotCraftKitPvp.zones.ZoneManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class GotCraftKitPvp extends JavaPlugin {

    private static GotCraftKitPvp instance;

    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private KitManager kitManager;
    private ZoneManager zoneManager;
    private AbilityManager abilityManager;
    private GUIManager guiManager;
    private ScoreboardManager scoreboardManager;
    private LeaderboardManager leaderboardManager;
    private me.lubomirstankov.gotCraftKitPvp.economy.EconomyManager economyManager;

    // Hooks
    private VaultHook vaultHook;
    private PlaceholderAPIHook placeholderAPIHook;

    // Listeners
    private HealthTagListener healthTagListener;

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("Enabling GotCraftKitPvp...");

        // Register BungeeCord messaging channel for hub command
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Initialize managers
        try {
            initializeManagers();
            registerCommands();
            registerListeners();
            setupHooks();

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("GotCraftKitPvp enabled successfully in " + loadTime + "ms!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable GotCraftKitPvp!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling GotCraftKitPvp...");

        // Save all data
        if (statsManager != null) {
            statsManager.saveAllStats();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        // Clear scoreboards
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }

        getLogger().info("GotCraftKitPvp disabled!");
    }

    private void initializeManagers() {
        getLogger().info("Initializing managers...");

        // Config must be first
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        // Save default kit files if they don't exist
        saveDefaultKits();

        // Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Core managers
        statsManager = new StatsManager(this);
        economyManager = new me.lubomirstankov.gotCraftKitPvp.economy.EconomyManager(this);
        kitManager = new KitManager(this);
        zoneManager = new ZoneManager(this);
        abilityManager = new AbilityManager(this);
        guiManager = new GUIManager(this);
        scoreboardManager = new ScoreboardManager(this);
        leaderboardManager = new LeaderboardManager(this);

        getLogger().info("All managers initialized!");
    }

    private void saveDefaultKits() {
        File kitsFolder = new File(getDataFolder(), "kits");
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs();
        }

        // Save default kit files from resources if they don't exist
        String[] defaultKits = {"default.yml", "warrior.yml", "archer.yml", "tank.yml"};
        for (String kitFile : defaultKits) {
            File file = new File(kitsFolder, kitFile);
            if (!file.exists()) {
                try {
                    saveResource("kits/" + kitFile, false);
                    getLogger().info("Created default kit file: " + kitFile);
                } catch (Exception e) {
                    getLogger().warning("Could not save default kit " + kitFile + ": " + e.getMessage());
                }
            }
        }
    }

    private void registerCommands() {
        getLogger().info("Registering commands...");

        KitPvpCommand kitPvpCommand = new KitPvpCommand(this);
        getCommand("kitpvp").setExecutor(kitPvpCommand);
        getCommand("kitpvp").setTabCompleter(kitPvpCommand);

        getCommand("kits").setExecutor(new KitsCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));

        GiveMoneyCommand giveMoneyCommand = new GiveMoneyCommand(this);
        getCommand("givemoney").setExecutor(giveMoneyCommand);
        getCommand("givemoney").setTabCompleter(giveMoneyCommand);

        // Register hub/leave command
        getCommand("hub").setExecutor(new HubCommand(this));

        getLogger().info("Commands registered!");
    }

    private void registerListeners() {
        getLogger().info("Registering listeners...");

        getServer().getPluginManager().registerEvents(new MessageSuppressionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new ZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);

        healthTagListener = new HealthTagListener(this);
        getServer().getPluginManager().registerEvents(healthTagListener, this);

        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new ZoneSelectionListener(this), this);

        getLogger().info("Listeners registered!");
    }

    private void setupHooks() {
        getLogger().info("Setting up hooks...");

        // Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(this);
            if (vaultHook.setup()) {
                getLogger().info("Hooked into Vault!");
            }
        }

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            placeholderAPIHook.register();
            getLogger().info("Hooked into PlaceholderAPI!");
        }
    }

    public void reload() {
        getLogger().info("Reloading configuration...");

        configManager.reload();
        messageManager.reload();
        kitManager.reload();
        zoneManager.reload();
        abilityManager.reload();
        leaderboardManager.reload();

        getLogger().info("Configuration reloaded!");
    }

    // Getters
    public static GotCraftKitPvp getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public me.lubomirstankov.gotCraftKitPvp.economy.EconomyManager getEconomyManager() {
        return economyManager;
    }


    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public HealthTagListener getHealthTagListener() {
        return healthTagListener;
    }
}
