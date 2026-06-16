package com.iskollect.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton JDBC connection manager for Iskollect.
 *
 * Reads connection credentials from config.properties (on the classpath).
 * Reconnects automatically if the stored connection has gone stale.
 *
 * config.properties keys:
 *   db.url      = jdbc:postgresql://localhost:5432/iskollect_db
 *   db.user     = <username>
 *   db.password = <password>
 *
 * Maven dependency (pom.xml):
 *   <dependency>
 *       <groupId>org.postgresql</groupId>
 *       <artifactId>postgresql</artifactId>
 *       <version>42.7.3</version>
 *   </dependency>
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;

    // ── Properties keys ───────────────────────────────────────────────────
    private static final String CONFIG_FILE  = "config.properties";
    private static final String KEY_URL      = "db.url";
    private static final String KEY_USER     = "db.user";
    private static final String KEY_PASS     = "db.password";

    // ── PostgreSQL driver class ───────────────────────────────────────────
    private static final String PG_DRIVER    = "org.postgresql.Driver";

    // ── Private constructor (Singleton) ───────────────────────────────────

    private DBConnection() {
        try {
            Properties props = loadProperties();
            String url  = props.getProperty(KEY_URL);
            String user = props.getProperty(KEY_USER);
            String pass = props.getProperty(KEY_PASS);

            Class.forName(PG_DRIVER);
            this.connection = DriverManager.getConnection(url, user, pass);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "PostgreSQL JDBC driver not found. Add org.postgresql:postgresql to pom.xml.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to connect to the database. Check config.properties.", e);
        } catch (IOException e) {
            throw new RuntimeException(
                "config.properties not found on classpath.", e);
        }
    }

    // ── Singleton accessor ────────────────────────────────────────────────

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    // ── Connection accessor ───────────────────────────────────────────────

    /**
     * Returns the active Connection. If the connection has gone stale
     * (e.g., idle timeout), it is re-created transparently.
     */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                instance = new DBConnection();
                return instance.connection;
            }
        } catch (SQLException e) {
            instance = new DBConnection();
            return instance.connection;
        }
        return connection;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Warning: could not close DB connection — " + e.getMessage());
            } finally {
                connection = null;
                instance   = null;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) throw new IOException(CONFIG_FILE + " not found on classpath.");
            props.load(in);
        }
        return props;
    }
}