package parque.controlador;

import parque.dao.EmpleadoDAO;
import parque.dao.NominaDAO;
import parque.modelo.Empleado;
import parque.modelo.Nomina;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del módulo de Nóminas.
 *
 * Implementa la lógica completa de cálculo de nómina del parque:
 *   1. Validar que el empleado existe.
 *   2. Verificar que no existe ya nómina para ese mes.
 *   3. Obtener el sueldo base de la tabla CATEGORIA.
 *   4. Sumar conceptos adicionales (pagas extra, comisiones).
 *   5. Calcular las retenciones (IRPF y cuota SS obrera).
 *   6. Calcular el líquido neto.
 *   7. Persistir la nómina en BD mediante NominaDAO.
 *
 * Los tipos de retención y los tramos son simplificados para el proyecto;
 * en un sistema real se consultarían las tablas AEAT vigentes.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class NominaController {

    // ── DAOs ─────────────────────────────────────────────────────────────────
    private final NominaDAO   nominaDAO;
    private final EmpleadoDAO empleadoDAO;

    // ── Tipos de retención (porcentajes sobre bruto) ─────────────────────────
    //    Valores orientativos simplificados para el proyecto académico.
    //    Fuente real: Ley IRPF y Reglamento SS vigentes.

    /** Porcentaje de cuota obrera a la Seguridad Social (contingencias comunes). */
    private static final BigDecimal PCT_SS = new BigDecimal("0.0635");  // 6,35%

    /**
     * Tramos IRPF simplificados (bruto mensual → tipo aplicado).
     * En la realidad son tramos anuales; aquí los aproximamos mensualmente.
     */
    private static final BigDecimal IRPF_TRAMO_1 = new BigDecimal("0.09");   // < 1.500€/mes
    private static final BigDecimal IRPF_TRAMO_2 = new BigDecimal("0.14");   // 1.500–2.000€/mes
    private static final BigDecimal IRPF_TRAMO_3 = new BigDecimal("0.20");   // > 2.000€/mes

    private static final BigDecimal LIMITE_TRAMO_1 = new BigDecimal("1500.00");
    private static final BigDecimal LIMITE_TRAMO_2 = new BigDecimal("2000.00");

    /** Cuantía de cada paga extraordinaria (se añaden en junio y diciembre). */
    private static final int MES_PAGA_EXTRA_1 = 6;   // Junio
    private static final int MES_PAGA_EXTRA_2 = 12;  // Diciembre

    // ── Constructor ──────────────────────────────────────────────────────────

    public NominaController() {
        this.nominaDAO   = new NominaDAO();
        this.empleadoDAO = new EmpleadoDAO();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  OPERACIÓN PRINCIPAL — CALCULAR Y REGISTRAR NÓMINA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calcula y registra en BD la nómina mensual de un empleado.
     *
     * El método coordina todas las validaciones y cálculos; el DAO
     * solo se ocupa de persistir el objeto Nomina ya construido.
     *
     * @param codEmpleado  código del empleado
     * @param mes          fecha del período (se usa el año y mes; el día es irrelevante)
     * @param comisiones   comisiones del período (puede ser 0)
     * @return objeto Nomina con todos los campos calculados y persistido en BD,
     *         o null si falla alguna validación
     */
    public Nomina calcularYRegistrarNomina(int codEmpleado,
                                           LocalDate mes,
                                           BigDecimal comisiones) {
        // ── Paso 1: Validar que el empleado existe ───────────────────────────
        Empleado empleado = empleadoDAO.buscarPorId(codEmpleado);
        if (empleado == null) {
            System.err.printf("[NominaCtrl] ERROR: No existe el empleado con código %d.%n", codEmpleado);
            return null;
        }

        // ── Paso 2: Verificar nómina no duplicada ────────────────────────────
        if (nominaDAO.existeNominaEnMes(codEmpleado, mes)) {
            System.err.printf("[NominaCtrl] ERROR: Ya existe nómina para el empleado %d en %02d/%d.%n",
                codEmpleado, mes.getMonthValue(), mes.getYear());
            return null;
        }

        // ── Paso 3: Obtener sueldo base de la categoría ──────────────────────
        BigDecimal sueldoBase = obtenerSueldoBaseCategoria(empleado.getCategoria());
        if (sueldoBase == null) {
            System.err.printf("[NominaCtrl] ERROR: Categoría '%s' no encontrada en BD.%n",
                empleado.getCategoria());
            return null;
        }

        // ── Paso 4: Calcular pagas extra ─────────────────────────────────────
        //    En junio y diciembre se añade una paga extra = 1 × sueldo_base.
        BigDecimal pagasExtra = calcularPagasExtra(sueldoBase, mes);

        // ── Paso 5: Calcular bruto total ─────────────────────────────────────
        BigDecimal comisionesVal = comisiones != null ? comisiones : BigDecimal.ZERO;
        BigDecimal bruto = sueldoBase
                          .add(pagasExtra)
                          .add(comisionesVal);

        // ── Paso 6: Calcular retenciones ─────────────────────────────────────
        BigDecimal retencionIrpf = calcularRetencionIrpf(bruto, empleado);
        BigDecimal retencionSS   = calcularRetencionSS(bruto);

        // ── Paso 7: Calcular líquido neto ────────────────────────────────────
        BigDecimal liquido = bruto
                            .subtract(retencionIrpf)
                            .subtract(retencionSS)
                            .setScale(2, RoundingMode.HALF_UP);

        // ── Paso 8: Construir el objeto Nomina ───────────────────────────────
        Nomina nomina = new Nomina(
            codEmpleado,
            mes.withDayOfMonth(mes.lengthOfMonth()),  // último día del mes como fecha
            sueldoBase,
            pagasExtra,
            comisionesVal,
            retencionIrpf,
            retencionSS,
            liquido
        );

        // ── Paso 9: Persistir en BD ──────────────────────────────────────────
        boolean ok = nominaDAO.insertar(nomina);
        if (!ok) {
            System.err.println("[NominaCtrl] ERROR: No se pudo insertar la nómina en la BD.");
            return null;
        }

        // ── Paso 10: Imprimir resumen en consola ─────────────────────────────
        imprimirResumenNomina(empleado, nomina);
        return nomina;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Devuelve el historial completo de nóminas de un empleado.
     *
     * @param codEmpleado código del empleado
     * @return lista de nóminas (vacía si no tiene ninguna)
     */
    public List<Nomina> historialNominas(int codEmpleado) {
        if (empleadoDAO.buscarPorId(codEmpleado) == null) {
            System.err.println("[NominaCtrl] Empleado no encontrado.");
            return List.of();
        }
        return nominaDAO.buscarPorEmpleado(codEmpleado);
    }

    /**
     * Devuelve la última nómina de un empleado.
     *
     * @param codEmpleado código del empleado
     * @return la nómina más reciente, o null
     */
    public Nomina ultimaNomina(int codEmpleado) {
        return nominaDAO.buscarUltimaDeEmpleado(codEmpleado);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTODOS PRIVADOS DE CÁLCULO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Consulta el sueldo base de una categoría directamente en MySQL.
     * Los datos de categoría están normalizados en tabla CATEGORIA,
     * por eso consultamos BD en lugar de usar una constante en Java.
     *
     * La consulta hace un JOIN lógico: dado el código de categoría del
     * empleado, obtenemos su sueldo_base de la tabla CATEGORIA.
     *
     * @param codigoCategoria p. ej. "ME", "JA", "CA"
     * @return sueldo base como BigDecimal, o null si no existe la categoría
     */
    private BigDecimal obtenerSueldoBaseCategoria(String codigoCategoria) {
        final String SQL = "SELECT sueldo_base FROM CATEGORIA WHERE codigo = ?";

        try (java.sql.PreparedStatement ps =
                 parque.db.Conexion.getInstancia().getConnection().prepareStatement(SQL)) {

            ps.setString(1, codigoCategoria);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("sueldo_base");
            }
        } catch (java.sql.SQLException e) {
            System.err.println("[NominaCtrl] Error al obtener sueldo base: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calcula las pagas extraordinarias del mes.
     *
     * Regla aplicada: en junio y diciembre se genera una paga extra
     * equivalente al sueldo base mensual (prorrateo simplificado).
     * Si el mes no es de paga, devuelve ZERO.
     *
     * @param sueldoBase sueldo base del mes
     * @param mes        fecha del período
     * @return importe de pagas extra (puede ser ZERO)
     */
    private BigDecimal calcularPagasExtra(BigDecimal sueldoBase, LocalDate mes) {
        int mesNum = mes.getMonthValue();
        if (mesNum == MES_PAGA_EXTRA_1 || mesNum == MES_PAGA_EXTRA_2) {
            System.out.printf("[NominaCtrl] Mes de paga extra (mes %d): +%.2f€%n",
                mesNum, sueldoBase);
            return sueldoBase;  // 1 paga extra = 1 × sueldo base
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calcula la retención de IRPF aplicando tramos simplificados.
     *
     * El tipo se aplica sobre el bruto total del mes. En la realidad
     * el IRPF se calcula sobre la base anual y se aplica mensualmente;
     * aquí lo simplificamos para el proyecto académico.
     *
     * Tramos aplicados:
     *   ≤ 1.500€/mes → 9%
     *   1.500–2.000€/mes → 14%
     *   > 2.000€/mes → 20%
     *
     * Reducciones aplicadas sobre el tipo base:
     *   -1% por cada hijo a cargo (máximo -4%)
     *
     * @param bruto    importe bruto del período
     * @param empleado empleado (para aplicar reducciones por hijos)
     * @return importe de retención IRPF redondeado a 2 decimales
     */
    private BigDecimal calcularRetencionIrpf(BigDecimal bruto, Empleado empleado) {
        // Determinar tipo base según tramo
        BigDecimal tipo;
        if (bruto.compareTo(LIMITE_TRAMO_1) <= 0) {
            tipo = IRPF_TRAMO_1;
        } else if (bruto.compareTo(LIMITE_TRAMO_2) <= 0) {
            tipo = IRPF_TRAMO_2;
        } else {
            tipo = IRPF_TRAMO_3;
        }

        // Aplicar reducción por hijos (-1% por hijo, máximo -4%)
        int hijos = Math.min(empleado.getNumHijos(), 4);
        if (hijos > 0) {
            BigDecimal reduccion = new BigDecimal(hijos).multiply(new BigDecimal("0.01"));
            tipo = tipo.subtract(reduccion).max(BigDecimal.ZERO);
            System.out.printf("[NominaCtrl] Reducción IRPF por %d hijo/s: -%.0f%%%n", hijos, reduccion.multiply(BigDecimal.valueOf(100)));
        }

        return bruto.multiply(tipo).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula la cuota obrera de la Seguridad Social.
     *
     * Tipo fijo del 6,35% sobre el bruto (contingencias comunes + desempleo
     * + formación profesional — simplificado para el proyecto).
     *
     * @param bruto importe bruto del período
     * @return importe de retención SS redondeado a 2 decimales
     */
    private BigDecimal calcularRetencionSS(BigDecimal bruto) {
        return bruto.multiply(PCT_SS).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Imprime en consola un recibo de nómina formateado.
     *
     * @param empleado empleado al que corresponde la nómina
     * @param nomina   objeto Nomina ya calculado
     */
    private void imprimirResumenNomina(Empleado empleado, Nomina nomina) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.printf( "║  RECIBO DE NÓMINA  —  Cód. %d                    %n", nomina.getCodNomina());
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf( "║  Empleado : %-34s ║%n", empleado.getNombreCompleto());
        System.out.printf( "║  Categoría: %-34s ║%n", empleado.getCategoria());
        System.out.printf( "║  Período  : %-34s ║%n",
            String.format("%02d/%d", nomina.getFecha().getMonthValue(), nomina.getFecha().getYear()));
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf( "║  Sueldo base        : %,10.2f €               ║%n", nomina.getSalarioBase());
        System.out.printf( "║  Pagas extra        : %,10.2f €               ║%n", nomina.getPagasExtra());
        System.out.printf( "║  Comisiones         : %,10.2f €               ║%n", nomina.getComisiones());
        System.out.println("║  ─────────────────────────────────────────────  ║");
        System.out.printf( "║  BRUTO TOTAL        : %,10.2f €               ║%n", nomina.getBrutoTotal());
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf( "║  (-) Retención IRPF : %,10.2f €               ║%n", nomina.getRetencionIrpf());
        System.out.printf( "║  (-) Cuota S.S.     : %,10.2f €               ║%n", nomina.getRetencionSS());
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf( "║  LÍQUIDO A PERCIBIR : %,10.2f €               ║%n", nomina.getLiquido());
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }
}
