package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.function.model.Usuario;
import com.function.service.UsuarioService;
import com.function.exception.UsuarioNotFoundException;
import com.function.exception.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) 
            (src, typeOfSrc, context) -> context.serialize(src.toString()))
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) 
            (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
        .create();
    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado";
    private static final String BODY_VACIO = "El cuerpo de la solicitud está vacío";
    private static final String ID_REQUERIDO = "Se requiere el ID del usuario";
    private final UsuarioService usuarioService;

    public Function() {
        this.usuarioService = new UsuarioService();
    }

    @FunctionName("usuarios")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        String method = request.getHttpMethod().name();

        try {
            switch (method) {
                case "GET":
                    return handleGet(request);
                case "POST":
                    return handlePost(request);
                case "PUT":
                    return handlePut(request);
                case "DELETE":
                    return handleDelete(request);
                default:
                    return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED)
                            .body("Método HTTP no soportado")
                            .build();
            }
        } catch (ValidationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error de validación");
            response.put("detalles", e.getViolations());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(gson.toJson(response))
                    .build();
        } catch (UsuarioNotFoundException e) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(e.getMessage())
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor: " + e.getMessage())
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request) {
        String id = request.getQueryParameters().get("id");
        if (id != null) {
            try {
                Usuario usuario = usuarioService.obtenerPorId(Long.parseLong(id));
                if (usuario == null) {
                    throw new UsuarioNotFoundException(USUARIO_NO_ENCONTRADO);
                }
                return request.createResponseBuilder(HttpStatus.OK)
                        .body(gson.toJson(usuario))
                        .build();
            } catch (NumberFormatException e) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("ID inválido")
                        .build();
            }
        }
        
        return request.createResponseBuilder(HttpStatus.OK)
                .body(gson.toJson(usuarioService.obtenerTodos()))
                .build();
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request) {
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(BODY_VACIO)
                    .build();
        }

        Usuario nuevoUsuario = gson.fromJson(requestBody, Usuario.class);
        Usuario usuarioCreado = usuarioService.crear(nuevoUsuario);
        
        return request.createResponseBuilder(HttpStatus.CREATED)
                .body(gson.toJson(usuarioCreado))
                .build();
    }

    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request) {
        String updateBody = request.getBody().orElse("");
        if (updateBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(BODY_VACIO)
                    .build();
        }

        Usuario usuarioActualizado = gson.fromJson(updateBody, Usuario.class);
        if (usuarioActualizado.getId() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(ID_REQUERIDO)
                    .build();
        }

        try {
            Usuario resultado = usuarioService.actualizar(usuarioActualizado);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(gson.toJson(resultado))
                    .build();
        } catch (RuntimeException e) {
            throw new UsuarioNotFoundException(USUARIO_NO_ENCONTRADO);
        }
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request) {
        String deleteId = request.getQueryParameters().get("id");
        if (deleteId == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(ID_REQUERIDO)
                    .build();
        }

        try {
            usuarioService.eliminar(Long.parseLong(deleteId));
            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Usuario eliminado correctamente")
                    .build();
        } catch (RuntimeException e) {
            throw new UsuarioNotFoundException(USUARIO_NO_ENCONTRADO);
        }
    }
}
