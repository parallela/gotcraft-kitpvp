package me.lubomirstankov.gotCraftKitPvp;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.lubomirstankov.gotCraftKitPvp.abilities.AbilityManager;
import me.lubomirstankov.gotCraftKitPvp.commands.*;
import me.lubomirstankov.gotCraftKitPvp.config.ConfigManager;
import me.lubomirstankov.gotCraftKitPvp.config.MessageManager;
import me.lubomirstankov.gotCraftKitPvp.database.AutoSaveTask;
import me.lubomirstankov.gotCraftKitPvp.database.DatabaseManager;
import me.lubomirstankov.gotCraftKitPvp.gui.GUIManager;
import me.lubomirstankov.gotCraftKitPvp.hooks.PlaceholderAPIHook;
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
    private PlaceholderAPIHook placeholderAPIHook;

    // Listeners
    private HealthTagListener healthTagListener;
    private HealthRegenerationListener healthRegenerationListener;

    // AutoSave Task
    private AutoSaveTask autoSaveTask;

    @Override
    public void onLoad() {
        // Load PacketEvents - MUST be in onLoad()
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .reEncodeByDefault(false)
            .checkForUpdates(false)
            .bStats(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("Enabling GotCraftKitPvp...");

        // Initialize PacketEvents
        PacketEvents.getAPI().init();

        // Register BungeeCord messaging channel for hub command
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Initialize managers
        try {
            initializeManagers();
            registerCommands();
            registerListeners();
            setupHooks();
            startAutoSave();

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("GotCraftKitPvp enabled successfully in " + loadTime + "ms!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable GotCraftKitPvp!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("==============================================");
        getLogger().info("Initiating safe shutdown sequence...");
        getLogger().info("==============================================");

        // Stop autosave task first
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            getLogger().info("AutoSave task stopped");
        }

        // Terminate PacketEvents
        PacketEvents.getAPI().terminate();

        // CRITICAL: Save all data SYNCHRONOUSLY
        getLogger().info("Saving all player data...");

        if (statsManager != null) {
            statsManager.saveAllStats();
            getLogger().info("All stats saved");
        }

        if (economyManager != null) {
            economyManager.saveAll();
            getLogger().info("All economy data saved");
        }

        // Flush database pending writes
        if (databaseManager != null) {
            getLogger().info("Flushing database writes...");
            databaseManager.flushPendingWrites();

            // Wait for async saves to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            databaseManager.close();
        }

        // Clear scoreboards
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }

        // Stop health regeneration task
        if (healthRegenerationListener != null) {
            healthRegenerationListener.shutdown();
        }

        getLogger().info("==============================================");
        getLogger().info("GotCraftKitPvp disabled safely!");
        getLogger().info("All data has been saved to database");
        getLogger().info("==============================================");
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
//        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);

//        healthTagListener = new HealthTagListener(this);
//        getServer().getPluginManager().registerEvents(healthTagListener, this);

        healthRegenerationListener = new HealthRegenerationListener(this);
        getServer().getPluginManager().registerEvents(healthRegenerationListener, this);

        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new ZoneSelectionListener(this), this);

        getLogger().info("Listeners registered!");
    }

    private void setupHooks() {
        getLogger().info("Setting up hooks...");

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            placeholderAPIHook.register();
            getLogger().info("Hooked into PlaceholderAPI!");
        }
    }

    private void startAutoSave() {
        int intervalMinutes = getConfig().getInt("database.autosave-interval", 5);
        if (intervalMinutes > 0) {
            long intervalTicks = intervalMinutes * 60 * 20L; // Convert minutes to ticks

            autoSaveTask = new AutoSaveTask(this);
            autoSaveTask.runTaskTimerAsynchronously(this, intervalTicks, intervalTicks);

            getLogger().info("==============================================");
            getLogger().info("AutoSave enabled - interval: " + intervalMinutes + " minutes");
            getLogger().info("This prevents data loss from server crashes");
            getLogger().info("==============================================");
        } else {
            getLogger().warning("AutoSave is disabled - this is NOT recommended!");
        }
    }

    public void reload() {
        getLogger().info("==============================================");
        getLogger().info("RELOAD INITIATED - SAVING ALL DATA FIRST!");
        getLogger().info("==============================================");

        // CRITICAL FIX: Save all data BEFORE reloading configs
        // Run async to avoid blocking main thread and triggering watchdog
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            if (statsManager != null) {
                getLogger().info("Saving all stats...");
                statsManager.saveAllStats();
            }

            if (economyManager != null) {
                getLogger().info("Saving all economy data...");
                economyManager.saveAll();
            }

            if (databaseManager != null) {
                getLogger().info("Flushing database writes...");
                databaseManager.flushPendingWrites();
            }
        }).thenRun(() -> {
            // Wait a bit for saves to settle
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Now reload configs on main thread
            getServer().getScheduler().runTask(this, () -> {
                getLogger().info("All data saved successfully - proceeding with config reload");

                configManager.reload();
                messageManager.reload();
                kitManager.reload();
                zoneManager.reload();
                abilityManager.reload();
                leaderboardManager.reload();

                getLogger().info("==============================================");
                getLogger().info("Configuration reloaded successfully!");
                getLogger().info("Player data preserved - no data loss");
                getLogger().info("==============================================");
            });
        }).exceptionally(ex -> {
            getLogger().log(Level.SEVERE, "Error during reload save operation", ex);
            return null;
        });
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

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public HealthTagListener getHealthTagListener() {
        return healthTagListener;
    }
}
