package xyz.nikitacartes.easywhitelist.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class WhitelistStore implements AutoCloseable {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+$");
    private static final String TABLE_NAME = "whitelist_entries";

    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private volatile Set<String> cachedNames = Set.of();
    private volatile boolean ready;

    public WhitelistStore(JavaPlugin plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();
        String jdbcUrl = config.getString("database.jdbc-url", "");

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("database.jdbc-url must be set in config.yml");
        }

        if (!TABLE_NAME_PATTERN.matcher(TABLE_NAME).matches()) {
            throw new IllegalStateException("Invalid hardcoded whitelist table name");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setPoolName("EasyWhitelistPool");
        hikariConfig.setAutoCommit(true);

        dataSource = new HikariDataSource(hikariConfig);
    }

    public void initialize() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + "nickname VARCHAR(16) PRIMARY KEY,"
                    + "active BOOLEAN NOT NULL DEFAULT TRUE,"
                    + "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),"
                    + "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()"
                    + ")");
        }

        reloadCache(true);
    }

    public void reloadCache() throws SQLException {
        reloadCache(false);
    }

    public void reloadCache(boolean logResult) throws SQLException {
        Set<String> names = new HashSet<>();

        try (Connection connection = dataSource.getConnection();
               PreparedStatement statement = connection.prepareStatement("SELECT nickname FROM " + TABLE_NAME + " WHERE active = TRUE");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(normalize(resultSet.getString("nickname")));
            }
        }

        cachedNames = Collections.unmodifiableSet(names);
        ready = true;
        if (logResult) {
            plugin.getLogger().info("Loaded " + cachedNames.size() + " whitelist entries from PostgreSQL.");
        }
    }

    public boolean isWhitelisted(String name) {
        return cachedNames.contains(normalize(name));
    }

    public Set<String> snapshot() {
        return cachedNames;
    }

    public boolean isReady() {
        return ready;
    }

    public void upsert(String nickname, boolean active) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO " + TABLE_NAME + " (nickname, active, updated_at) VALUES (?, ?, NOW()) "
                             + "ON CONFLICT (nickname) DO UPDATE SET active = EXCLUDED.active, updated_at = NOW()")) {
            statement.setString(1, normalize(nickname));
            statement.setBoolean(2, active);
            statement.executeUpdate();
        }
        reloadCache(false);
    }

    public void remove(String nickname) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE nickname = ?")) {
            statement.setString(1, normalize(nickname));
            statement.executeUpdate();
        }
        reloadCache(false);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @Override
    public void close() {
        dataSource.close();
    }
}