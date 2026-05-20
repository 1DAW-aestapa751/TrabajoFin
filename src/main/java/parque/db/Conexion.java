package parque.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase de conexión a MySQL usando el patrón Singleton thread-safe.
 *
 * Garantiza que exista una única instancia de Connection en toda la
 * aplicación, evitando abrir múltiples conexiones innecesarias.
 *
 * Uso:
 *   Connection con = Conexion.getInstancia().getConnection();
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class Conexion {

    // ── Parámetros de conexión ───────────────────────────────────────────────
    private static final String URL      = "jdbc:mysql://localhost:3306/parque_atracciones"
                                         + "?useSSL=false"
                                         + "&allowPublicKeyRetrieval=true"
                                         + "&serverTimezone=Europe/Madrid"
                                         + "&characterEncoding=UTF-8";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "";          // ← cambiar en producción

    // ── Instancia única (volatile garantiza visibilidad en hilos) ───────────
    private static volatile Conexion instancia = null;

    // ── Objeto de conexión JDBC ──────────────────────────────────────────────
    private Connection connection;

    // ── Constructor privado — impide instanciación externa ──────────────────
    private Conexion() {
        conectar();
    }

    /**
     * Devuelve la única instancia de Conexion (Singleton con doble comprobación).
     * Thread-safe sin necesidad de sincronizar todo el método.
     */
    public static Conexion getInstancia() {
        if (instancia == null) {
            synchronized (Conexion.class) {
                if (instancia == null) {
                    instancia = new Conexion();
                }
            }
        }
        return instancia;
    }

    /**
     * Devuelve el objeto Connection activo.
     * Si la conexión está cerrada o es nula, la restablece automáticamente.
     *
     * @return Connection activa con la base de datos
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Reconectando a MySQL...");
                conectar();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al verificar la conexión: " + e.getMessage());
            conectar();
        }
        return connection;
    }

    /**
     * Abre la conexión con MySQL cargando el driver JDBC.
     */
    private void conectar() {
        try {
            // Desde Java 6+ el driver se carga automáticamente via ServiceLoader,
            // pero lo registramos explícitamente para mayor compatibilidad.
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USUARIO, PASSWORD);
            System.out.println("[DB] Conexión establecida con parque_atracciones.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver MySQL no encontrado. ¿Has añadido mysql-connector-j al classpath?");
            System.err.println("     Descarga: https://dev.mysql.com/downloads/connector/j/");
            throw new RuntimeException("Driver JDBC no disponible.", e);
        } catch (SQLException e) {
            System.err.println("[DB] Error al conectar con MySQL: " + e.getMessage());
            System.err.println("     Verifica que MySQL está arrancado y las credenciales son correctas.");
            throw new RuntimeException("Fallo en la conexión a la BD.", e);
        }
    }

    /**
     * Cierra la conexión activa. Llamar al cerrar la aplicación.
     */
    public void cerrarConexion() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                instancia = null;
                System.out.println("[DB] Conexión cerrada correctamente.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
