package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        @FunctionName("graphql")
        public HttpResponseMessage graphql(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                try {
                        String body = request.getBody().orElse("");
                        String query = mapper.readTree(body).path("query").asText();

                        String schema = """
                                        type Query {
                                            libros: [Libro]
                                        }

                                        type Mutation {
                                            crearLibro(idLibro: Int, codigoLibro: String, titulo: String, autor: String, disponible: Int): Resultado
                                            actualizarLibro(idLibro: Int, codigoLibro: String, titulo: String, autor: String, disponible: Int): Resultado
                                            eliminarLibro(idLibro: Int): Resultado
                                        }

                                        type Libro {
                                            idLibro: Int
                                            codigoLibro: String
                                            titulo: String
                                            autor: String
                                            disponible: Int
                                        }

                                        type Resultado {
                                            mensaje: String
                                        }
                                        """;

                        DataFetcher<?> librosFetcher = env -> {
                                String sql = "SELECT ID_LIBRO, CODIGO_LIBRO, TITULO, AUTOR, DISPONIBLE FROM LIBRO ORDER BY ID_LIBRO";
                                List<Map<String, Object>> lista = new ArrayList<>();

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql);
                                                ResultSet rs = stmt.executeQuery()) {

                                        while (rs.next()) {
                                                Map<String, Object> libro = new HashMap<>();
                                                libro.put("idLibro", rs.getInt("ID_LIBRO"));
                                                libro.put("codigoLibro", rs.getString("CODIGO_LIBRO"));
                                                libro.put("titulo", rs.getString("TITULO"));
                                                libro.put("autor", rs.getString("AUTOR"));
                                                libro.put("disponible", rs.getInt("DISPONIBLE"));
                                                lista.add(libro);
                                        }
                                }

                                return lista;
                        };

                        DataFetcher<?> crearLibroFetcher = env -> {
                                Integer idLibro = env.getArgument("idLibro");
                                String codigoLibro = env.getArgument("codigoLibro");
                                String titulo = env.getArgument("titulo");
                                String autor = env.getArgument("autor");
                                Integer disponible = env.getArgument("disponible");

                                if (idLibro == null || codigoLibro == null || titulo == null || autor == null
                                                || disponible == null) {
                                        throw new RuntimeException(
                                                        "Todos los campos son obligatorios para crear el libro");
                                }

                                String sql = "INSERT INTO LIBRO (ID_LIBRO, CODIGO_LIBRO, TITULO, AUTOR, DISPONIBLE) VALUES (?, ?, ?, ?, ?)";

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                                        stmt.setInt(1, idLibro);
                                        stmt.setString(2, codigoLibro);
                                        stmt.setString(3, titulo);
                                        stmt.setString(4, autor);
                                        stmt.setInt(5, disponible);

                                        stmt.executeUpdate();
                                }

                                return Map.of("mensaje", "Libro creado correctamente");
                        };

                        DataFetcher<?> actualizarLibroFetcher = env -> {
                                Integer idLibro = env.getArgument("idLibro");
                                String codigoLibro = env.getArgument("codigoLibro");
                                String titulo = env.getArgument("titulo");
                                String autor = env.getArgument("autor");
                                Integer disponible = env.getArgument("disponible");

                                if (idLibro == null || codigoLibro == null || titulo == null || autor == null
                                                || disponible == null) {
                                        throw new RuntimeException(
                                                        "Todos los campos son obligatorios para actualizar el libro");
                                }

                                String sql = "UPDATE LIBRO SET CODIGO_LIBRO = ?, TITULO = ?, AUTOR = ?, DISPONIBLE = ? WHERE ID_LIBRO = ?";

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                                        stmt.setString(1, codigoLibro);
                                        stmt.setString(2, titulo);
                                        stmt.setString(3, autor);
                                        stmt.setInt(4, disponible);
                                        stmt.setInt(5, idLibro);

                                        int filas = stmt.executeUpdate();

                                        if (filas == 0) {
                                                return Map.of("mensaje", "No se encontró el libro para actualizar");
                                        }
                                }

                                return Map.of("mensaje", "Libro actualizado correctamente");
                        };

                        DataFetcher<?> eliminarLibroFetcher = env -> {
                                Integer idLibro = env.getArgument("idLibro");

                                if (idLibro == null) {
                                        throw new RuntimeException("El idLibro es obligatorio para eliminar");
                                }

                                String sql = "DELETE FROM LIBRO WHERE ID_LIBRO = ?";

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                                        stmt.setInt(1, idLibro);

                                        int filas = stmt.executeUpdate();

                                        if (filas == 0) {
                                                return Map.of("mensaje", "No se encontró el libro para eliminar");
                                        }
                                }

                                return Map.of("mensaje", "Libro eliminado correctamente");
                        };

                        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                                        .type("Query", builder -> builder.dataFetcher("libros", librosFetcher))
                                        .type("Mutation", builder -> builder
                                                        .dataFetcher("crearLibro", crearLibroFetcher)
                                                        .dataFetcher("actualizarLibro", actualizarLibroFetcher)
                                                        .dataFetcher("eliminarLibro", eliminarLibroFetcher))
                                        .build();

                        GraphQLSchema graphQLSchema = new SchemaGenerator()
                                        .makeExecutableSchema(new SchemaParser().parse(schema), wiring);

                        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

                        ExecutionResult result = graphQL.execute(query);

                        return request.createResponseBuilder(HttpStatus.OK)
                                        .header("Content-Type", "application/json")
                                        .body(mapper.writeValueAsString(result.toSpecification()))
                                        .build();

                } catch (Exception e) {
                        context.getLogger().severe("Error GraphQL: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .header("Content-Type", "application/json")
                                        .body("{\"errors\":[{\"message\":\"Error GraphQL\"}]}")
                                        .build();
                }
        }

        @FunctionName("graphqlUsuarios")
        public HttpResponseMessage graphqlUsuarios(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

                try {
                        String body = request.getBody().orElse("");
                        String query = mapper.readTree(body).path("query").asText();

                        String schema = """
                                        type Query {
                                            usuarios: [Usuario]
                                        }

                                        type Mutation {
                                            crearUsuarioGraphQL(idUsuario: Int, rut: String, nombres: String, apellidoPaterno: String, apellidoMaterno: String): Resultado
                                            eliminarUsuarioGraphQL(idUsuario: Int): Resultado
                                        }

                                        type Usuario {
                                            idUsuario: Int
                                            rut: String
                                            nombres: String
                                            apellidoPaterno: String
                                            apellidoMaterno: String
                                        }

                                        type Resultado {
                                            mensaje: String
                                        }
                                        """;

                        DataFetcher<?> usuariosFetcher = env -> {
                                String sql = "SELECT ID_USUARIO, RUT, NOMBRES, APELLIDO_PATERNO, APELLIDO_MATERNO FROM USUARIO ORDER BY ID_USUARIO";
                                List<Map<String, Object>> lista = new ArrayList<>();

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql);
                                                ResultSet rs = stmt.executeQuery()) {

                                        while (rs.next()) {
                                                Map<String, Object> usuario = new HashMap<>();
                                                usuario.put("idUsuario", rs.getInt("ID_USUARIO"));
                                                usuario.put("rut", rs.getString("RUT"));
                                                usuario.put("nombres", rs.getString("NOMBRES"));
                                                usuario.put("apellidoPaterno", rs.getString("APELLIDO_PATERNO"));
                                                usuario.put("apellidoMaterno", rs.getString("APELLIDO_MATERNO"));
                                                lista.add(usuario);
                                        }
                                }

                                return lista;
                        };

                        DataFetcher<?> crearUsuarioGraphQLFetcher = env -> {
                                Integer idUsuario = env.getArgument("idUsuario");
                                String rut = env.getArgument("rut");
                                String nombres = env.getArgument("nombres");
                                String apellidoPaterno = env.getArgument("apellidoPaterno");
                                String apellidoMaterno = env.getArgument("apellidoMaterno");

                                if (idUsuario == null || rut == null || rut.isBlank() ||
                                                nombres == null || nombres.isBlank() ||
                                                apellidoPaterno == null || apellidoPaterno.isBlank()) {
                                        throw new RuntimeException("Faltan datos obligatorios para crear el usuario");
                                }

                                String sql = "INSERT INTO USUARIO (ID_USUARIO, RUT, NOMBRES, APELLIDO_PATERNO, APELLIDO_MATERNO) VALUES (?, ?, ?, ?, ?)";

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                                        stmt.setInt(1, idUsuario);
                                        stmt.setString(2, rut);
                                        stmt.setString(3, nombres);
                                        stmt.setString(4, apellidoPaterno);
                                        stmt.setString(5, apellidoMaterno);

                                        stmt.executeUpdate();
                                }

                                return Map.of("mensaje", "Usuario creado correctamente");
                        };

                        DataFetcher<?> eliminarUsuarioGraphQLFetcher = env -> {
                                Integer idUsuario = env.getArgument("idUsuario");

                                if (idUsuario == null) {
                                        throw new RuntimeException("El idUsuario es obligatorio para eliminar");
                                }

                                String sql = "DELETE FROM USUARIO WHERE ID_USUARIO = ?";

                                try (Connection conn = OracleConnection.getConnection();
                                                PreparedStatement stmt = conn.prepareStatement(sql)) {

                                        stmt.setInt(1, idUsuario);
                                        int filas = stmt.executeUpdate();

                                        if (filas == 0) {
                                                return Map.of("mensaje", "No se encontró el usuario para eliminar");
                                        }
                                }

                                return Map.of("mensaje", "Usuario eliminado correctamente");
                        };

                        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                                        .type("Query", builder -> builder.dataFetcher("usuarios", usuariosFetcher))
                                        .type("Mutation", builder -> builder
                                                        .dataFetcher("crearUsuarioGraphQL", crearUsuarioGraphQLFetcher)
                                                        .dataFetcher("eliminarUsuarioGraphQL",
                                                                        eliminarUsuarioGraphQLFetcher))
                                        .build();

                        GraphQLSchema graphQLSchema = new SchemaGenerator()
                                        .makeExecutableSchema(new SchemaParser().parse(schema), wiring);

                        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

                        ExecutionResult result = graphQL.execute(query);

                        return request.createResponseBuilder(HttpStatus.OK)
                                        .header("Content-Type", "application/json")
                                        .body(mapper.writeValueAsString(result.toSpecification()))
                                        .build();

                } catch (Exception e) {
                        context.getLogger().severe("Error GraphQL Usuarios: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .header("Content-Type", "application/json")
                                        .body("{\"errors\":[{\"message\":\"Error GraphQL Usuarios\"}]}")
                                        .build();
                }
        }
}