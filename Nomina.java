package parque.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * POJO que representa una fila de la tabla NOMINA.
 *
 * Contiene todos los conceptos retributivos del período:
 * sueldo base, extras, comisiones, retenciones y líquido final.
 *
 * Se usa BigDecimal (no double) para los importes monetarios,
 * evitando errores de redondeo en los cálculos de nómina.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class Nomina {

    private int        codNomina;
    private int        codEmpleado;
    private LocalDate  fecha;
    private BigDecimal salarioBase;
    private BigDecimal pagasExtra;
    private BigDecimal comisiones;
    private BigDecimal retencionIrpf;
    private BigDecimal retencionSS;
    private BigDecimal liquido;

    // ── Constructores ────────────────────────────────────────────────────────

    public Nomina() {}

    /** Constructor completo — para mapeo desde ResultSet. */
    public Nomina(int codNomina, int codEmpleado, LocalDate fecha,
                  BigDecimal salarioBase, BigDecimal pagasExtra, BigDecimal comisiones,
                  BigDecimal retencionIrpf, BigDecimal retencionSS, BigDecimal liquido) {
        this.codNomina     = codNomina;
        this.codEmpleado   = codEmpleado;
        this.fecha         = fecha;
        this.salarioBase   = salarioBase;
        this.pagasExtra    = pagasExtra;
        this.comisiones    = comisiones;
        this.retencionIrpf = retencionIrpf;
        this.retencionSS   = retencionSS;
        this.liquido       = liquido;
    }

    /** Constructor para alta (sin codNomina — lo genera AUTO_INCREMENT). */
    public Nomina(int codEmpleado, LocalDate fecha,
                  BigDecimal salarioBase, BigDecimal pagasExtra, BigDecimal comisiones,
                  BigDecimal retencionIrpf, BigDecimal retencionSS, BigDecimal liquido) {
        this(0, codEmpleado, fecha, salarioBase, pagasExtra,
             comisiones, retencionIrpf, retencionSS, liquido);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int        getCodNomina()     { return codNomina; }
    public int        getCodEmpleado()   { return codEmpleado; }
    public LocalDate  getFecha()         { return fecha; }
    public BigDecimal getSalarioBase()   { return salarioBase; }
    public BigDecimal getPagasExtra()    { return pagasExtra; }
    public BigDecimal getComisiones()    { return comisiones; }
    public BigDecimal getRetencionIrpf() { return retencionIrpf; }
    public BigDecimal getRetencionSS()   { return retencionSS; }
    public BigDecimal getLiquido()       { return liquido; }

    public void setCodNomina(int codNomina)             { this.codNomina     = codNomina; }
    public void setCodEmpleado(int codEmpleado)         { this.codEmpleado   = codEmpleado; }
    public void setFecha(LocalDate fecha)               { this.fecha         = fecha; }
    public void setSalarioBase(BigDecimal salarioBase)  { this.salarioBase   = salarioBase; }
    public void setPagasExtra(BigDecimal pagasExtra)    { this.pagasExtra    = pagasExtra; }
    public void setComisiones(BigDecimal comisiones)    { this.comisiones    = comisiones; }
    public void setRetencionIrpf(BigDecimal r)          { this.retencionIrpf = r; }
    public void setRetencionSS(BigDecimal r)            { this.retencionSS   = r; }
    public void setLiquido(BigDecimal liquido)          { this.liquido       = liquido; }

    /** Calcula y devuelve el bruto total (base + extras + comisiones). */
    public BigDecimal getBrutoTotal() {
        return salarioBase
               .add(pagasExtra   != null ? pagasExtra   : BigDecimal.ZERO)
               .add(comisiones   != null ? comisiones   : BigDecimal.ZERO);
    }

    /** Calcula y devuelve el total de retenciones. */
    public BigDecimal getTotalRetenciones() {
        return (retencionIrpf != null ? retencionIrpf : BigDecimal.ZERO)
               .add(retencionSS != null ? retencionSS : BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        return String.format(
            "Nómina[cod=%d | emp=%d | %s | Bruto=%.2f€ | Retenc=%.2f€ | Líq=%.2f€]",
            codNomina, codEmpleado,
            fecha != null ? fecha.format(fmt) : "—",
            getBrutoTotal(),
            getTotalRetenciones(),
            liquido
        );
    }
}
