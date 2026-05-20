package parque.dao;

import java.util.List;

/**
 * Interfaz genérica DAO (Data Access Object).
 *
 * Define el contrato CRUD que deben implementar todos los DAOs del sistema.
 * Usando genéricos, una sola interfaz sirve para cualquier entidad.
 *
 * @param <T>  Tipo de la entidad (Empleado, Atraccion, Pedido…)
 * @param <ID> Tipo de la clave primaria (normalmente Integer)
 *
 * Proyecto Final 1º DAW — Sistema de Gestión Parque de Atracciones
 */
public interface IDao<T, ID> {

    /**
     * Inserta un nuevo registro en la base de datos.
     * @param entidad objeto a insertar
     * @return true si la inserción fue exitosa
     */
    boolean insertar(T entidad);

    /**
     * Busca un registro por su clave primaria.
     * @param id clave primaria del registro
     * @return la entidad encontrada, o null si no existe
     */
    T buscarPorId(ID id);

    /**
     * Recupera todos los registros de la tabla.
     * @return lista con todas las entidades
     */
    List<T> buscarTodos();

    /**
     * Actualiza un registro existente en la base de datos.
     * @param entidad objeto con los datos actualizados (debe tener el id correcto)
     * @return true si la actualización afectó al menos una fila
     */
    boolean actualizar(T entidad);

    /**
     * Elimina un registro por su clave primaria.
     * @param id clave primaria del registro a eliminar
     * @return true si el borrado afectó al menos una fila
     */
    boolean borrar(ID id);
}
