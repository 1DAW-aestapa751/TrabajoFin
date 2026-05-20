package parque.dao;

import parque.db.Conexion;
import parque.modelo.Nomina;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la entidad Nomina.
 *
 * Gestiona todas las operaciones SQL contra la tabla NOMINA:
 * inserción de nóminas calculadas, consultas históricas por empleado
 * y verificación de duplicados por período.
 *
 * Nota sobre tipos numéricos:
 *   Usamos BigDecimal en Java ↔ DECIMAL(10,2) en MySQL.
 *   NUNCA double para importes monetarios — los errores de
 *   punto flotante son inaceptables en una nómina.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class NominaDAO implements IDao<Nomina, Integer> {

    private final Connection conexion;

    public NominaDAO() {
        this.conexion = Conexion.getInstancia().getConnection();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CRUD BÁSICO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * INSERT — Persiste una nómina ya calculada en la base de datos.
     * El codNomina es AUTO_INCREMENT; se asigna al objeto tras la inserción.
     *
     * @param nomina objeto Nomina con todos los campos calculados
     * @return true si la inserción fue exitosa
     */
    @Override
    public boolean insertar(Nomina nomina) {
        final String SQL = """
            INSERT INTO NOMINA
                (cod_empleado, fecha, salario_base, pagas_extra,
                 comisiones, retencion_irpf, retencion_ss, liquido)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1,            nomina.getCodEmpleado());
            ps.setDate(2,           Date.valueOf(nomina.getFecha()));
            ps.setBigDecimal(3,     nomina.getSalarioBase());
            ps.setBigDecimal(4,     nomina.getPagasExtra());
            ps.setBigDecimal(5,     nomina.getComisiones());
            ps.setBigDecimal(6,     nomina.getRetencionIrpf());
            ps.setBigDecimal(7,     nomina.getRetencionSS());
            ps.setBigDecimal(8,     nomina.getLiquido());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) nomina.setCodNomina(claves.getInt(1));
                }
                System.out.printf("[DAO] Nómina insertada con código %d para empleado %d.%n",
                    nomina.getCodNomina(), nomina.getCodEmpleado());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[DAO] Error al insertar nómina: " + e.getMessage());
        }
        return false;
    }

    /**
     * SELECT por PK — Recupera una nómina por su código.
     *
     * @param codNomina PK de la nómina
     * @return objeto Nomina, o null si no existe
     */
    @Override
    public Nomina buscarPorId(Integer codNomina) {
        final String SQL = "SELECT * FROM NOMINA WHERE cod_nomina = ?";

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codNomina);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar nómina: " + e.getMessage());
        }
        return null;
    }

    /** SELECT ALL — Recupera todas las nóminas de la BD. */
    @Override
    public List<Nomina> buscarTodos() {
        final String SQL = "SELECT * FROM NOMINA ORDER BY fecha DESC, cod_empleado";
        List<Nomina> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearResultSet(rs));
        } catch (SQLException e) {
            System.err.println("[DAO] Error al listar nóminas: " + e.getMessage());
        }
        return lista;
    }

    /** UPDATE — Las nóminas históricas no se modifican. Método no implementado. */
    @Override
    public boolean actualizar(Nomina nomina) {
        System.err.println("[DAO] Las nóminas históricas son inmutables y no pueden modificarse.");
        return false;
    }

    /** DELETE — Las nóminas históricas no se eliminan. Método no implementado. */
    @Override
    public boolean borrar(Integer codNomina) {
        System.err.println("[DAO] Las nóminas históricas no pueden eliminarse.");
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSULTAS ESPECÍFICAS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Recupera el historial completo de nóminas de un empleado,
     * ordenadas cronológicamente descendente (la más reciente primero).
     *
     * @param codEmpleado código del empleado
     * @return lista de nóminas históricas
     */
    public List<Nomina> buscarPorEmpleado(int codEmpleado) {
        final String SQL = """
            SELECT * FROM NOMINA
            WHERE cod_empleado = ?
            ORDER BY fecha DESC
            """;
        List<Nomina> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codEmpleado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar nóminas del empleado: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Comprueba si ya existe una nómina para el empleado en el mes indicado.
     * Evita generar nóminas duplicadas para el mismo período.
     *
     * Usamos YEAR() y MONTH() de MySQL para comparar solo el mes/año
     * sin importar el día exacto almacenado en el campo fecha.
     *
     * @param codEmpleado código del empleado
     * @param fecha       cualquier fecha del mes a verificar
     * @return true si ya existe nómina para ese mes
     */
    public boolean existeNominaEnMes(int codEmpleado, LocalDate fecha) {
        final String SQL = """
            SELECT COUNT(*) FROM NOMINA
            WHERE cod_empleado = ?
              AND YEAR(fecha)  = ?
              AND MONTH(fecha) = ?
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codEmpleado);
            ps.setInt(2, fecha.getYear());
            ps.setInt(3, fecha.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al verificar nómina existente: " + e.getMessage());
        }
        return false;
    }

    /**
     * Recupera la nómina más reciente de un empleado.
     * Útil para mostrar la última nómina en pantalla.
     *
     * @param codEmpleado código del empleado
     * @return la nómina más reciente, o null si no tiene ninguna
     */
    public Nomina buscarUltimaDeEmpleado(int codEmpleado) {
        final String SQL = """
            SELECT * FROM NOMINA
            WHERE cod_empleado = ?
            ORDER BY fecha DESC
            LIMIT 1
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codEmpleado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al buscar última nómina: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calcula el líquido total pagado a un empleado en un año natural.
     * Útil para el módulo de facturación (gastos de personal).
     *
     * @param codEmpleado código del empleado
     * @param anyo        año natural (p. ej. 2024)
     * @return suma del líquido de todas las nóminas del año, o ZERO si no hay ninguna
     */
    public BigDecimal calcularLiquidoAnual(int codEmpleado, int anyo) {
        final String SQL = """
            SELECT COALESCE(SUM(liquido), 0) AS total
            FROM NOMINA
            WHERE cod_empleado = ?
              AND YEAR(fecha)  = ?
            """;

        try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
            ps.setInt(1, codEmpleado);
            ps.setInt(2, anyo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Error al calcular líquido anual: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MAPEO ResultSet → Nomina
    // ════════════════════════════════════════════════════════════════════════

    private Nomina mapearResultSet(ResultSet rs) throws SQLException {
        Nomina n = new Nomina();
        n.setCodNomina(rs.getInt("cod_nomina"));
        n.setCodEmpleado(rs.getInt("cod_empleado"));

        Date fechaSql = rs.getDate("fecha");
        if (fechaSql != null) n.setFecha(fechaSql.toLocalDate());

        n.setSalarioBase(rs.getBigDecimal("salario_base"));
        n.setPagasExtra(rs.getBigDecimal("pagas_extra"));
        n.setComisiones(rs.getBigDecimal("comisiones"));
        n.setRetencionIrpf(rs.getBigDecimal("retencion_irpf"));
        n.setRetencionSS(rs.getBigDecimal("retencion_ss"));
        n.setLiquido(rs.getBigDecimal("liquido"));
        return n;
    }
}
