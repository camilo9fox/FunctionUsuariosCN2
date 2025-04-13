package com.function.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GraphQLException extends RuntimeException implements GraphQLError {
    private static final long serialVersionUID = 1L;
    private final transient Map<String, Object> extensions;

    public GraphQLException(String message) {
        this(message, null, null);
    }

    public GraphQLException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public GraphQLException(String message, Map<String, Object> extensions) {
        this(message, null, extensions);
    }

    public GraphQLException(String message, Throwable cause, Map<String, Object> extensions) {
        super(message, cause);
        this.extensions = extensions;
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
        return extensions;
    }
}
