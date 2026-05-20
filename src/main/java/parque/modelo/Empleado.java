package parque.modelo;

import java.time.LocalDate;

/**
 * Clase POJO que representa la tabla EMPLEADO de la base de datos.
 * Contiene los atributos del empleado y sus getters/setters.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class Empleado {

    // ── Atributos (corresponden 1:1 con las columnas de la tabla EMPLEADO) ──
    private int    codEmpleado;
    private String nombre;
    private String apellidos;
    private String dni;
    private String numSS;
    private String categoria;       // Código: JA, CA, SE, AN, DP, ME, LI, EA, CJ
    private LocalDate fechaNacimiento;
    private String domicilio;
    private String telefono;
    private String estadoCivil;
    private int    numHijos;

    // ── Constructor vacío (necesario para JDBC) ──────────────────────────────
    public Empleado() {}

    // ── Constructor completo ─────────────────────────────────────────────────
    public Empleado(int codEmpleado, String nombre, String apellidos,
                    String dni, String numSS, String categoria,
                    LocalDate fechaNacimiento, String domicilio,
                    String telefono, String estadoCivil, int numHijos) {
        this.codEmpleado      = codEmpleado;
        this.nombre           = nombre;
        this.apellidos        = apellidos;
        this.dni              = dni;
        this.numSS            = numSS;
        this.categoria        = categoria;
        this.fechaNacimiento  = fechaNacimiento;
        this.domicilio        = domicilio;
        this.telefono         = telefono;
        this.estadoCivil      = estadoCivil;
        this.numHijos         = numHijos;
    }

    // ── Constructor para alta (sin codEmpleado — lo genera AUTO_INCREMENT) ───
    public Empleado(String nombre, String apellidos, String dni, String numSS,
                    String categoria, LocalDate fechaNacimiento, String domicilio,
                    String telefono, String estadoCivil, int numHijos) {
        this(0, nombre, apellidos, dni, numSS, categoria,
             fechaNacimiento, domicilio, telefono, estadoCivil, numHijos);
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int       getCodEmpleado()     { return codEmpleado; }
    public String    getNombre()          { return nombre; }
    public String    getApellidos()       { return apellidos; }
    public String    getNombreCompleto()  { return nombre + " " + apellidos; }
    public String    getDni()             { return dni; }
    public String    getNumSS()           { return numSS; }
    public String    getCategoria()       { return categoria; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String    getDomicilio()       { return domicilio; }
    public String    getTelefono()        { return telefono; }
    public String    getEstadoCivil()     { return estadoCivil; }
    public int       getNumHijos()        { return numHijos; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setCodEmpleado(int codEmpleado)           { this.codEmpleado     = codEmpleado; }
    public void setNombre(String nombre)                  { this.nombre          = nombre; }
    public void setApellidos(String apellidos)            { this.apellidos       = apellidos; }
    public void setDni(String dni)                        { this.dni             = dni; }
    public void setNumSS(String numSS)                    { this.numSS           = numSS; }
    public void setCategoria(String categoria)            { this.categoria       = categoria; }
    public void setFechaNacimiento(LocalDate f)           { this.fechaNacimiento = f; }
    public void setDomicilio(String domicilio)            { this.domicilio       = domicilio; }
    public void setTelefono(String telefono)              { this.telefono        = telefono; }
    public void setEstadoCivil(String estadoCivil)        { this.estadoCivil     = estadoCivil; }
    public void setNumHijos(int numHijos)                 { this.numHijos        = numHijos; }

    // ── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
            "Empleado[cod=%d | %s | DNI: %s | Cat: %s | Tel: %s]",
            codEmpleado, getNombreCompleto(), dni, categoria, telefono
        );
    }
}
