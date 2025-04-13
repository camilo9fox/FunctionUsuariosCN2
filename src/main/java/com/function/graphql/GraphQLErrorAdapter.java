package com.function.graphql;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

public class GraphQLErrorAdapter implements GraphQLError {
    private final GraphQLError error;
    private final transient Map<String, Object> extensions;

    public GraphQLErrorAdapter(GraphQLError error, Map<String, Object> extensions) {
        this.error = error;
        this.extensions = extensions;
    }

    @Override
    public String getMessage() {
        return error.getMessage();
    }

    @Override
    public List<SourceLocation> getLocations() {
        return error.getLocations();
    }

    @Override
    public ErrorClassification getErrorType() {
        return error.getErrorType();
    }

    @Override
    public List<Object> getPath() {
        return error.getPath();
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }
}
