package com.studentleave.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database connection manager with connection pooling
 * Demonstrates JDBC usage, connection pooling, and resource management
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    
    // Database configuration
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;
    private static String DB_DRIVER;
    
    // Connection pool settings
    private static final int INITIAL_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final ConcurrentLinkedQueue<Connection> connectionPool = new ConcurrentLinkedQueue<>();
    private static volatile boolean initialized = false;
    
    static {
        initializeDatabase();
    }
    
    /**
     * Initialize database configuration from properties file
     */
    private static void initializeDatabase() {
        try {
            loadDatabaseProperties();
            loadDatabaseDriver();
            initializeConnectionPool();
            initialized = true;
            logger.info("Database connection manager initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection manager", e);
            // Set default values for demo purposes
            setDefaultConfiguration();
        }
    }
    
    /**
     * Load database properties from configuration file
     */
    private static void loadDatabaseProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class.getResourceAsStream("/database.properties")) {
            if (input != null) {
                props.load(input);
                DB_URL = props.getProperty("db.url", "jdbc:mysql://localhost:3306/student_leave_db");
                DB_USER = props.getProperty("db.username", "root");
                DB_PASSWORD = props.getProperty("db.password", "");
                DB_DRIVER = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            } else {
                // Properties file not found, use default configuration
                setDefaultConfiguration();
            }
        }
    }
    
    /**
     * Set default database configuration for demo purposes
     */
    private static void setDefaultConfiguration() {
        DB_URL = "jdbc:mysql://localhost:3306/student_leave_db";
        DB_USER = "root";
        DB_PASSWORD = "trupti@123";
        DB_DRIVER = "com.mysql.cj.jdbc.Driver";
        logger.info("Using default database configuration");
    }
    
    /**
     * Load and register database driver
     */
    private static void loadDatabaseDriver() {
        try {
            Class.forName(DB_DRIVER);
            logger.info("Database driver loaded successfully: " + DB_DRIVER);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to load database driver: " + DB_DRIVER, e);
            throw new RuntimeException("Database driver not found", e);
        }
    }
    
    /**
     * Initialize connection pool with initial connections
     */
    private static void initializeConnectionPool() {
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            try {
                Connection connection = createNewConnection();
                if (connection != null) {
                    connectionPool.offer(connection);
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to create initial connection for pool", e);
            }
        }
        logger.info("Connection pool initialized with " + connectionPool.size() + " connections");
    }
    
    /**
     * Create a new database connection
     */
    private static Connection createNewConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create database connection", e);
            throw e;
        }
    }
    
    /**
     * Get a connection from the pool
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database connection manager not initialized");
        }
        
        Connection connection = connectionPool.poll();
        
        // If no connection available in pool or connection is closed, create new one
        if (connection == null || connection.isClosed()) {
            if (getActiveConnectionCount() < MAX_POOL_SIZE) {
                connection = createNewConnection();
                logger.fine("Created new connection (pool was empty)");
            } else {
                throw new SQLException("Maximum connection pool size reached");
            }
        } else {
            logger.fine("Reused connection from pool");
        }
        
        return connection;
    }
    
    /**
     * Return connection to the pool
     */
    public static void returnConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connectionPool.offer(connection);
                    logger.fine("Connection returned to pool");
                } else {
                    logger.fine("Closed connection not returned to pool");
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error checking connection status", e);
            }
        }
    }
    
    /**
     * Close connection (use this when connection should not be reused)
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.fine("Connection closed");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }
    
    /**
     * Execute a SQL script (for database initialization)
     */
    public static boolean executeScript(String script) {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            
            String[] statements = script.split(";");
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    statement.execute(sql.trim());
                }
            }
            returnConnection(connection);
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute SQL script", e);
            return false;
        }
    }
    
    /**
     * Test database connectivity
     */
    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            boolean isValid = connection.isValid(5); // 5 second timeout
            returnConnection(connection);
            return isValid;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get current active connection count (approximate)
     */
    public static int getActiveConnectionCount() {
        return MAX_POOL_SIZE - connectionPool.size();
    }
    
    /**
     * Get pool size
     */
    public static int getPoolSize() {
        return connectionPool.size();
    }
    
    /**
     * Shutdown connection pool
     */
    public static void shutdown() {
        logger.info("Shutting down database connection pool...");
        while (!connectionPool.isEmpty()) {
            Connection connection = connectionPool.poll();
            closeConnection(connection);
        }
        logger.info("Database connection pool shutdown complete");
    }
    
    /**
     * Get database URL (for information purposes)
     */
    public static String getDatabaseUrl() {
        return DB_URL;
    }
}
