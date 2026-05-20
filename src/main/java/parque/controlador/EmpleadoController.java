package parque.controlador;

import parque.dao.EmpleadoDAO;
import parque.modelo.Empleado;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Controlador de la entidad Empleado.
 *
 * Actúa como capa intermedia entre la Vista y el DAO.
 * Aquí reside la lógica de negocio: validaciones, reglas del enunciado
 * y coordinación de operaciones complejas.
 *
 * La Vista llama a los métodos de este controlador;
 * el controlador llama al DAO para persistir o recuperar datos.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class EmpleadoController {

    private final EmpleadoDAO empleadoDAO;

    // Categorías válidas según el enunciado del proyecto
    private static final List<String> CATEGORIAS_VALIDAS =
        List.of("JA", "CA", "SE", "AN", "DP", "ME", "LI", "EA", "CJ");

    public EmpleadoController() {
        this.empleadoDAO = new EmpleadoDAO();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  OPERACIONES CRUD CON LÓGICA DE NEGOCIO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Da de alta a un nuevo empleado.
     *
     * Validaciones aplicadas:
     *  1. El DNI no debe estar ya registrado.
     *  2. La categoría debe ser un código válido.
     *  3. La fecha de nacimiento implica edad mínima de 16 años.
     *  4. El número de hijos no puede ser negativo.
     *
     * @return el empleado insertado con su código asignado, o null si falla
     */
    public Empleado altaEmpleado(String nombre, String apellidos, String dni,
                                  String numSS, String categoria,
                                  LocalDate fechaNacimiento, String domicilio,
                                  String telefono, String estadoCivil, int numHijos) {
        // Validación 1: DNI duplicado
        if (empleadoDAO.existeDni(dni)) {
            System.err.println("[Controller] Alta rechazada: el DNI " + dni + " ya está registrado.");
            return null;
        }

        // Validación 2: categoría válida
        if (!CATEGORIAS_VALIDAS.contains(categoria.toUpperCase())) {
            System.err.println("[Controller] Alta rechazada: categoría '" + categoria + "' no válida.");
            System.err.println("             Categorías permitidas: " + CATEGORIAS_VALIDAS);
            return null;
        }

        // Validación 3: edad mínima 16 años
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 16) {
            System.err.println("[Controller] Alta rechazada: el empleado debe tener al menos 16 años.");
            return null;
        }

        // Validación 4: hijos no negativos
        if (numHijos < 0) {
            System.err.println("[Controller] Alta rechazada: el número de hijos no puede ser negativo.");
            return null;
        }

        Empleado nuevo = new Empleado(
            nombre.trim(), apellidos.trim(), dni.toUpperCase().trim(),
            numSS.trim(), categoria.toUpperCase().trim(), fechaNacimiento,
            domicilio.trim(), telefono, estadoCivil, numHijos
        );

        boolean ok = empleadoDAO.insertar(nuevo);
        return ok ? nuevo : null;
    }

    /**
     * Busca un empleado por código.
     *
     * @param codEmpleado código del empleado
     * @return el empleado o null si no existe
     */
    public Empleado obtenerEmpleado(int codEmpleado) {
        if (codEmpleado <= 0) {
            System.err.println("[Controller] Código de empleado inválido.");
            return null;
        }
        Empleado e = empleadoDAO.buscarPorId(codEmpleado);
        if (e == null) System.err.println("[Controller] No existe empleado con código " + codEmpleado);
        return e;
    }

    /**
     * Devuelve la lista completa de empleados.
     */
    public List<Empleado> listarEmpleados() {
        return empleadoDAO.buscarTodos();
    }

    /**
     * Devuelve empleados filtrados por categoría profesional.
     *
     * @param categoria código de categoría (ME, JA, CA…)
     */
    public List<Empleado> listarPorCategoria(String categoria) {
        if (!CATEGORIAS_VALIDAS.contains(categoria.toUpperCase())) {
            System.err.println("[Controller] Categoría '" + categoria + "' no reconocida.");
            return List.of();
        }
        return empleadoDAO.buscarPorCategoria(categoria.toUpperCase());
    }

    /**
     * Busca empleados por fragmento de nombre o apellido.
     *
     * @param texto texto a buscar
     */
    public List<Empleado> buscarPorNombre(String texto) {
        if (texto == null || texto.isBlank()) {
            System.err.println("[Controller] El texto de búsqueda no puede estar vacío.");
            return List.of();
        }
        return empleadoDAO.buscarPorNombre(texto);
    }

    /**
     * Actualiza los datos de contacto y personales de un empleado.
     * El DNI y N.º SS son inmutables una vez registrados.
     *
     * @return true si la actualización fue exitosa
     */
    public boolean actualizarEmpleado(Empleado empleado) {
        if (empleado == null || empleado.getCodEmpleado() <= 0) {
            System.err.println("[Controller] Empleado inválido para actualización.");
            return false;
        }

        // Verificar que el empleado existe antes de actualizar
        if (empleadoDAO.buscarPorId(empleado.getCodEmpleado()) == null) {
            System.err.println("[Controller] No se puede actualizar: empleado no encontrado.");
            return false;
        }

        return empleadoDAO.actualizar(empleado);
    }

    /**
     * Da de baja a un empleado.
     *
     * Advertencia: MySQL impedirá el borrado (RESTRICT) si el empleado
     * tiene nóminas, revisiones o pedidos asociados. En ese caso la
     * operación fallará con un mensaje descriptivo.
     *
     * @param codEmpleado código del empleado a eliminar
     * @return true si la baja fue exitosa
     */
    public boolean bajaEmpleado(int codEmpleado) {
        if (empleadoDAO.buscarPorId(codEmpleado) == null) {
            System.err.println("[Controller] No existe empleado con código " + codEmpleado);
            return false;
        }
        return empleadoDAO.borrar(codEmpleado);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LÓGICA DE NEGOCIO ESPECÍFICA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Determina el empleado responsable de emitir el pedido cuando el stock
     * de un producto en un almacén baja del 40% del mínimo.
     *
     * Regla del enunciado: se notifica al empleado de mayor categoría
     * (según sueldo_base) asociado al almacén correspondiente.
     *
     * @param codAlmacen código del almacén con stock bajo
     * @return el empleado responsable, o null si no hay encargado asignado
     */
    public Empleado obtenerResponsablePedido(int codAlmacen) {
        Empleado responsable = empleadoDAO.buscarEncargadoMayorCategoria(codAlmacen);
        if (responsable == null) {
            System.err.println("[Controller] No hay encargado asignado al almacén " + codAlmacen);
        } else {
            System.out.printf("[Controller] Responsable de pedido: %s (Categoría: %s)%n",
                responsable.getNombreCompleto(), responsable.getCategoria());
        }
        return responsable;
    }

    /**
     * Calcula la edad actual de un empleado a partir de su fecha de nacimiento.
     *
     * @param codEmpleado código del empleado
     * @return edad en años, o -1 si el empleado no existe
     */
    public int calcularEdad(int codEmpleado) {
        Empleado e = empleadoDAO.buscarPorId(codEmpleado);
        if (e == null) return -1;
        return Period.between(e.getFechaNacimiento(), LocalDate.now()).getYears();
    }

    /**
     * Devuelve el total de empleados registrados en el sistema.
     */
    public int totalEmpleados() {
        return empleadoDAO.contarEmpleados();
    }

    /**
     * Imprime en consola una ficha formateada del empleado.
     * Útil durante el desarrollo o para informes rápidos en modo texto.
     *
     * @param codEmpleado código del empleado
     */
    public void mostrarFichaEmpleado(int codEmpleado) {
        Empleado e = empleadoDAO.buscarPorId(codEmpleado);
        if (e == null) {
            System.err.println("[Controller] Empleado no encontrado.");
            return;
        }
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.printf( "║  FICHA DE EMPLEADO  —  Cód: %-4d            ║%n", e.getCodEmpleado());
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf( "║  Nombre:    %-32s ║%n", e.getNombreCompleto());
        System.out.printf( "║  DNI:       %-32s ║%n", e.getDni());
        System.out.printf( "║  N.º S.S.:  %-32s ║%n", e.getNumSS());
        System.out.printf( "║  Categoría: %-32s ║%n", e.getCategoria());
        System.out.printf( "║  Nacimiento:%-32s ║%n", e.getFechaNacimiento());
        System.out.printf( "║  Edad:      %-32d ║%n", calcularEdad(codEmpleado));
        System.out.printf( "║  Domicilio: %-32s ║%n", e.getDomicilio());
        System.out.printf( "║  Teléfono:  %-32s ║%n", e.getTelefono() != null ? e.getTelefono() : "—");
        System.out.printf( "║  Estado:    %-32s ║%n", e.getEstadoCivil() != null ? e.getEstadoCivil() : "—");
        System.out.printf( "║  Hijos:     %-32d ║%n", e.getNumHijos());
        System.out.println("╚══════════════════════════════════════════════╝");
    }
}
