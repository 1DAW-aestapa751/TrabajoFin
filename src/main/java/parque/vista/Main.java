package parque.vista;

import parque.db.Conexion;

import java.util.Scanner;

/**
 * Punto de entrada de la aplicación.
 *
 * Muestra el menú principal y gestiona la navegación entre módulos.
 * Al salir cierra la conexión con la base de datos.
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   SISTEMA DE GESTIÓN — PARQUE DE ATRACCIONES   ║");
        System.out.println("║             Proyecto Final 1º DAW               ║");
        System.out.println("╚════════════════════════════════════════════════╝");

        Scanner scanner = new Scanner(System.in);
        boolean salir   = false;

        // Verificar conexión al arrancar
        try {
            Conexion.getInstancia().getConnection();
        } catch (RuntimeException e) {
            System.err.println("No se pudo conectar a la base de datos. Verifica MySQL.");
            System.exit(1);
        }

        while (!salir) {
            imprimirMenuPrincipal();
            System.out.print("Selecciona una opción: ");
            String linea = scanner.nextLine().trim();

            switch (linea) {
                case "1" -> new MenuEmpleados(scanner).mostrar();
                // case "2" -> new MenuAtracciones(scanner).mostrar();
                // case "3" -> new MenuPedidos(scanner).mostrar();
                // case "4" -> new MenuNominas(scanner).mostrar();
                // case "5" -> new MenuEntradas(scanner).mostrar();
                case "0" -> salir = true;
                default  -> System.out.println("Opción no válida.");
            }
        }

        // Cerrar conexión al salir
        Conexion.getInstancia().cerrarConexion();
        System.out.println("\n¡Hasta pronto! Sesión cerrada correctamente.");
        scanner.close();
    }

    private static void imprimirMenuPrincipal() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║         MENÚ PRINCIPAL                ║");
        System.out.println("╠═══════════════════════════════════════╣");
        System.out.println("║  1. Gestión de Empleados              ║");
        System.out.println("║  2. Gestión de Atracciones            ║");
        System.out.println("║  3. Pedidos a Proveedores             ║");
        System.out.println("║  4. Nóminas                           ║");
        System.out.println("║  5. Entradas y Tickets                ║");
        System.out.println("║  0. Salir                             ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }
}
