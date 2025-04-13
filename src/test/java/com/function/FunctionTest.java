package com.function;

import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FunctionTest {

    private HttpRequestMessage<Optional<String>> getMockRequest(String body) {
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        
        doReturn(Optional.of(body)).when(req).getBody();
        doReturn(new HashMap<String, String>()).when(req).getHeaders();
        doReturn(new HashMap<String, String>()).when(req).getQueryParameters();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        return req;
    }

    private ExecutionContext getMockContext() {
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        return context;
    }

    @Test
    void testGraphQLQuery() {
        // Setup
        final String queryBody = "{\"query\":\"query { usuarios { id username email } }\"}";
        final HttpRequestMessage<Optional<String>> req = getMockRequest(queryBody);
        final ExecutionContext context = getMockContext();

        // Test
        final Function function = new Function();
        final HttpResponseMessage ret = function.executeQuery(req, context);

        // Verify
        assertEquals(HttpStatus.OK, ret.getStatus());
        assertNotNull(ret.getBody());
    }

    @Test
    void testGraphQLMutation() {
        // Setup
        final String mutationBody = "{\"query\":\"mutation { crearUsuario(input: {username: \\\"test\\\", email: \\\"test@test.com\\\"}) { id username email } }\"}";
        final HttpRequestMessage<Optional<String>> req = getMockRequest(mutationBody);
        final ExecutionContext context = getMockContext();

        // Test
        final Function function = new Function();
        final HttpResponseMessage ret = function.executeMutation(req, context);

        // Verify
        assertEquals(HttpStatus.OK, ret.getStatus());
        assertNotNull(ret.getBody());
    }

    @Test
    void testInvalidQueryInMutation() {
        // Setup
        final String queryBody = "{\"query\":\"query { usuarios { id username email } }\"}";
        final HttpRequestMessage<Optional<String>> req = getMockRequest(queryBody);
        final ExecutionContext context = getMockContext();

        // Test
        final Function function = new Function();
        final HttpResponseMessage ret = function.executeMutation(req, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    @Test
    void testInvalidMutationInQuery() {
        // Setup
        final String mutationBody = "{\"query\":\"mutation { crearUsuario(input: {username: \\\"test\\\", email: \\\"test@test.com\\\"}) { id username email } }\"}";
        final HttpRequestMessage<Optional<String>> req = getMockRequest(mutationBody);
        final ExecutionContext context = getMockContext();

        // Test
        final Function function = new Function();
        final HttpResponseMessage ret = function.executeQuery(req, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatus());
    }

    private static class HttpResponseMessageMock implements HttpResponseMessage {
        private final HttpStatusType httpStatus;
        private final Map<String, String> headers;
        private final Object body;

        HttpResponseMessageMock(HttpStatusType status, Map<String, String> headers, Object body) {
            this.httpStatus = status;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public HttpStatusType getStatus() {
            return this.httpStatus;
        }

        @Override
        public String getHeader(String key) {
            return this.headers.get(key);
        }

        @Override
        public Object getBody() {
            return this.body;
        }

        private static class HttpResponseMessageBuilderMock implements HttpResponseMessage.Builder {
            private HttpStatusType httpStatus;
            private final Map<String, String> headers = new HashMap<>();
            private Object body;

            public Builder status(HttpStatus status) {
                this.httpStatus = status;
                return this;
            }

            @Override
            public Builder status(HttpStatusType httpStatusType) {
                this.httpStatus = httpStatusType;
                return this;
            }

            @Override
            public HttpResponseMessage.Builder header(String key, String value) {
                this.headers.put(key, value);
                return this;
            }

            @Override
            public HttpResponseMessage.Builder body(Object body) {
                this.body = body;
                return this;
            }

            @Override
            public HttpResponseMessage build() {
                return new HttpResponseMessageMock(this.httpStatus, this.headers, this.body);
            }
        }
    }
}
