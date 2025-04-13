package com.function.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import jakarta.validation.ConstraintViolation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationException extends RuntimeException implements GraphQLError {
    private static final long serialVersionUID = 1L;
    private final List<String> violations;

    public ValidationException(List<String> violations) {
        super("Error de validación: " + String.join(", ", violations));
        this.violations = violations;
    }

    public ValidationException(Set<? extends ConstraintViolation<?>> violations) {
        this(violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList()));
    }

    public List<String> getViolations() {
        return violations;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return Collections.emptyList();
    }

    @Override
    public ErrorClassification getErrorType() {
        return null;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("validationErrors", violations);
        return extensions;
    }
}
