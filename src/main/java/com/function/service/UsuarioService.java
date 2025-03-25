package com.function.service;

import com.function.model.Usuario;
import com.function.exception.UsuarioNotFoundException;
import com.function.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

public class UsuarioService {
    private final EntityManagerFactory emf;
    private final Validator validator;

    public UsuarioService() {
        this.emf = Persistence.createEntityManagerFactory("OracleDB");
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    private void validar(Usuario usuario) {
        Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }

    public List<Usuario> obtenerTodos() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u", Usuario.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Usuario obtenerPorId(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Usuario usuario = em.find(Usuario.class, id);
            if (usuario == null) {
                throw new UsuarioNotFoundException(id);
            }
            return usuario;
        } finally {
            em.close();
        }
    }

    public Usuario crear(Usuario usuario) {
        validar(usuario);
        EntityManager em = emf.createEntityManager();
        try {
            // Encriptar la contraseña antes de guardar
            usuario.setPasswordHash(PasswordService.hashPassword(usuario.getPasswordHash()));
            
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(usuario);
            tx.commit();
            return usuario;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Usuario actualizar(Usuario usuario) {
        if (usuario.getId() == null) {
            Set<ConstraintViolation<Usuario>> violations = new HashSet<>();
            violations.add(validator.validateValue(Usuario.class, "id", null).iterator().next());
            throw new ValidationException(violations);
        }

        EntityManager em = emf.createEntityManager();
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            
            Usuario usuarioExistente = em.find(Usuario.class, usuario.getId());
            if (usuarioExistente == null) {
                throw new UsuarioNotFoundException(usuario.getId());
            }

            // Actualizar solo los campos no nulos
            if (usuario.getUsername() != null) {
                usuarioExistente.setUsername(usuario.getUsername());
            }
            if (usuario.getEmail() != null) {
                usuarioExistente.setEmail(usuario.getEmail());
            }
            if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().equals(usuarioExistente.getPasswordHash())) {
                usuarioExistente.setPasswordHash(PasswordService.hashPassword(usuario.getPasswordHash()));
            }
            if (usuario.getNombre() != null) {
                usuarioExistente.setNombre(usuario.getNombre());
            }
            if (usuario.getApellido() != null) {
                usuarioExistente.setApellido(usuario.getApellido());
            }
            if (usuario.getRol() != null) {
                usuarioExistente.setRol(usuario.getRol());
            }
            if (usuario.getActivo() != null) {
                usuarioExistente.setActivo(usuario.getActivo());
            }

            // Validar el objeto resultante antes de guardarlo
            validar(usuarioExistente);

            Usuario usuarioActualizado = em.merge(usuarioExistente);
            tx.commit();
            return usuarioActualizado;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void eliminar(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.find(Usuario.class, id);
            if (usuario == null) {
                throw new UsuarioNotFoundException(id);
            }
            em.remove(usuario);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
