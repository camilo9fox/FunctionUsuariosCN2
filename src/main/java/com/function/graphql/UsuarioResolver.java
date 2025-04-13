package com.function.graphql;

import com.function.model.Usuario;
import com.function.model.Rol;
import com.function.service.UsuarioService;
import com.function.exception.ValidationException;
import com.function.exception.GraphQLException;
import graphql.schema.DataFetcher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.*;

public class UsuarioResolver {
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_NOMBRE = "nombre";
    private static final String FIELD_APELLIDO = "apellido";
    private static final String FIELD_ROL = "rol";
    private static final String FIELD_ACTIVO = "activo";
    private static final String FIELD_ID = "id";

    private final UsuarioService usuarioService;
    private final EntityManagerFactory emf;

    public UsuarioResolver(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        this.emf = Persistence.createEntityManagerFactory("OracleDB");
    }

    public DataFetcher<List<Usuario>> getUsuariosDataFetcher() {
        return environment -> usuarioService.obtenerTodos();
    }

    public DataFetcher<Usuario> getUsuarioDataFetcher() {
        return environment -> {
            String id = environment.getArgument(FIELD_ID);
            try {
                return usuarioService.obtenerPorId(Long.parseLong(id));
            } catch (Exception e) {
                throw new GraphQLException("Usuario no encontrado con ID: " + id);
            }
        };
    }

    private void validarDatosUsuario(Map<String, Object> input, List<String> errores) {
        String username = (String) input.get(FIELD_USERNAME);
        if (username == null || username.trim().isEmpty()) {
            errores.add("El nombre de usuario es requerido");
        }
        
        String email = (String) input.get(FIELD_EMAIL);
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errores.add("Email inválido");
        }
        
        String password = (String) input.get(FIELD_PASSWORD);
        if (password == null || password.length() < 6) {
            errores.add("La contraseña debe tener al menos 6 caracteres");
        }
        
        Object rolId = input.get(FIELD_ROL);
        if (rolId == null) {
            errores.add("El rol es requerido");
        }
    }

    private Rol obtenerRol(Object rolId) {
        EntityManager em = emf.createEntityManager();
        try {
            if (rolId instanceof String) {
                // Si es un String, asumimos que es el ID del rol
                Long id = Long.parseLong((String) rolId);
                Rol rol = em.find(Rol.class, id);
                if (rol == null) {
                    throw new GraphQLException("Rol no encontrado con ID: " + id);
                }
                return rol;
            } else if (rolId instanceof Number) {
                // Si es un número, lo convertimos a Long
                Long id = ((Number) rolId).longValue();
                Rol rol = em.find(Rol.class, id);
                if (rol == null) {
                    throw new GraphQLException("Rol no encontrado con ID: " + id);
                }
                return rol;
            } else {
                throw new GraphQLException("Formato de ID de rol inválido");
            }
        } finally {
            em.close();
        }
    }

    private Usuario crearUsuarioDesdeInput(Map<String, Object> input) {
        Usuario usuario = new Usuario();
        usuario.setUsername((String) input.get(FIELD_USERNAME));
        usuario.setEmail((String) input.get(FIELD_EMAIL));
        usuario.setPasswordHash((String) input.get(FIELD_PASSWORD));
        usuario.setNombre((String) input.get(FIELD_NOMBRE));
        usuario.setApellido((String) input.get(FIELD_APELLIDO));
        
        // Obtener el rol existente por ID
        Rol rol = obtenerRol(input.get(FIELD_ROL));
        usuario.setRol(rol);
        
        usuario.setActivo(true);
        return usuario;
    }

    public DataFetcher<Usuario> crearUsuarioDataFetcher() {
        return environment -> {
            try {
                Map<String, Object> input = environment.getArgument("input");
                List<String> errores = new ArrayList<>();
                
                validarDatosUsuario(input, errores);
                
                if (!errores.isEmpty()) {
                    throw new ValidationException(errores);
                }

                Usuario usuario = crearUsuarioDesdeInput(input);
                return usuarioService.crear(usuario);
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new GraphQLException("Error al crear usuario: " + e.getMessage());
            }
        };
    }

    private void validarYActualizarCampo(Usuario usuario, Map<String, Object> input, String campo) {
        Object valor = input.get(campo);
        if (valor != null) {
            switch (campo) {
                case FIELD_USERNAME:
                    usuario.setUsername((String) valor);
                    break;
                case FIELD_EMAIL:
                    String email = (String) valor;
                    if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        throw new ValidationException(Collections.singletonList("Email inválido"));
                    }
                    usuario.setEmail(email);
                    break;
                case FIELD_PASSWORD:
                    String password = (String) valor;
                    if (password.length() < 6) {
                        throw new ValidationException(Collections.singletonList("La contraseña debe tener al menos 6 caracteres"));
                    }
                    usuario.setPasswordHash(password);
                    break;
                case FIELD_NOMBRE:
                    usuario.setNombre((String) valor);
                    break;
                case FIELD_APELLIDO:
                    usuario.setApellido((String) valor);
                    break;
                case FIELD_ROL:
                    Rol rol = obtenerRol(valor);
                    usuario.setRol(rol);
                    break;
                case FIELD_ACTIVO:
                    usuario.setActivo((Boolean) valor);
                    break;
                default:
                    throw new ValidationException(Collections.singletonList("Campo no reconocido: " + campo));
            }
        }
    }

    public DataFetcher<Usuario> actualizarUsuarioDataFetcher() {
        return environment -> {
            try {
                Map<String, Object> input = environment.getArgument("input");
                Long id = Long.parseLong((String) input.get(FIELD_ID));
                
                Usuario usuario = usuarioService.obtenerPorId(id);
                if (usuario == null) {
                    throw new ValidationException(Collections.singletonList("Usuario no encontrado con ID: " + id));
                }

                validarYActualizarCampo(usuario, input, FIELD_USERNAME);
                validarYActualizarCampo(usuario, input, FIELD_EMAIL);
                validarYActualizarCampo(usuario, input, FIELD_PASSWORD);
                validarYActualizarCampo(usuario, input, FIELD_NOMBRE);
                validarYActualizarCampo(usuario, input, FIELD_APELLIDO);
                validarYActualizarCampo(usuario, input, FIELD_ROL);
                validarYActualizarCampo(usuario, input, FIELD_ACTIVO);
                
                return usuarioService.actualizar(usuario);
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new GraphQLException("Error al actualizar usuario: " + e.getMessage());
            }
        };
    }

    public DataFetcher<Boolean> eliminarUsuarioDataFetcher() {
        return environment -> {
            try {
                String id = environment.getArgument(FIELD_ID);
                usuarioService.eliminar(Long.parseLong(id));
                return true;
            } catch (Exception e) {
                throw new GraphQLException("Error al eliminar usuario: " + e.getMessage());
            }
        };
    }
}
