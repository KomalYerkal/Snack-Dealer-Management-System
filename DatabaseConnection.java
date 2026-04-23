package snack.db;

import java.sql.*;

/**
 * Singleton database connection wrapper.
 * Configure URL / USER / PASSWORD before running.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/snack_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "komal2006"; // ← change to your MySQL password

    private static Connection conn = null;

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Add mysql-connector-j.jar to classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to database: " + e.getMessage(), e);
        }
        return conn;
    }

    public static void close() {
        try {
            if (conn != null && !conn.isClosed())
                conn.close();
        } catch (SQLException ignored) {
        }
    }
}