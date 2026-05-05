package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.azure.messaging.eventgrid.EventGridEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class Function {

        @FunctionName("procesarEventosBiblioteca")
        public void procesarEventos(
                        @EventGridTrigger(name = "event") String eventContent,
                        final ExecutionContext context) {

                try {
                        context.getLogger().info("Cuerpo del evento recibido: " + eventContent);
                        List<EventGridEvent> events = EventGridEvent.fromString(eventContent);

                        for (EventGridEvent event : events) {
                                String tipoEvento = event.getEventType();
                                context.getLogger().info("Procesando evento tipo: " + tipoEvento);

                                switch (tipoEvento) {
                                        case "Usuario.Creado":
                                                insertarUsuario(event.getData().toObject(Usuario.class), context);
                                                break;

                                        case "Usuario.Eliminado":
                                                // Obtenemos el ID del usuario del evento
                                                int idUser = event.getData().toObject(Integer.class);
                                                // Llamamos al método que cumple con la regla de negocio del examen
                                                eliminarUsuarioCompleto(idUser, context);
                                                break;

                                        case "Libro.Creado":
                                                insertarLibro(event.getData().toObject(Libro.class), context);
                                                break;

                                        case "Libro.Eliminado":
                                                int idLibro = event.getData().toObject(Integer.class);
                                                eliminarRegistroGenerico("LIBRO", "ID_LIBRO", idLibro, context);
                                                break;

                                        case "Prestamo.Creado":
                                                gestionarNuevoPrestamo(event.getData().toObject(Prestamo.class),
                                                                context);
                                                break;

                                        case "Prestamo.Eliminado":
                                                int idPrestamo = event.getData().toObject(Integer.class);
                                                finalizarPrestamoYLiberarLibro(idPrestamo, context);
                                                break;

                                        case "Usuario.Modificado":
                                                // Usas el mismo método toObject que ya te funciona para Libro y
                                                // Prestamo
                                                insertarOActualizarUsuario(event.getData().toObject(Usuario.class),
                                                                context);
                                                break;

                                        default:
                                                context.getLogger()
                                                                .warning("Tipo de evento no reconocido: " + tipoEvento);
                                                break;
                                }
                        }
                } catch (Exception e) {
                        context.getLogger().severe("Error crítico en el despachador de eventos: " + e.getMessage());
                }
        }

        // --- LÓGICA PARA PRÉSTAMOS (TRANSACCIONAL) ---

        private void gestionarNuevoPrestamo(Prestamo p, ExecutionContext context) {
                try (Connection conn = OracleConnection.getConnection()) {
                        conn.setAutoCommit(false);

                        String sqlCheck = "SELECT DISPONIBLE FROM LIBRO WHERE ID_LIBRO = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sqlCheck)) {
                                stmt.setInt(1, p.getIdLibro());
                                ResultSet rs = stmt.executeQuery();

                                if (rs.next() && rs.getInt("DISPONIBLE") == 1) {
                                        // 1. Insertar Préstamo
                                        String sqlIns = "INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, ID_LIBRO) VALUES (?, ?, ?)";
                                        try (PreparedStatement stmtIns = conn.prepareStatement(sqlIns)) {
                                                stmtIns.setInt(1, p.getIdPrestamo());
                                                stmtIns.setInt(2, p.getIdUsuario());
                                                stmtIns.setInt(3, p.getIdLibro());
                                                stmtIns.executeUpdate();
                                        }

                                        // 2. Actualizar disponibilidad
                                        String sqlUpd = "UPDATE LIBRO SET DISPONIBLE = 0 WHERE ID_LIBRO = ?";
                                        try (PreparedStatement stmtUpd = conn.prepareStatement(sqlUpd)) {
                                                stmtUpd.setInt(1, p.getIdLibro());
                                                stmtUpd.executeUpdate();
                                        }

                                        conn.commit();
                                        context.getLogger().info("ÉXITO: Préstamo " + p.getIdPrestamo()
                                                        + " registrado en Oracle.");
                                } else {
                                        context.getLogger().warning(
                                                        "NEGADO: Libro " + p.getIdLibro() + " no está disponible.");
                                        conn.rollback();
                                }
                        }
                } catch (Exception e) {
                        context.getLogger().severe("ERROR en gestionarNuevoPrestamo: " + e.getMessage());
                }
        }

        private void finalizarPrestamoYLiberarLibro(int idPrestamo, ExecutionContext context) {
                try (Connection conn = OracleConnection.getConnection()) {
                        conn.setAutoCommit(false);

                        String sqlGet = "SELECT ID_LIBRO FROM PRESTAMO WHERE ID_PRESTAMO = ?";
                        int idLibro = -1;
                        try (PreparedStatement stmt = conn.prepareStatement(sqlGet)) {
                                stmt.setInt(1, idPrestamo);
                                ResultSet rs = stmt.executeQuery();
                                if (rs.next())
                                        idLibro = rs.getInt("ID_LIBRO");
                        }

                        if (idLibro != -1) {
                                // 1. Eliminar préstamo
                                String sqlDel = "DELETE FROM PRESTAMO WHERE ID_PRESTAMO = ?";
                                try (PreparedStatement stmtDel = conn.prepareStatement(sqlDel)) {
                                        stmtDel.setInt(1, idPrestamo);
                                        stmtDel.executeUpdate();
                                }

                                // 2. Liberar libro
                                String sqlRel = "UPDATE LIBRO SET DISPONIBLE = 1 WHERE ID_LIBRO = ?";
                                try (PreparedStatement stmtRel = conn.prepareStatement(sqlRel)) {
                                        stmtRel.setInt(1, idLibro);
                                        stmtRel.executeUpdate();
                                }

                                conn.commit();
                                context.getLogger().info("ÉXITO: Préstamo " + idPrestamo + " finalizado. Libro "
                                                + idLibro + " libre.");
                        }
                } catch (Exception e) {
                        context.getLogger().severe("ERROR en finalizarPrestamoYLiberarLibro: " + e.getMessage());
                }
        }

        // --- MÉTODOS DE SOPORTE (USUARIOS Y LIBROS) ---

        private void insertarUsuario(Usuario u, ExecutionContext context) throws Exception {
                String sql = "INSERT INTO USUARIO (ID_USUARIO, RUT, NOMBRES, APELLIDO_PATERNO, APELLIDO_MATERNO) VALUES (?, ?, ?, ?, ?)";
                try (Connection conn = OracleConnection.getConnection()) {
                        conn.setAutoCommit(true); // Asegura que se guarde de inmediato
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                                stmt.setInt(1, u.getIdUsuario());
                                stmt.setString(2, u.getRut());
                                stmt.setString(3, u.getNombres());
                                stmt.setString(4, u.getApellidoPaterno());
                                stmt.setString(5, u.getApellidoMaterno());
                                stmt.executeUpdate();
                                context.getLogger().info("ÉXITO: Usuario " + u.getRut() + " insertado en Oracle.");
                        }
                } catch (Exception e) {
                        context.getLogger().severe("Fallo al insertar usuario en Oracle: " + e.getMessage());
                        throw e;
                }
        }

        private void insertarLibro(Libro l, ExecutionContext context) throws Exception {
                String sql = "INSERT INTO LIBRO (ID_LIBRO, CODIGO_LIBRO, TITULO, AUTOR, DISPONIBLE) VALUES (?, ?, ?, ?, ?)";
                try (Connection conn = OracleConnection.getConnection()) {
                        conn.setAutoCommit(true);
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                                stmt.setInt(1, l.getIdLibro());
                                stmt.setString(2, l.getCodigoLibro());
                                stmt.setString(3, l.getTitulo());
                                stmt.setString(4, l.getAutor());
                                stmt.setInt(5, l.getDisponible());
                                stmt.executeUpdate();
                                context.getLogger().info("ÉXITO: Libro " + l.getTitulo() + " insertado en Oracle.");
                        }
                } catch (Exception e) {
                        context.getLogger().severe("Fallo al insertar libro en Oracle: " + e.getMessage());
                        throw e;
                }
        }

        private void eliminarRegistroGenerico(String tabla, String columnaId, int id, ExecutionContext context)
                        throws Exception {
                String sql = "DELETE FROM " + tabla + " WHERE " + columnaId + " = ?";
                try (Connection conn = OracleConnection.getConnection()) {
                        conn.setAutoCommit(true);
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                                stmt.setInt(1, id);
                                int rows = stmt.executeUpdate();
                                context.getLogger().info(
                                                "ÉXITO: Registro eliminado en " + tabla + ". Filas afectadas: " + rows);
                        }
                } catch (Exception e) {
                        context.getLogger().severe("Error eliminando en " + tabla + ": " + e.getMessage());
                        throw e;
                }
        }

        private void insertarOActualizarUsuario(Usuario u, ExecutionContext context) throws Exception {
                String sql = "UPDATE USUARIO SET NOMBRES = ?, APELLIDO_PATERNO = ?, APELLIDO_MATERNO = ? WHERE ID_USUARIO = ?";
                try (Connection conn = OracleConnection.getConnection()) {
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                                stmt.setString(1, u.getNombres());
                                stmt.setString(2, u.getApellidoPaterno());
                                stmt.setString(3, u.getApellidoMaterno());
                                stmt.setInt(4, u.getIdUsuario());
                                int filas = stmt.executeUpdate();
                                if (filas > 0) {
                                        context.getLogger()
                                                        .info("ÉXITO: Usuario " + u.getIdUsuario() + " actualizado.");
                                }
                        }
                }
        }

        private void eliminarUsuarioCompleto(int idUsuario, ExecutionContext context) throws Exception {
                // 1. SQL para liberar los libros (opcional pero recomendado)
                String sqlLiberar = "UPDATE LIBRO SET DISPONIBLE = 1 WHERE ID_LIBRO IN (SELECT ID_LIBRO FROM PRESTAMO WHERE ID_USUARIO = ?)";
                // 2. SQL para cumplir el REQUISITO 3.a.2 (Borrar préstamos asociados)
                String sqlPrestamos = "DELETE FROM PRESTAMO WHERE ID_USUARIO = ?";
                // 3. SQL para borrar el usuario
                String sqlUsuario = "DELETE FROM USUARIO WHERE ID_USUARIO = ?";

                try (Connection conn = OracleConnection.getConnection()) {
                        conn.setAutoCommit(false); // IMPORTANTE: Transacción para que no queden datos a medias
                        try (PreparedStatement st1 = conn.prepareStatement(sqlLiberar);
                                        PreparedStatement st2 = conn.prepareStatement(sqlPrestamos);
                                        PreparedStatement st3 = conn.prepareStatement(sqlUsuario)) {

                                st1.setInt(1, idUsuario);
                                st1.executeUpdate();

                                st2.setInt(1, idUsuario);
                                st2.executeUpdate();

                                st3.setInt(1, idUsuario);
                                int filas = st3.executeUpdate();

                                conn.commit();
                                context.getLogger().info("ÉXITO: Se eliminó el usuario " + idUsuario
                                                + " y sus préstamos asociados.");
                        } catch (Exception e) {
                                conn.rollback();
                                context.getLogger().severe("ERROR al eliminar usuario: " + e.getMessage());
                                throw e;
                        }
                }
        }
}