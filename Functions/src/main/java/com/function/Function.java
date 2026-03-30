package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class Function {

        private static final ObjectMapper mapper = new ObjectMapper();

        @FunctionName("HttpExample")
        public HttpResponseMessage run(
                        @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                return request.createResponseBuilder(HttpStatus.OK)
                                .body("Hola mundo desde Azure Functions")
                                .build();
        }

        @FunctionName("listarUsuarios")
        public HttpResponseMessage listarUsuarios(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                String sql = "SELECT ID_USUARIO, RUT, NOMBRES, APELLIDO_PATERNO, APELLIDO_MATERNO FROM USUARIO ORDER BY ID_USUARIO";
                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                try (Connection conn = OracleConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {
                                if (!first) {
                                        json.append(",");
                                }

                                String rut = safe(rs.getString("RUT"));
                                String nombres = safe(rs.getString("NOMBRES"));
                                String apPat = safe(rs.getString("APELLIDO_PATERNO"));
                                String apMat = safe(rs.getString("APELLIDO_MATERNO"));

                                json.append("{")
                                                .append("\"idUsuario\":").append(rs.getInt("ID_USUARIO")).append(",")
                                                .append("\"rut\":\"").append(rut).append("\",")
                                                .append("\"nombres\":\"").append(nombres).append("\",")
                                                .append("\"apellidoPaterno\":\"").append(apPat).append("\",")
                                                .append("\"apellidoMaterno\":\"").append(apMat).append("\"")
                                                .append("}");

                                first = false;
                        }

                        json.append("]");

                        return request.createResponseBuilder(HttpStatus.OK)
                                        .header("Content-Type", "application/json")
                                        .body(json.toString())
                                        .build();

                } catch (Exception e) {
                        context.getLogger().severe("Error al listar usuarios: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error al listar usuarios")
                                        .build();
                }
        }

        @FunctionName("crearUsuario")
        public HttpResponseMessage crearUsuario(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                try {
                        String body = request.getBody().orElse("");
                        Usuario usuario = mapper.readValue(body, Usuario.class);

                        if (usuario.getIdUsuario() == null ||
                                        usuario.getRut() == null || usuario.getRut().isBlank() ||
                                        usuario.getNombres() == null || usuario.getNombres().isBlank() ||
                                        usuario.getApellidoPaterno() == null
                                        || usuario.getApellidoPaterno().isBlank()) {

                                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                                .body("Datos de usuario inválidos")
                                                .build();
                        }

                        String sql = "INSERT INTO USUARIO (ID_USUARIO, RUT, NOMBRES, APELLIDO_PATERNO, APELLIDO_MATERNO) VALUES (?, ?, ?, ?, ?)";

                        try (Connection conn = OracleConnection.getConnection();
                                        PreparedStatement stmt = conn.prepareStatement(sql)) {

                                stmt.setInt(1, usuario.getIdUsuario());
                                stmt.setString(2, usuario.getRut());
                                stmt.setString(3, usuario.getNombres());
                                stmt.setString(4, usuario.getApellidoPaterno());
                                stmt.setString(5, usuario.getApellidoMaterno());

                                stmt.executeUpdate();

                                return request.createResponseBuilder(HttpStatus.OK)
                                                .header("Content-Type", "application/json")
                                                .body("{\"mensaje\":\"Usuario creado correctamente\"}")
                                                .build();
                        }

                } catch (Exception e) {
                        context.getLogger().severe("Error al crear usuario: " + e.getMessage());

                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .header("Content-Type", "application/json")
                                        .body("{\"error\":\"Error al crear usuario\",\"detalle\":\""
                                                        + e.getMessage().replace("\"", "\\\"") + "\"}")
                                        .build();
                }
        }

        @FunctionName("listarPrestamos")
        public HttpResponseMessage listarPrestamos(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                String sql = """
                                SELECT
                                    p.ID_PRESTAMO,
                                    u.ID_USUARIO,
                                    u.RUT,
                                    u.NOMBRES,
                                    u.APELLIDO_PATERNO,
                                    u.APELLIDO_MATERNO,
                                    l.ID_LIBRO,
                                    l.CODIGO_LIBRO,
                                    l.TITULO,
                                    l.AUTOR,
                                    l.DISPONIBLE
                                FROM PRESTAMO p
                                INNER JOIN USUARIO u ON p.ID_USUARIO = u.ID_USUARIO
                                INNER JOIN LIBRO l ON p.ID_LIBRO = l.ID_LIBRO
                                ORDER BY p.ID_PRESTAMO
                                """;

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                try (Connection conn = OracleConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql);
                                ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {
                                if (!first) {
                                        json.append(",");
                                }

                                json.append("{")
                                                .append("\"idPrestamo\":").append(rs.getInt("ID_PRESTAMO")).append(",")
                                                .append("\"usuario\":{")
                                                .append("\"idUsuario\":").append(rs.getInt("ID_USUARIO")).append(",")
                                                .append("\"rut\":\"").append(safe(rs.getString("RUT"))).append("\",")
                                                .append("\"nombres\":\"").append(safe(rs.getString("NOMBRES")))
                                                .append("\",")
                                                .append("\"apellidoPaterno\":\"")
                                                .append(safe(rs.getString("APELLIDO_PATERNO"))).append("\",")
                                                .append("\"apellidoMaterno\":\"")
                                                .append(safe(rs.getString("APELLIDO_MATERNO"))).append("\"")
                                                .append("},")
                                                .append("\"libro\":{")
                                                .append("\"idLibro\":").append(rs.getInt("ID_LIBRO")).append(",")
                                                .append("\"codigoLibro\":\"").append(safe(rs.getString("CODIGO_LIBRO")))
                                                .append("\",")
                                                .append("\"titulo\":\"").append(safe(rs.getString("TITULO")))
                                                .append("\",")
                                                .append("\"autor\":\"").append(safe(rs.getString("AUTOR")))
                                                .append("\",")
                                                .append("\"disponible\":").append(rs.getInt("DISPONIBLE"))
                                                .append("}")
                                                .append("}");

                                first = false;
                        }

                        json.append("]");

                        return request.createResponseBuilder(HttpStatus.OK)
                                        .header("Content-Type", "application/json")
                                        .body(json.toString())
                                        .build();

                } catch (Exception e) {
                        context.getLogger().severe("Error al listar préstamos: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error al listar préstamos")
                                        .build();
                }
        }

        @FunctionName("crearPrestamo")
        public HttpResponseMessage crearPrestamo(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                try {
                        String body = request.getBody().orElse("");
                        Prestamo prestamo = mapper.readValue(body, Prestamo.class);

                        if (prestamo.getIdPrestamo() == null ||
                                        prestamo.getIdUsuario() == null ||
                                        prestamo.getIdLibro() == null) {

                                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                                .body("Datos de préstamo inválidos")
                                                .build();
                        }

                        try (Connection conn = OracleConnection.getConnection()) {
                                conn.setAutoCommit(false);

                                String validarLibro = "SELECT DISPONIBLE FROM LIBRO WHERE ID_LIBRO = ?";
                                try (PreparedStatement stmtValidar = conn.prepareStatement(validarLibro)) {
                                        stmtValidar.setInt(1, prestamo.getIdLibro());

                                        try (ResultSet rs = stmtValidar.executeQuery()) {
                                                if (!rs.next()) {
                                                        conn.rollback();
                                                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                                                        .body("El libro no existe")
                                                                        .build();
                                                }

                                                int disponible = rs.getInt("DISPONIBLE");
                                                if (disponible == 0) {
                                                        conn.rollback();
                                                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                                                        .body("El libro no está disponible")
                                                                        .build();
                                                }
                                        }
                                }

                                String insertarPrestamo = "INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, ID_LIBRO) VALUES (?, ?, ?)";
                                try (PreparedStatement stmtInsert = conn.prepareStatement(insertarPrestamo)) {
                                        stmtInsert.setInt(1, prestamo.getIdPrestamo());
                                        stmtInsert.setInt(2, prestamo.getIdUsuario());
                                        stmtInsert.setInt(3, prestamo.getIdLibro());
                                        stmtInsert.executeUpdate();
                                }

                                String actualizarLibro = "UPDATE LIBRO SET DISPONIBLE = 0 WHERE ID_LIBRO = ?";
                                try (PreparedStatement stmtUpdate = conn.prepareStatement(actualizarLibro)) {
                                        stmtUpdate.setInt(1, prestamo.getIdLibro());
                                        stmtUpdate.executeUpdate();
                                }

                                conn.commit();

                                return request.createResponseBuilder(HttpStatus.OK)
                                                .header("Content-Type", "application/json")
                                                .body("{\"mensaje\":\"Préstamo registrado correctamente\"}")
                                                .build();
                        }

                } catch (Exception e) {
                        context.getLogger().severe("Error al crear préstamo: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error al crear préstamo")
                                        .build();
                }
        }

        private String safe(String value) {
                if (value == null) {
                        return "";
                }
                return value.replace("\"", "\\\"");
        }
}