package me.lubomirstankov.gotCraftKitPvp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final GotCraftKitPvp plugin;
    private HikariDataSource dataSource;
    private final String type;

    public DatabaseManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        this.type = plugin.getConfigManager().getDatabaseType();
    }

    public void initialize() {
        try {
            setupDataSource();
            createTables();

            // Warm up the connection pool
            warmUpConnections();

            plugin.getLogger().info("Database initialized successfully!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void warmUpConnections() {
        // Pre-create connections to avoid first-use delays
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            plugin.getLogger().info("Database connection pool warmed up");
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to warm up database connections: " + e.getMessage());
        }
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("MYSQL")) {
            String host = plugin.getConfig().getString("database.mysql.host");
            int port = plugin.getConfig().getInt("database.mysql.port");
            String database = plugin.getConfig().getString("database.mysql.database");
            String username = plugin.getConfig().getString("database.mysql.username");
            String password = plugin.getConfig().getString("database.mysql.password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 2));
            config.setConnectionTimeout(plugin.getConfig().getLong("database.mysql.pool.connection-timeout", 5000));

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

        } else {
            // SQLite
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String path = dataFolder.getAbsolutePath() + File.separator + "data.db";
            config.setJdbcUrl("jdbc:sqlite:" + path);
            config.setMaximumPoolSize(1);
            config.setConnectionTestQuery("SELECT 1");
        }

        dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Player stats table
            String createStatsTable = "CREATE TABLE IF NOT EXISTS player_stats ("
                    + "uuid VARCHAR(36) PRIMARY KEY,"
                    + "name VARCHAR(16) NOT NULL,"
                    + "kills INTEGER DEFAULT 0,"
                    + "deaths INTEGER DEFAULT 0,"
                    + "current_streak INTEGER DEFAULT 0,"
                    + "best_streak INTEGER DEFAULT 0,"
                    + "level INTEGER DEFAULT 1,"
                    + "xp INTEGER DEFAULT 0,"
                    + "last_kit VARCHAR(50),"
                    + "money DOUBLE DEFAULT 0.0,"
                    + "created_at BIGINT,"
                    + "updated_at BIGINT"
                    + ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createStatsTable);
            }

            // Add money column if it doesn't exist (for existing databases)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN money DOUBLE DEFAULT 0.0");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // Kit purchases table (for economy)
            String createKitPurchasesTable = "CREATE TABLE IF NOT EXISTS kit_purchases ("
                    + "uuid VARCHAR(36),"
                    + "kit_name VARCHAR(50),"
                    + "purchased_at BIGINT,"
                    + "PRIMARY KEY (uuid, kit_name)"
                    + ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createKitPurchasesTable);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            plugin.getLogger().info("Closing database connection pool...");
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    // Async database operations
    public CompletableFuture<PlayerStats> loadPlayerStats(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM player_stats WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());

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
                            // Create new stats - don't call savePlayerStats here to avoid deadlock
                            PlayerStats stats = new PlayerStats(uuid, name);

                            // Save synchronously within this transaction
                            String insertQuery = "INSERT OR REPLACE INTO player_stats (uuid, name, kills, deaths, current_streak, best_streak, level, xp, last_kit, created_at, updated_at) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                            if (type.equalsIgnoreCase("MYSQL")) {
                                insertQuery = "INSERT INTO player_stats (uuid, name, kills, deaths, current_streak, best_streak, level, xp, last_kit, created_at, updated_at) "
                                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                        + "ON DUPLICATE KEY UPDATE name=VALUES(name), kills=VALUES(kills), deaths=VALUES(deaths), "
                                        + "current_streak=VALUES(current_streak), best_streak=VALUES(best_streak), level=VALUES(level), "
                                        + "xp=VALUES(xp), last_kit=VALUES(last_kit), updated_at=VALUES(updated_at)";
                            }

                            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                                insertStmt.setString(1, stats.getUuid().toString());
                                insertStmt.setString(2, stats.getName());
                                insertStmt.setInt(3, stats.getKills());
                                insertStmt.setInt(4, stats.getDeaths());
                                insertStmt.setInt(5, stats.getCurrentStreak());
                                insertStmt.setInt(6, stats.getBestStreak());
                                insertStmt.setInt(7, stats.getLevel());
                                insertStmt.setInt(8, stats.getXp());
                                insertStmt.setString(9, stats.getLastKit());
                                insertStmt.setLong(10, System.currentTimeMillis());
                                insertStmt.setLong(11, System.currentTimeMillis());
                                insertStmt.executeUpdate();
                            }

                            return stats;
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player stats for " + uuid, e);
                return new PlayerStats(uuid, name);
            }
        });
    }

    public CompletableFuture<Void> savePlayerStats(PlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String query = "INSERT OR REPLACE INTO player_stats (uuid, name, kills, deaths, current_streak, best_streak, level, xp, last_kit, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                // For MySQL, use ON DUPLICATE KEY UPDATE
                if (type.equalsIgnoreCase("MYSQL")) {
                    query = "INSERT INTO player_stats (uuid, name, kills, deaths, current_streak, best_streak, level, xp, last_kit, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE name=VALUES(name), kills=VALUES(kills), deaths=VALUES(deaths), "
                            + "current_streak=VALUES(current_streak), best_streak=VALUES(best_streak), level=VALUES(level), "
                            + "xp=VALUES(xp), last_kit=VALUES(last_kit), updated_at=VALUES(updated_at)";
                }

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, stats.getUuid().toString());
                    stmt.setString(2, stats.getName());
                    stmt.setInt(3, stats.getKills());
                    stmt.setInt(4, stats.getDeaths());
                    stmt.setInt(5, stats.getCurrentStreak());
                    stmt.setInt(6, stats.getBestStreak());
                    stmt.setInt(7, stats.getLevel());
                    stmt.setInt(8, stats.getXp());
                    stmt.setString(9, stats.getLastKit());
                    stmt.setLong(10, System.currentTimeMillis());
                    stmt.setLong(11, System.currentTimeMillis());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player stats for " + stats.getUuid(), e);
            }
        });
    }

    public CompletableFuture<List<PlayerStats>> getTopKills(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM player_stats ORDER BY kills DESC LIMIT ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(new PlayerStats(
                                    UUID.fromString(rs.getString("uuid")),
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
            }
            return topPlayers;
        });
    }

    public CompletableFuture<List<PlayerStats>> getTopStreaks(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM player_stats ORDER BY best_streak DESC LIMIT ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(new PlayerStats(
                                    UUID.fromString(rs.getString("uuid")),
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
            }
            return topPlayers;
        });
    }

    public CompletableFuture<List<PlayerStats>> getTopLevels(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> topPlayers = new ArrayList<>();
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM player_stats ORDER BY level DESC, xp DESC LIMIT ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            topPlayers.add(new PlayerStats(
                                    UUID.fromString(rs.getString("uuid")),
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
            }
            return topPlayers;
        });
    }

    // Kit purchases
    public CompletableFuture<Boolean> hasKitPurchased(UUID uuid, String kitName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String query = "SELECT * FROM kit_purchases WHERE uuid = ? AND kit_name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, kitName);

                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check kit purchase", e);
                return false;
            }
        });
    }

    public CompletableFuture<Void> purchaseKit(UUID uuid, String kitName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                String query = "INSERT OR IGNORE INTO kit_purchases (uuid, kit_name, purchased_at) VALUES (?, ?, ?)";

                if (type.equalsIgnoreCase("MYSQL")) {
                    query = "INSERT IGNORE INTO kit_purchases (uuid, kit_name, purchased_at) VALUES (?, ?, ?)";
                }

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, kitName);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save kit purchase", e);
            }
        });
    }

    // Economy methods
    public CompletableFuture<Double> getPlayerMoney(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                String query = "SELECT money FROM player_stats WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getDouble("money");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player money", e);
            }
            return null; // Indicates player not found, will use starting balance
        });
    }

    public CompletableFuture<Void> savePlayerMoney(UUID uuid, double money) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                // First ensure the player exists in stats table
                String insertQuery = "INSERT OR IGNORE INTO player_stats (uuid, name, money) VALUES (?, ?, ?)";
                if (type.equalsIgnoreCase("MYSQL")) {
                    insertQuery = "INSERT IGNORE INTO player_stats (uuid, name, money) VALUES (?, ?, ?)";
                }

                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, "Unknown"); // Will be updated on join
                    stmt.setDouble(3, money);
                    stmt.executeUpdate();
                }

                // Update money
                String updateQuery = "UPDATE player_stats SET money = ? WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    stmt.setDouble(1, money);
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player money", e);
            }
        });
    }
}


