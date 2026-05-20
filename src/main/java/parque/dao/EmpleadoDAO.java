package parque.dao;

import parque.db.Conexion;
import parque.modelo.Empleado;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la entidad Empleado.
 *
 * Implementa todas las operaciones CRUD contra la tabla EMPLEADO de MySQL,
 * más consultas adicionales específicas del negocio (por categoría, búsqueda
 * por DNI, empleado de mayor categoría de un almacén, etc.).
 *
 * Usa PreparedStatement para prevenir inyección SQL y gestiona
 * los recursos JDBC con try-with-resources.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class EmpleadoDAO implements IDao<Empleado, Integer> {

    // ── Referencia a la conexión Singleton ──────────────────────────────────
    private final Connection conexion;

    public EmpleadoDAO() {
        this.conexion = Conexion.getInstancia().getConnection();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CRUD BÁSICO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * INSERT — Inserta un nuevo empleado en la base de datos.
     * El codEmpleado es AUTO_INCREMENT; MySQL lo asigna automáticamente.
     * Tras la inserción, el id generado se inyecta de vuelta en el objeto.
     *
     * @param empleado objeto Empleado a insertar (codEmpleado será ignorado)
     * @return true si la inserción fue exitosa
     */
    @Override
    public boolean insertar(Empleado empleado) {
        final String SQL = """
            INSERT INTO EMPLEADO
                (nombre, apellidos, dni, num_ss, categoria,
                 fecha_nacimiento, domicilio, telefono, estado_civil, num_hijos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1,  empleado.getNombre());
            ps.setString(2,  empleado.getApellidos());
            ps.setString(3,  empleado.getDni());
            ps.setString(4,  empleado.getNumSS());
            ps.setString(5,  empleado.getCategoria());
            ps.setDate(6,    Date.valueOf(empleado.getFechaNacimiento()));
            ps.setString(7,  empleado.getDomicilio());
            ps.setString(8,  empleado.getTelefono());
            ps.setString(9,  empleado.getEstadoCivil());
            ps.setInt(10,    empleado.getNumHijos());

            int filasAfectadas = ps.executeUpdate();

            // Recuperar el id generado por AUTO_INCREMENT
            if (filasAfectadas > 0) {
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        empleado.setCodEmpleado(claves.getInt(1));
                    }
                }
                System.out.printf("[DAO] Empleado insertado con código %d.%n",
                                  empleado.getCodEmpleado());
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("[DAO] Error: DNI o N.º S.S. ya registrado → " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DAO] Error al insertar empleado: " + e.getMessage());
        }
        return false;
    }

    /**
     * SELECT por PK — Busca un empleado por su código.
     *
     * @param codEmpleado clave primaria del empleado
     * @return objeto Empleado, o null si no existe
     */
    @Override
    public Empleado buscarPorId(Integer codEmpleado) {
        final String SQL = "SELECT * FROM EMPLEADO WHERE cod_empleado = ?";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codEmpleado);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar empleado " + codEmpleado + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * SELECT ALL — Recupera la lista completa de empleados.
     *
     * @return lista de todos los empleados, vacía si no hay ninguno
     */
    @Override
    public List<Empleado> buscarTodos() {
        final String SQL = "SELECT * FROM EMPLEADO ORDER BY apellidos, nombre";
        List<Empleado> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al recuperar empleados: " + e.getMessage());
        }
        return lista;
    }

    /**
     * UPDATE — Actualiza todos los campos modificables de un empleado.
     * El codEmpleado identifica la fila; el DNI y N.º SS no se modifican
     * una vez registrados (unicidad garantizada).
     *
     * @param empleado objeto con los nuevos datos (codEmpleado debe ser válido)
     * @return true si se actualizó al menos una fila
     */
    @Override
    public boolean actualizar(Empleado empleado) {
        final String SQL = """
            UPDATE EMPLEADO SET
                nombre           = ?,
                apellidos        = ?,
                categoria        = ?,
                fecha_nacimiento = ?,
                domicilio        = ?,
                telefono         = ?,
                estado_civil     = ?,
                num_hijos        = ?
            WHERE cod_empleado = ?
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {

            ps.setString(1, empleado.getNombre());
            ps.setString(2, empleado.getApellidos());
            ps.setString(3, empleado.getCategoria());
            ps.setDate(4,   Date.valueOf(empleado.getFechaNacimiento()));
            ps.setString(5, empleado.getDomicilio());
            ps.setString(6, empleado.getTelefono());
            ps.setString(7, empleado.getEstadoCivil());
            ps.setInt(8,    empleado.getNumHijos());
            ps.setInt(9,    empleado.getCodEmpleado());

            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.printf("[DAO] Empleado %d actualizado.%n", empleado.getCodEmpleado());
            return ok;

        } catch (SQLException e) {
            System.err.println("[DAO] Error al actualizar empleado: " + e.getMessage());
        }
        return false;
    }

    /**
     * DELETE — Elimina un empleado por su código.
     * MySQL lanzará un error si el empleado tiene nóminas, revisiones o pedidos
     * asociados (ON DELETE RESTRICT), lo que capturamos y reportamos.
     *
     * @param codEmpleado código del empleado a eliminar
     * @return true si se eliminó correctamente
     */
    @Override
    public boolean borrar(Integer codEmpleado) {
        final String SQL = "DELETE FROM EMPLEADO WHERE cod_empleado = ?";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codEmpleado);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.printf("[DAO] Empleado %d eliminado.%n", codEmpleado);
            return ok;

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("[DAO] No se puede eliminar: el empleado tiene registros dependientes.");
        } catch (SQLException e) {
            System.err.println("[DAO] Error al eliminar empleado: " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSULTAS ESPECÍFICAS DE NEGOCIO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Busca un empleado por su DNI (clave natural alternativa).
     *
     * @param dni DNI del empleado
     * @return objeto Empleado, o null si no existe
     */
    public Empleado buscarPorDni(String dni) {
        final String SQL = "SELECT * FROM EMPLEADO WHERE dni = ?";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setString(1, dni.toUpperCase().trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar por DNI: " + e.getMessage());
        }
        return null;
    }

    /**
     * Recupera todos los empleados de una categoría específica,
     * ordenados por apellidos.
     *
     * @param codigoCategoria código de 2-3 letras (JA, ME, CA, etc.)
     * @return lista de empleados de esa categoría
     */
    public List<Empleado> buscarPorCategoria(String codigoCategoria) {
        final String SQL = "SELECT * FROM EMPLEADO WHERE categoria = ? ORDER BY apellidos, nombre";
        List<Empleado> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setString(1, codigoCategoria.toUpperCase().trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar por categoría: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Busca empleados cuyo nombre o apellidos contengan el texto dado.
     * Útil para implementar un buscador en la interfaz de usuario.
     *
     * @param texto fragmento de nombre o apellido a buscar
     * @return lista de empleados que coinciden
     */
    public List<Empleado> buscarPorNombre(String texto) {
        final String SQL = """
            SELECT * FROM EMPLEADO
            WHERE CONCAT(nombre, ' ', apellidos) LIKE ?
            ORDER BY apellidos, nombre
            """;
        List<Empleado> lista = new ArrayList<>();
        String patron = "%" + texto.trim() + "%";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setString(1, patron);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar por nombre: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Devuelve el empleado de mayor categoría asociado a un almacén concreto.
     *
     * Implementa la regla de negocio del enunciado: cuando el stock de un
     * producto baja del 40%, se notifica al empleado de mayor categoría
     * presente en ese almacén para que emita el pedido.
     *
     * El orden de precedencia de categorías (sueldo_base DESC) se obtiene
     * de la tabla CATEGORIA, que actúa como tabla de referencia ordenada.
     *
     * @param codAlmacen código del almacén
     * @return empleado de mayor categoría, o null si no hay ninguno asignado
     */
    public Empleado buscarEncargadoMayorCategoria(int codAlmacen) {
        final String SQL = """
            SELECT e.*
            FROM EMPLEADO e
            JOIN TIENDA t   ON e.cod_empleado = t.cod_emp_encargado
            JOIN ALMACEN a  ON t.cod_tienda   = a.cod_tienda
            JOIN CATEGORIA c ON e.categoria   = c.codigo
            WHERE a.cod_almacen = ?
            ORDER BY c.sueldo_base DESC
            LIMIT 1
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codAlmacen);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar encargado mayor categoría: " + e.getMessage());
        }
        return null;
    }

    /**
     * Devuelve el número total de empleados registrados.
     * Útil para estadísticas en pantalla principal.
     *
     * @return total de empleados
     */
    public int contarEmpleados() {
        final String SQL = "SELECT COUNT(*) FROM EMPLEADO";

        try (PreparedStatement ps = conexion.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[DAO] Error al contar empleados: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Verifica si ya existe un empleado con el DNI indicado.
     * Se usa en validaciones antes de insertar.
     *
     * @param dni DNI a verificar
     * @return true si el DNI ya está registrado
     */
    public boolean existeDni(String dni) {
        final String SQL = "SELECT COUNT(*) FROM EMPLEADO WHERE dni = ?";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setString(1, dni.toUpperCase().trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al verificar DNI: " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTODO PRIVADO DE MAPEO ResultSet → Empleado
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Convierte una fila del ResultSet en un objeto Empleado.
     * Centralizar el mapeo evita duplicar código en cada método de consulta.
     *
     * @param rs ResultSet posicionado en la fila a leer
     * @return objeto Empleado con los datos de la fila
     * @throws SQLException si hay error al leer alguna columna
     */
    private Empleado mapearResultSet(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();

        e.setCodEmpleado(rs.getInt("cod_empleado"));
        e.setNombre(rs.getString("nombre"));
        e.setApellidos(rs.getString("apellidos"));
        e.setDni(rs.getString("dni"));
        e.setNumSS(rs.getString("num_ss"));
        e.setCategoria(rs.getString("categoria"));

        // Date → LocalDate (el driver devuelve java.sql.Date)
        Date fechaSql = rs.getDate("fecha_nacimiento");
        if (fechaSql != null) e.setFechaNacimiento(fechaSql.toLocalDate());

        e.setDomicilio(rs.getString("domicilio"));
        e.setTelefono(rs.getString("telefono"));
        e.setEstadoCivil(rs.getString("estado_civil"));
        e.setNumHijos(rs.getInt("num_hijos"));

        return e;
    }
}
