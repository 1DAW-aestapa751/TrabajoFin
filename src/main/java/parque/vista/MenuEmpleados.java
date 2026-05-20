package parque.vista;

import parque.controlador.EmpleadoController;
import parque.modelo.Empleado;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Vista de consola para la gestión de empleados.
 *
 * Muestra menús, recoge entradas del usuario y delega toda la
 * lógica al EmpleadoController. La vista no accede nunca al DAO
 * directamente (separación de responsabilidades).
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class MenuEmpleados {

    private final EmpleadoController controller;
    private final Scanner scanner;

    public MenuEmpleados(Scanner scanner) {
        this.controller = new EmpleadoController();
        this.scanner    = scanner;
    }

    /**
     * Muestra el menú principal de empleados y gestiona la navegación.
     */
    public void mostrar() {
        boolean salir = false;
        while (!salir) {
            imprimirMenu();
            int opcion = leerEntero("Selecciona una opción: ");

            switch (opcion) {
                case 1 -> altaEmpleado();
                case 2 -> consultarEmpleado();
                case 3 -> listarEmpleados();
                case 4 -> buscarPorNombre();
                case 5 -> listarPorCategoria();
                case 6 -> modificarEmpleado();
                case 7 -> bajaEmpleado();
                case 0 -> salir = true;
                default -> System.out.println("Opción no válida. Inténtalo de nuevo.");
            }
        }
    }

    // ── Opciones del menú ────────────────────────────────────────────────────

    private void altaEmpleado() {
        System.out.println("\n── ALTA DE EMPLEADO ──────────────────────────");
        String nombre    = leerTexto("Nombre: ");
        String apellidos = leerTexto("Apellidos: ");
        String dni       = leerTexto("DNI (p. ej. 12345678A): ");
        String numSS     = leerTexto("N.º Seguridad Social: ");

        System.out.println("Categorías: JA CA SE AN DP ME LI EA CJ");
        String categoria = leerTexto("Categoría: ").toUpperCase();

        LocalDate fechaNac = leerFecha("Fecha nacimiento (AAAA-MM-DD): ");
        if (fechaNac == null) return;

        String domicilio   = leerTexto("Domicilio: ");
        String telefono    = leerTexto("Teléfono (Enter para omitir): ");
        String estadoCivil = leerTexto("Estado civil (soltero/casado/divorciado/viudo + o/a): ");
        int    numHijos    = leerEntero("Número de hijos: ");

        Empleado nuevo = controller.altaEmpleado(
            nombre, apellidos, dni, numSS, categoria,
            fechaNac, domicilio,
            telefono.isBlank() ? null : telefono,
            estadoCivil.isBlank() ? null : estadoCivil,
            numHijos
        );

        if (nuevo != null) {
            System.out.println("\n✓ Empleado dado de alta correctamente.");
            System.out.println("  Código asignado: " + nuevo.getCodEmpleado());
        } else {
            System.out.println("\n✗ No se pudo registrar el empleado. Revisa los datos.");
        }
    }

    private void consultarEmpleado() {
        System.out.println("\n── CONSULTAR EMPLEADO ────────────────────────");
        int cod = leerEntero("Código de empleado: ");
        controller.mostrarFichaEmpleado(cod);
    }

    private void listarEmpleados() {
        System.out.println("\n── LISTADO DE EMPLEADOS ──────────────────────");
        List<Empleado> lista = controller.listarEmpleados();
        if (lista.isEmpty()) {
            System.out.println("No hay empleados registrados.");
            return;
        }
        System.out.printf("%-6s %-30s %-12s %-5s%n",
            "Cód", "Nombre completo", "DNI", "Cat.");
        System.out.println("─".repeat(58));
        lista.forEach(e -> System.out.printf("%-6d %-30s %-12s %-5s%n",
            e.getCodEmpleado(), e.getNombreCompleto(), e.getDni(), e.getCategoria()));
        System.out.printf("\nTotal: %d empleados.%n", lista.size());
    }

    private void buscarPorNombre() {
        System.out.println("\n── BUSCAR POR NOMBRE ─────────────────────────");
        String texto = leerTexto("Nombre o apellido a buscar: ");
        List<Empleado> resultado = controller.buscarPorNombre(texto);
        if (resultado.isEmpty()) {
            System.out.println("Sin resultados para: " + texto);
        } else {
            resultado.forEach(e -> System.out.println("  → " + e));
        }
    }

    private void listarPorCategoria() {
        System.out.println("\n── FILTRAR POR CATEGORÍA ─────────────────────");
        System.out.println("  JA=Jardinero  CA=Catering  SE=Seguridad");
        System.out.println("  AN=Animador   DP=Dependiente  ME=Mecánico");
        System.out.println("  LI=Limpieza   EA=Emp.Atracción  CJ=Taquillera");
        String cat = leerTexto("Código de categoría: ").toUpperCase();
        List<Empleado> lista = controller.listarPorCategoria(cat);
        if (lista.isEmpty()) {
            System.out.println("No hay empleados en la categoría " + cat);
        } else {
            lista.forEach(e -> System.out.println("  → " + e));
        }
    }

    private void modificarEmpleado() {
        System.out.println("\n── MODIFICAR EMPLEADO ────────────────────────");
        int cod = leerEntero("Código del empleado a modificar: ");
        Empleado existente = controller.obtenerEmpleado(cod);
        if (existente == null) return;

        System.out.println("(Enter para mantener el valor actual)");
        System.out.println("Nombre actual: " + existente.getNombre());
        String nombre = leerTexto("Nuevo nombre: ");
        if (!nombre.isBlank()) existente.setNombre(nombre.trim());

        System.out.println("Apellidos actuales: " + existente.getApellidos());
        String apellidos = leerTexto("Nuevos apellidos: ");
        if (!apellidos.isBlank()) existente.setApellidos(apellidos.trim());

        System.out.println("Domicilio actual: " + existente.getDomicilio());
        String domicilio = leerTexto("Nuevo domicilio: ");
        if (!domicilio.isBlank()) existente.setDomicilio(domicilio.trim());

        System.out.println("Teléfono actual: " + existente.getTelefono());
        String telefono = leerTexto("Nuevo teléfono: ");
        if (!telefono.isBlank()) existente.setTelefono(telefono.trim());

        boolean ok = controller.actualizarEmpleado(existente);
        System.out.println(ok ? "\n✓ Empleado actualizado." : "\n✗ No se pudo actualizar.");
    }

    private void bajaEmpleado() {
        System.out.println("\n── BAJA DE EMPLEADO ──────────────────────────");
        int cod = leerEntero("Código del empleado a dar de baja: ");
        Empleado e = controller.obtenerEmpleado(cod);
        if (e == null) return;

        System.out.println("¿Seguro que deseas eliminar a " + e.getNombreCompleto() + "? (s/n)");
        String conf = scanner.nextLine().trim().toLowerCase();
        if (!conf.equals("s")) { System.out.println("Operación cancelada."); return; }

        boolean ok = controller.bajaEmpleado(cod);
        System.out.println(ok
            ? "\n✓ Empleado dado de baja correctamente."
            : "\n✗ No se pudo dar de baja. ¿Tiene registros dependientes?");
    }

    // ── Helpers de entrada ───────────────────────────────────────────────────

    private void imprimirMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      GESTIÓN DE EMPLEADOS            ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Alta de empleado                 ║");
        System.out.println("║  2. Consultar empleado               ║");
        System.out.println("║  3. Listar todos los empleados       ║");
        System.out.println("║  4. Buscar por nombre/apellido       ║");
        System.out.println("║  5. Filtrar por categoría            ║");
        System.out.println("║  6. Modificar empleado               ║");
        System.out.println("║  7. Baja de empleado                 ║");
        System.out.println("║  0. Volver al menú principal         ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private String leerTexto(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private int leerEntero(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Por favor, introduce un número entero.");
            }
        }
    }

    private LocalDate leerFecha(String prompt) {
        System.out.print(prompt);
        try {
            return LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.err.println("Formato de fecha inválido. Usa AAAA-MM-DD.");
            return null;
        }
    }
}
