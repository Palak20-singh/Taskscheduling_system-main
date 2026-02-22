import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection
 * Provides a single static method to get a PostgreSQL JDBC connection.
 *
 * HOW TO CONFIGURE:
 *   Change the HOST, PORT, DATABASE, USER, PASSWORD constants below
 *   to match your local PostgreSQL installation.
 */
public class DBConnection {

    // ── Edit these to match your PostgreSQL setup ────────────────────────────
    private static final String HOST     = "localhost";
    private static final String PORT     = "5432";
    private static final String DATABASE = "promanage_db";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "yourpassword";    // ← change this
    // ─────────────────────────────────────────────────────────────────────────

    private static final String URL =
            "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;

    /**
     * Opens and returns a new JDBC Connection.
     * Always close the connection in a finally block or try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "PostgreSQL JDBC driver not found.\n"
                + "Add postgresql-<version>.jar to your project libraries.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Quick connection test — call from Main to verify setup. */
    public static void testConnection() {
        System.out.println("Testing database connection...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("SUCCESS: Connected to database -> " + DATABASE);
            }
        } catch (SQLException e) {
            System.err.println("FAILED: " + e.getMessage());
        }
    }
}
