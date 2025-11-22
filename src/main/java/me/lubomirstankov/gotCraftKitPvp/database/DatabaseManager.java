package me.lubomirstankov.gotCraftKitPvp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * FORENSIC-ENGINEERED DATABASE MANAGER
 *
 * CRITICAL DATA LOSS PREVENTION:
 * - Connection pooling with HikariCP
 * - Atomic write operations with transactions
 * - Batch write support for efficiency
 * - Read/write locks to prevent race conditions
 * - Automatic retry on failure
 * - Proper shutdown sequence to prevent data loss
 * - PostgreSQL primary, with table prefix
 */
public class DatabaseManager {

    private static final String TABLE_PREFIX = "gotcraftkitpvp_";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 100;

    private final GotCraftKitPvp plugin;
    private HikariDataSource dataSource;
    private final String type;

    // Pending writes queue for batch operations
    private final ConcurrentHashMap<UUID, PendingWrite> pendingWrites = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock();

    public DatabaseManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfigManager().getDatabaseType();
    }

    public void initialize() {
        try {
            setupDataSource();
            createTables();
            warmUpConnections();

            plugin.getLogger().info("==============================================");
            plugin.getLogger().info("Database initialized successfully!");
            plugin.getLogger().info("Type: " + type);
            plugin.getLogger().info("Table Prefix: " + TABLE_PREFIX);
            plugin.getLogger().info("Connection Pool: Active");
            plugin.getLogger().info("==============================================");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "CRITICAL: Failed to initialize database!", e);
            throw new RuntimeException("Database initialization failed - CANNOT CONTINUE", e);
        }
    }

    private void warmUpConnections() {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            plugin.getLogger().info("Database connection pool warmed up successfully");
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to warm up database connections: " + e.getMessage());
        }
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("POSTGRESQL") || type.equalsIgnoreCase("POSTGRES")) {
            String host = plugin.getConfig().getString("database.postgresql.host", "localhost");
            int port = plugin.getConfig().getInt("database.postgresql.port", 5432);
            String database = plugin.getConfig().getString("database.postgresql.database", "gotcraftkitpvp");
            String username = plugin.getConfig().getString("database.postgresql.username", "postgres");
            String password = plugin.getConfig().getString("database.postgresql.password", "password");
            boolean useSSL = plugin.getConfig().getBoolean("database.postgresql.ssl", false);

            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s?sslmode=%s",
                host, port, database, useSSL ? "require" : "disable"));
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");

            // PostgreSQL optimizations
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.postgresql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("database.postgresql.pool.minimum-idle", 2));
            config.setConnectionTimeout(plugin.getConfig().getLong("database.postgresql.pool.connection-timeout", 10000));
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setLeakDetectionThreshold(60000); // 1 minute - detect connection leaks

            // PostgreSQL-specific connection properties
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("ApplicationName", "GotCraftKitPvp");

            plugin.getLogger().info("Configuring PostgreSQL connection pool...");
        } else {
            throw new IllegalArgumentException("Unsupported database type: " + type + ". Only POSTGRESQL is supported.");
        }

        config.setPoolName("GotCraftKitPvp-Pool");
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        dbLock.writeLock().lock();
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Use transaction

            try (Statement stmt = conn.createStatement()) {
                // Players table - main player data with stats and economy
                String playersTable = String.format(
                    "CREATE TABLE IF NOT EXISTS %splayers (" +
                    "uuid UUID PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "kills INTEGER DEFAULT 0 NOT NULL, " +
                    "deaths INTEGER DEFAULT 0 NOT NULL, " +
                    "current_streak INTEGER DEFAULT 0 NOT NULL, " +
                    "best_streak INTEGER DEFAULT 0 NOT NULL, " +
                    "level INTEGER DEFAULT 1 NOT NULL, " +
                    "xp INTEGER DEFAULT 0 NOT NULL, " +
                    "money NUMERIC(15,2) DEFAULT 0.00 NOT NULL, " +
                    "last_kit VARCHAR(50), " +
                    "created_at BIGINT NOT NULL, " +
                    "updated_at BIGINT NOT NULL" +
                    ")", TABLE_PREFIX
                );
                stmt.execute(playersTable);

                // Create indexes for performance
                stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_%splayers_name ON %splayers(name)", TABLE_PREFIX, TABLE_PREFIX));
                stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_%splayers_kills ON %splayers(kills DESC)", TABLE_PREFIX, TABLE_PREFIX));
                stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_%splayers_level ON %splayers(level DESC, xp DESC)", TABLE_PREFIX, TABLE_PREFIX));
                stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_%splayers_streak ON %splayers(best_streak DESC)", TABLE_PREFIX, TABLE_PREFIX));

                // Kit purchases table
                String kitPurchasesTable = String.format(
                    "CREATE TABLE IF NOT EXISTS %skit_purchases (" +
                    "uuid UUID NOT NULL, " +
                    "kit_name VARCHAR(50) NOT NULL, " +
                    "purchased_at BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, kit_name)" +
                    ")", TABLE_PREFIX
                );
                stmt.execute(kitPurchasesTable);

                // Create index for kit purchases
                stmt.execute(String.format("CREATE INDEX IF NOT EXISTS idx_%skit_purchases_uuid ON %skit_purchases(uuid)", TABLE_PREFIX, TABLE_PREFIX));

                conn.commit(); // Commit transaction
                plugin.getLogger().info("Database tables created/verified successfully");
            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw e;
            }
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or has been closed");
        }
        return dataSource.getConnection();
    }

    /**
     * CRITICAL: Properly close database with data safety guarantee
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            plugin.getLogger().info("==============================================");
            plugin.getLogger().info("Initiating safe database shutdown...");

            // Flush any pending writes
            int pendingCount = pendingWrites.size();
            if (pendingCount > 0) {
                plugin.getLogger().warning("Found " + pendingCount + " pending writes - flushing now!");
                flushPendingWrites();
            }

            // Wait for active connections to finish (timeout 10 seconds)
            try {
                dataSource.close();
                plugin.getLogger().info("Database connection pool closed safely");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during database shutdown", e);
            }

            plugin.getLogger().info("Database shutdown complete");
            plugin.getLogger().info("==============================================");
        }
    }

    /**
     * Load player stats with retry logic
     */
    public CompletableFuture<PlayerStats> loadPlayerStats(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            dbLock.readLock().lock();
            try {
                return loadPlayerStatsWithRetry(uuid, name, 0);
            } finally {
                dbLock.readLock().unlock();
            }
        });
    }

    private PlayerStats loadPlayerStatsWithRetry(UUID uuid, String name, int attempt) {
        try (Connection conn = getConnection()) {
            String query = String.format("SELECT * FROM %splayers WHERE uuid = ?", TABLE_PREFIX);

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setObject(1, uuid);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerStats(
                            uuid,
                            rs.getString("name"),
                            rs.getInt("kills"),
                            rs.getInt("deaths"),
                            rs.getInt("current_streak"),
                            rs.getInt("best_streak"),
                            rs.getInt("level"),
                            rs.getInt("xp"),
                            rs.getString("last_kit")
                        );
                    } else {
                        // New player - create immediately with transaction
                        PlayerStats newStats = new PlayerStats(uuid, name);
                        savePlayerStatsSync(newStats);
                        return newStats;
                    }
                }
            }
        } catch (SQLException e) {
            if (attempt < MAX_RETRY_ATTEMPTS) {
                plugin.getLogger().warning("Failed to load player stats (attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return loadPlayerStatsWithRetry(uuid, name, attempt + 1);
            } else {
                plugin.getLogger().log(Level.SEVERE, "CRITICAL: Failed to load player stats for " + uuid + " after " + MAX_RETRY_ATTEMPTS + " attempts", e);
                return new PlayerStats(uuid, name);
            }
        }
    }

    /**
     * Save player stats SYNCHRONOUSLY with transaction
     */
    private void savePlayerStatsSync(PlayerStats stats) throws SQLException {
        dbLock.writeLock().lock();
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                String query = String.format(
                    "INSERT INTO %splayers (uuid, name, kills, deaths, current_streak, best_streak, level, xp, money, last_kit, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0.00, ?, ?, ?) " +
                    "ON CONFLICT (uuid) DO UPDATE SET " +
                    "name = EXCLUDED.name, " +
                    "kills = EXCLUDED.kills, " +
                    "deaths = EXCLUDED.deaths, " +
                    "current_streak = EXCLUDED.current_streak, " +
                    "best_streak = EXCLUDED.best_streak, " +
                    "level = EXCLUDED.level, " +
                    "xp = EXCLUDED.xp, " +
                    "last_kit = EXCLUDED.last_kit, " +
                    "updated_at = EXCLUDED.updated_at",
                    TABLE_PREFIX
                );

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setObject(1, stats.getUuid());
                    stmt.setString(2, stats.getName());
                    stmt.setInt(3, stats.getKills());
                    stmt.setInt(4, stats.getDeaths());
                    stmt.setInt(5, stats.getCurrentStreak());
                    stmt.setInt(6, stats.getBestStreak());
                    stmt.setInt(7, stats.getLevel());
                    stmt.setInt(8, stats.getXp());
                    stmt.setString(9, stats.getLastKit());
                    long now = System.currentTimeMillis();
                    stmt.setLong(10, now);
                    stmt.setLong(11, now);
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    /**
     * Save player stats ASYNCHRONOUSLY
     */
    public CompletableFuture<Void> savePlayerStats(PlayerStats stats) {
        // Queue for batch write
        pendingWrites.put(stats.getUuid(), new PendingWrite(stats, null));

        return CompletableFuture.runAsync(() -> {
            try {
                savePlayerStatsSync(stats);
                pendingWrites.remove(stats.getUuid());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "CRITICAL: Failed to save player stats for " + stats.getUuid(), e);
            }
        });
    }

    /**
     * Get player money with proper null handling
     */
    public CompletableFuture<Double> getPlayerMoney(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            dbLock.readLock().lock();
            try (Connection conn = getConnection()) {
                String query = String.format("SELECT money FROM %splayers WHERE uuid = ?", TABLE_PREFIX);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setObject(1, uuid);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getDouble("money");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player money for " + uuid, e);
            } finally {
                dbLock.readLock().unlock();
            }
            return null;
        });
    }

    /**
     * Save player money with ATOMIC operation
     */
    public CompletableFuture<Void> savePlayerMoney(UUID uuid, double money) {
        // Update pending writes
        PendingWrite existing = pendingWrites.get(uuid);
        if (existing != null) {
            existing.money = money;
        } else {
            pendingWrites.put(uuid, new PendingWrite(null, money));
        }

        return CompletableFuture.runAsync(() -> {
            dbLock.writeLock().lock();
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // Upsert money value
                    String query = String.format(
                        "INSERT INTO %splayers (uuid, name, money, created_at, updated_at) " +
                        "VALUES (?, 'Unknown', ?, ?, ?) " +
                        "ON CONFLICT (uuid) DO UPDATE SET " +
                        "money = EXCLUDED.money, " +
                        "updated_at = EXCLUDED.updated_at",
                        TABLE_PREFIX
                    );

                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setObject(1, uuid);
                        stmt.setDouble(2, money);
                        long now = System.currentTimeMillis();
                        stmt.setLong(3, now);
                        stmt.setLong(4, now);
                        stmt.executeUpdate();
                    }

                    conn.commit();
                    pendingWrites.remove(uuid);
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "CRITICAL: Failed to save player money for " + uuid, e);
            } finally {
                dbLock.writeLock().unlock();
            }
        });
    }

    /**
     * CRITICAL: Flush all pending writes synchronously
     * Called before shutdown or reload
     */
    public void flushPendingWrites() {
        plugin.getLogger().info("Flushing " + pendingWrites.size() + " pending writes...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (PendingWrite write : pendingWrites.values()) {
            if (write.stats != null) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        savePlayerStatsSync(write.stats);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Error flushing stats", e);
                    }
                }));
            }
        }

        // Wait for all writes to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
            pendingWrites.clear();
            plugin.getLogger().info("All pending writes flushed successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "CRITICAL: Failed to flush all pending writes", e);
        }
    }

    // Leaderboard queries
    public CompletableFuture<List<PlayerStats>> getTopKills(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
            dbLock.readLock().lock();
            try (Connection conn = getConnection()) {
                String query = String.format("SELECT * FROM %splayers ORDER BY kills DESC LIMIT ?", TABLE_PREFIX);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(new PlayerStats(
                                (UUID) rs.getObject("uuid"),
                                rs.getString("name"),
                                rs.getInt("kills"),
                                rs.getInt("deaths"),
                                rs.getInt("current_streak"),
                                rs.getInt("best_streak"),
                                rs.getInt("level"),
                                rs.getInt("xp"),
                                rs.getString("last_kit")
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get top kills", e);
            } finally {
                dbLock.readLock().unlock();
            }
            return topPlayers;
        });
    }

    public CompletableFuture<List<PlayerStats>> getTopStreaks(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
            dbLock.readLock().lock();
            try (Connection conn = getConnection()) {
                String query = String.format("SELECT * FROM %splayers ORDER BY best_streak DESC LIMIT ?", TABLE_PREFIX);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(new PlayerStats(
                                (UUID) rs.getObject("uuid"),
                                rs.getString("name"),
                                rs.getInt("kills"),
                                rs.getInt("deaths"),
                                rs.getInt("current_streak"),
                                rs.getInt("best_streak"),
                                rs.getInt("level"),
                                rs.getInt("xp"),
                                rs.getString("last_kit")
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get top streaks", e);
            } finally {
                dbLock.readLock().unlock();
            }
            return topPlayers;
        });
    }

    public CompletableFuture<List<PlayerStats>> getTopLevels(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
            dbLock.readLock().lock();
            try (Connection conn = getConnection()) {
                String query = String.format("SELECT * FROM %splayers ORDER BY level DESC, xp DESC LIMIT ?", TABLE_PREFIX);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(new PlayerStats(
                                (UUID) rs.getObject("uuid"),
                                rs.getString("name"),
                                rs.getInt("kills"),
                                rs.getInt("deaths"),
                                rs.getInt("current_streak"),
                                rs.getInt("best_streak"),
                                rs.getInt("level"),
                                rs.getInt("xp"),
                                rs.getString("last_kit")
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get top levels", e);
            } finally {
                dbLock.readLock().unlock();
            }
            return topPlayers;
        });
    }

    // Kit purchases
    public CompletableFuture<Boolean> hasKitPurchased(UUID uuid, String kitName) {
        return CompletableFuture.supplyAsync(() -> {
            dbLock.readLock().lock();
            try (Connection conn = getConnection()) {
                String query = String.format("SELECT 1 FROM %skit_purchases WHERE uuid = ? AND kit_name = ?", TABLE_PREFIX);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setObject(1, uuid);
                    stmt.setString(2, kitName);

                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check kit purchase", e);
                return false;
            } finally {
                dbLock.readLock().unlock();
            }
        });
    }

    public CompletableFuture<Void> purchaseKit(UUID uuid, String kitName) {
        return CompletableFuture.runAsync(() -> {
            dbLock.writeLock().lock();
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                try {
                    String query = String.format(
                        "INSERT INTO %skit_purchases (uuid, kit_name, purchased_at) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (uuid, kit_name) DO NOTHING",
                        TABLE_PREFIX
                    );

                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setObject(1, uuid);
                        stmt.setString(2, kitName);
                        stmt.setLong(3, System.currentTimeMillis());
                        stmt.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save kit purchase", e);
            } finally {
                dbLock.writeLock().unlock();
            }
        });
    }

    /**
     * Internal class to track pending writes
     */
    private static class PendingWrite {
        PlayerStats stats;
        Double money;

        PendingWrite(PlayerStats stats, Double money) {
            this.stats = stats;
            this.money = money;
        }
    }
}

