package parque.modelo;

import java.time.LocalDate;

/**
 * POJO que representa una fila de la tabla REVISION.
 *
 * Recoge el resultado de cada inspección mecánica mensual:
 * atracción revisada, empleado responsable, fecha, resultado
 * y — en caso de no ser apta — las piezas a sustituir y
 * la fecha de la próxima revisión programada.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class Revision {

    private int       codRevision;
    private int       codAtraccion;
    private int       codEmpleado;
    private LocalDate fecha;
    private boolean   esApta;
    private String    piezasCambiar;    // null si la revisión es apta
    private LocalDate proximaRevision;

    // ── Constructores ────────────────────────────────────────────────────────

    public Revision() {}

    /** Constructor completo — para mapeo desde ResultSet. */
    public Revision(int codRevision, int codAtraccion, int codEmpleado,
                    LocalDate fecha, boolean esApta,
                    String piezasCambiar, LocalDate proximaRevision) {
        this.codRevision    = codRevision;
        this.codAtraccion   = codAtraccion;
        this.codEmpleado    = codEmpleado;
        this.fecha          = fecha;
        this.esApta         = esApta;
        this.piezasCambiar  = piezasCambiar;
        this.proximaRevision = proximaRevision;
    }

    /** Constructor para alta (sin codRevision — lo genera AUTO_INCREMENT). */
    public Revision(int codAtraccion, int codEmpleado, LocalDate fecha,
                    boolean esApta, String piezasCambiar, LocalDate proximaRevision) {
        this(0, codAtraccion, codEmpleado, fecha, esApta, piezasCambiar, proximaRevision);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int       getCodRevision()     { return codRevision; }
    public int       getCodAtraccion()    { return codAtraccion; }
    public int       getCodEmpleado()     { return codEmpleado; }
    public LocalDate getFecha()           { return fecha; }
    public boolean   isEsApta()           { return esApta; }
    public String    getPiezasCambiar()   { return piezasCambiar; }
    public LocalDate getProximaRevision() { return proximaRevision; }

    public void setCodRevision(int codRevision)        { this.codRevision     = codRevision; }
    public void setCodAtraccion(int codAtraccion)      { this.codAtraccion    = codAtraccion; }
    public void setCodEmpleado(int codEmpleado)        { this.codEmpleado     = codEmpleado; }
    public void setFecha(LocalDate fecha)              { this.fecha           = fecha; }
    public void setEsApta(boolean esApta)              { this.esApta          = esApta; }
    public void setPiezasCambiar(String piezas)        { this.piezasCambiar   = piezas; }
    public void setProximaRevision(LocalDate proxima)  { this.proximaRevision = proxima; }

    @Override
    public String toString() {
        return String.format(
            "Revision[cod=%d | atr=%d | emp=%d | %s | %s%s]",
            codRevision, codAtraccion, codEmpleado, fecha,
            esApta ? "APTA" : "NO APTA",
            piezasCambiar != null ? " | Piezas: " + piezasCambiar : ""
        );
    }
}
