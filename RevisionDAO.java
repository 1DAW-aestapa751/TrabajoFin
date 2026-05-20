package parque.dao;

import parque.db.Conexion;
import parque.modelo.Revision;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la entidad Revision.
 *
 * Gestiona el registro de revisiones mecánicas y la actualización
 * del estado de operatividad de las atracciones.
 *
 * La operación más importante es registrarRevision(), que en un solo
 * método de negocio realiza dos operaciones SQL enlazadas:
 *   1. INSERT INTO REVISION (registrar la revisión)
 *   2. UPDATE ATRACCION SET operativa = ? (cambiar estado si no es apta)
 *
 * Ambas se ejecutan dentro de una transacción para garantizar
 * la atomicidad: si falla cualquiera de las dos, se hace rollback.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class RevisionDAO implements IDao<Revision, Integer> {

    private final Connection conexion;

    public RevisionDAO() {
        this.conexion = Conexion.getInstancia().getConnection();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  OPERACIÓN CENTRAL — INSERT + UPDATE en transacción
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Registra una revisión mecánica y actualiza el estado de la atracción.
     *
     * Flujo transaccional:
     *  1. Desactiva autocommit → inicia transacción explícita.
     *  2. INSERT INTO REVISION con todos los datos del acta.
     *  3. UPDATE ATRACCION SET operativa = (esApta ? 1 : 0).
     *     - Si apta:    operativa = 1 (reabre si estaba clausurada)
     *     - Si no apta: operativa = 0 (clausura la atracción)
     *  4. COMMIT si ambas sentencias tienen éxito.
     *  5. ROLLBACK si cualquier sentencia lanza excepción.
     *  6. Restaura autocommit = true al finalizar.
     *
     * El uso de una transacción garantiza que nunca quede la BD en un
     * estado inconsistente (revisión insertada pero atracción sin actualizar).
     *
     * @param revision objeto Revision a registrar (codRevision será asignado por BD)
     * @return true si ambas sentencias se ejecutaron y se hizo commit
     */
    public boolean registrarConTransaccion(Revision revision) {
        final String SQL_INSERT_REVISION = """
            INSERT INTO REVISION
                (cod_atraccion, cod_empleado, fecha, es_apta, piezas_cambiar, proxima_revision)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        final String SQL_UPDATE_ATRACCION = """
            UPDATE ATRACCION
            SET operativa = ?
            WHERE cod_atraccion = ?
            """;

        try {
            // ── 1. Iniciar transacción ──────────────────────────────────────
            conexion.setAutoCommit(false);

            // ── 2. Insertar la revisión ─────────────────────────────────────
            try (PreparedStatement psRevision = conexion.prepareStatement(
                    SQL_INSERT_REVISION, Statement.RETURN_GENERATED_KEYS)) {

                psRevision.setInt(1,    revision.getCodAtraccion());
                psRevision.setInt(2,    revision.getCodEmpleado());
                psRevision.setDate(3,   Date.valueOf(revision.getFecha()));
                psRevision.setBoolean(4, revision.isEsApta());

                if (revision.getPiezasCambiar() != null && !revision.getPiezasCambiar().isBlank()) {
                    psRevision.setString(5, revision.getPiezasCambiar());
                } else {
                    psRevision.setNull(5, Types.VARCHAR);
                }

                if (revision.getProximaRevision() != null) {
                    psRevision.setDate(6, Date.valueOf(revision.getProximaRevision()));
                } else {
                    psRevision.setNull(6, Types.DATE);
                }

                psRevision.executeUpdate();

                // Recuperar el id generado
                try (ResultSet claves = psRevision.getGeneratedKeys()) {
                    if (claves.next()) revision.setCodRevision(claves.getInt(1));
                }
            }

            // ── 3. Actualizar estado de la atracción ────────────────────────
            try (PreparedStatement psAtraccion = conexion.prepareStatement(SQL_UPDATE_ATRACCION)) {
                // 1 = operativa (apta), 0 = clausurada (no apta)
                psAtraccion.setInt(1, revision.isEsApta() ? 1 : 0);
                psAtraccion.setInt(2, revision.getCodAtraccion());
                psAtraccion.executeUpdate();
            }

            // ── 4. Commit ────────────────────────────────────────────────────
            conexion.commit();

            String estadoStr = revision.isEsApta() ? "APTA — atracción operativa" : "NO APTA — atracción CLAUSURADA";
            System.out.printf("[DAO] Revisión %d registrada. Atracción %d: %s%n",
                revision.getCodRevision(), revision.getCodAtraccion(), estadoStr);
            return true;

        } catch (SQLException e) {
            // ── 5. Rollback si algo falla ────────────────────────────────────
            System.err.println("[DAO] Error en la transacción de revisión. Haciendo rollback...");
            System.err.println("      Causa: " + e.getMessage());
            try {
                conexion.rollback();
                System.err.println("[DAO] Rollback completado. BD en estado consistente.");
            } catch (SQLException ex) {
                System.err.println("[DAO] Error crítico al hacer rollback: " + ex.getMessage());
            }
        } finally {
            // ── 6. Restaurar autocommit siempre ─────────────────────────────
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("[DAO] No se pudo restaurar autocommit: " + e.getMessage());
            }
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CRUD BÁSICO (implementación de IDao)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * INSERT simple (sin actualizar ATRACCION).
     * Para la operación completa usa registrarConTransaccion().
     */
    @Override
    public boolean insertar(Revision revision) {
        final String SQL = """
            INSERT INTO REVISION
                (cod_atraccion, cod_empleado, fecha, es_apta, piezas_cambiar, proxima_revision)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,     revision.getCodAtraccion());
            ps.setInt(2,     revision.getCodEmpleado());
            ps.setDate(3,    Date.valueOf(revision.getFecha()));
            ps.setBoolean(4, revision.isEsApta());
            ps.setString(5,  revision.getPiezasCambiar());
            if (revision.getProximaRevision() != null)
                ps.setDate(6, Date.valueOf(revision.getProximaRevision()));
            else
                ps.setNull(6, Types.DATE);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) revision.setCodRevision(claves.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al insertar revisión: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Revision buscarPorId(Integer codRevision) {
        final String SQL = "SELECT * FROM REVISION WHERE cod_revision = ?";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codRevision);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar revisión: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Revision> buscarTodos() {
        final String SQL = "SELECT * FROM REVISION ORDER BY fecha DESC";
        List<Revision> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearResultSet(rs));
        } catch (SQLException e) {
            System.err.println("[DAO] Error al listar revisiones: " + e.getMessage());
        }
        return lista;
    }

    /** Las revisiones son registros históricos inmutables. */
    @Override
    public boolean actualizar(Revision revision) {
        System.err.println("[DAO] Las revisiones históricas no pueden modificarse.");
        return false;
    }

    /** Las revisiones son registros históricos inmutables. */
    @Override
    public boolean borrar(Integer codRevision) {
        System.err.println("[DAO] Las revisiones históricas no pueden eliminarse.");
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSULTAS ESPECÍFICAS DE NEGOCIO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Recupera el historial completo de revisiones de una atracción,
     * ordenadas de la más reciente a la más antigua.
     *
     * @param codAtraccion código de la atracción
     * @return lista de revisiones históricas
     */
    public List<Revision> buscarPorAtraccion(int codAtraccion) {
        final String SQL = """
            SELECT * FROM REVISION
            WHERE cod_atraccion = ?
            ORDER BY fecha DESC
            """;
        List<Revision> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codAtraccion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar revisiones de atracción: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Recupera la revisión más reciente de una atracción concreta.
     * Permite saber de un vistazo el estado actual de la atracción.
     *
     * @param codAtraccion código de la atracción
     * @return la revisión más reciente, o null si no tiene ninguna
     */
    public Revision buscarUltimaRevision(int codAtraccion) {
        final String SQL = """
            SELECT * FROM REVISION
            WHERE cod_atraccion = ?
            ORDER BY fecha DESC
            LIMIT 1
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codAtraccion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar última revisión: " + e.getMessage());
        }
        return null;
    }

    /**
     * Recupera las revisiones con resultado no apto registradas entre dos fechas.
     * Útil para el informe de atracciones clausuradas o con incidencias.
     *
     * @param desde fecha de inicio del rango
     * @param hasta fecha de fin del rango
     * @return lista de revisiones no aptas en el período
     */
    public List<Revision> buscarNoAptasEnPeriodo(LocalDate desde, LocalDate hasta) {
        final String SQL = """
            SELECT * FROM REVISION
            WHERE es_apta = 0
              AND fecha BETWEEN ? AND ?
            ORDER BY fecha DESC
            """;
        List<Revision> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar revisiones no aptas: " + e.getMessage());
        }
        return lista;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MAPEO ResultSet → Revision
    // ════════════════════════════════════════════════════════════════════════

    private Revision mapearResultSet(ResultSet rs) throws SQLException {
        Revision r = new Revision();
        r.setCodRevision(rs.getInt("cod_revision"));
        r.setCodAtraccion(rs.getInt("cod_atraccion"));
        r.setCodEmpleado(rs.getInt("cod_empleado"));

        Date fechaSql = rs.getDate("fecha");
        if (fechaSql != null) r.setFecha(fechaSql.toLocalDate());

        r.setEsApta(rs.getBoolean("es_apta"));
        r.setPiezasCambiar(rs.getString("piezas_cambiar"));

        Date proximaSql = rs.getDate("proxima_revision");
        if (proximaSql != null) r.setProximaRevision(proximaSql.toLocalDate());

        return r;
    }
}
