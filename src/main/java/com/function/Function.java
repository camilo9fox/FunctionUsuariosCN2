package com.function;

import com.function.graphql.UsuarioResolver;
import com.function.service.UsuarioService;
import com.function.exception.GraphQLException;
import com.function.exception.ValidationException;
import com.google.gson.Gson;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import graphql.GraphQL;
import graphql.ExecutionInput;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class Function {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String MESSAGE_KEY = "message";
    private static final String EXTENSIONS_KEY = "extensions";
    private final GraphQL graphQL;
    private final Gson gson;

    public Function() {
        this.gson = new Gson();
        this.graphQL = initializeGraphQL();
    }

    private GraphQL initializeGraphQL() {
        InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("schema.graphqls");
        if (schemaStream == null) {
            throw new GraphQLException("No se pudo encontrar el archivo schema.graphqls");
        }

        try {
            String schemaContent;
            try (InputStreamReader reader = new InputStreamReader(schemaStream, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                schemaContent = sb.toString();
            }

            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry typeRegistry = schemaParser.parse(schemaContent);

            UsuarioService usuarioService = new UsuarioService();
            UsuarioResolver usuarioResolver = new UsuarioResolver(usuarioService);

            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                    .dataFetcher("usuarios", usuarioResolver.getUsuariosDataFetcher())
                    .dataFetcher("usuario", usuarioResolver.getUsuarioDataFetcher()))
                .type("Mutation", builder -> builder
                    .dataFetcher("crearUsuario", usuarioResolver.crearUsuarioDataFetcher())
                    .dataFetcher("actualizarUsuario", usuarioResolver.actualizarUsuarioDataFetcher())
                    .dataFetcher("eliminarUsuario", usuarioResolver.eliminarUsuarioDataFetcher()))
                .build();

            SchemaGenerator schemaGenerator = new SchemaGenerator();
            GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

            return GraphQL.newGraphQL(graphQLSchema).build();
        } catch (IOException e) {
            throw new GraphQLException("Error al inicializar GraphQL", e);
        }
    }

    @FunctionName("graphql-query")
    public HttpResponseMessage executeQuery(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        try {
            String body = request.getBody()
                .orElseThrow(() -> new ValidationException(Collections.singletonList("Se requiere el cuerpo de la solicitud")));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> json = gson.fromJson(body, Map.class);
            String query = (String) json.get("query");
            
            if (query == null || query.trim().isEmpty()) {
                throw new ValidationException(Collections.singletonList("Se requiere una consulta GraphQL"));
            }

            if (query.trim().toLowerCase().startsWith("mutation")) {
                throw new ValidationException(Collections.singletonList("Esta función es solo para queries. Use /api/graphql-mutation para mutaciones."));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) json.getOrDefault("variables", new HashMap<>());

            if (context != null && context.getLogger() != null) {
                Logger logger = context.getLogger();
                logger.info(String.format("Ejecutando query GraphQL: %s", query));
                logger.info(String.format("Variables: %s", gson.toJson(variables)));
            }

            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .build();

            Map<String, Object> result = graphQL.execute(executionInput).toSpecification();
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .body(gson.toJson(result))
                    .build();
                    
        } catch (Exception e) {
            return handleError(e, context, request);
        }
    }

    @FunctionName("graphql-mutation")
    public HttpResponseMessage executeMutation(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        try {
            String body = request.getBody()
                .orElseThrow(() -> new ValidationException(Collections.singletonList("Se requiere el cuerpo de la solicitud")));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> json = gson.fromJson(body, Map.class);
            String query = (String) json.get("query");
            
            if (query == null || query.trim().isEmpty()) {
                throw new ValidationException(Collections.singletonList("Se requiere una mutación GraphQL"));
            }

            if (!query.trim().toLowerCase().startsWith("mutation")) {
                throw new ValidationException(Collections.singletonList("Esta función es solo para mutaciones. Use /api/graphql-query para queries."));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) json.getOrDefault("variables", new HashMap<>());

            if (context != null && context.getLogger() != null) {
                Logger logger = context.getLogger();
                logger.info(String.format("Ejecutando mutation GraphQL: %s", query));
                logger.info(String.format("Variables: %s", gson.toJson(variables)));
            }

            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .build();

            Map<String, Object> result = graphQL.execute(executionInput).toSpecification();
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .body(gson.toJson(result))
                    .build();
                    
        } catch (Exception e) {
            return handleError(e, context, request);
        }
    }

    private HttpResponseMessage handleError(Exception e, ExecutionContext context, HttpRequestMessage<Optional<String>> request) {
        if (context != null && context.getLogger() != null) {
            Logger logger = context.getLogger();
            logger.severe("Error al procesar la solicitud GraphQL: " + e.getMessage());
            if (e.getCause() != null) {
                logger.severe("Causa: " + e.getCause().getMessage());
            }
        }
        
        Map<String, Object> error = new HashMap<>();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (e instanceof ValidationException) {
            ValidationException validationException = (ValidationException) e;
            error.put(MESSAGE_KEY, e.getMessage());
            error.put(EXTENSIONS_KEY, validationException.getExtensions());
        } else if (e instanceof GraphQLException) {
            GraphQLException graphQLException = (GraphQLException) e;
            error.put(MESSAGE_KEY, e.getMessage());
            if (graphQLException.getExtensions() != null) {
                error.put(EXTENSIONS_KEY, graphQLException.getExtensions());
            }
            if (graphQLException.getCause() != null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            error.put(MESSAGE_KEY, "Error interno del servidor: " + e.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        return request.createResponseBuilder(status)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .body(gson.toJson(Map.of("errors", Collections.singletonList(error))))
                .build();
    }
}
