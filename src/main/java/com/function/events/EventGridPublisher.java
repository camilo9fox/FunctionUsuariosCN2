package com.function.events;

import com.function.model.Usuario;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EventGridPublisher {
    private final String topicEndpoint;
    private final String accessKey;
    private final HttpClient httpClient;
    private final Gson gson;
    private static final Logger LOGGER = Logger.getLogger(EventGridPublisher.class.getName());

    public EventGridPublisher() {
        this.topicEndpoint = "https://cn2-g7-topic.eastus2-1.eventgrid.azure.net/api/events";
        this.accessKey = "2nwi65UeMUy64n7PO4ERmuauQaJLyFffT4xwO8Yu529H8QSxfjf8JQQJ99BEACHYHv6XJ3w3AAABAZEGcgVF";

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                    @Override
                    public void write(JsonWriter out, LocalDateTime value) throws IOException {
                        out.value(value != null ? formatter.format(value) : null);
                    }

                    @Override
                    public LocalDateTime read(JsonReader in) throws IOException {
                        String datetime = in.nextString();
                        return datetime != null ? LocalDateTime.parse(datetime, formatter) : null;
                    }
                })
                .create();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void publishUserCreated(Usuario usuario) {
        publishEvent("UserCreated", usuario);
    }

    public void publishUserUpdated(Usuario usuario) {
        publishEvent("UserUpdated", usuario);
    }

    public void publishUserDeleted(Map<String, Object> deleteData) {
        publishEvent("UserDeleted", deleteData);
    }

    public void publishUserRetrieved(Usuario usuario) {
        publishEvent("UserRetrieved", usuario);
    }

    private void publishEvent(String eventType, Object data) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", UUID.randomUUID().toString());
        event.put("subject", "/" + eventType.toLowerCase());
        event.put("eventType", "com.function.usuarios." + eventType);
        event.put("data", data);
        event.put("eventTime", OffsetDateTime.now().toString());
        event.put("dataVersion", "1.0");
        event.put("topic", "");

        try {
            List<Map<String, Object>> events = Collections.singletonList(event);
            String jsonBody = gson.toJson(events);

            if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
                LOGGER.info(() -> "Enviando evento: " + jsonBody);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(topicEndpoint))
                    .header("aeg-sas-key", accessKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                LOGGER.severe(() -> String.format("Error al publicar evento. Status: %d, Body: %s, Request Body: %s",
                        response.statusCode(), response.body(), jsonBody));
            } else if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
                LOGGER.info(() -> String.format("Evento publicado exitosamente. Status: %d", response.statusCode()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe(() -> "Interrupción al publicar evento: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe(() -> "Error al publicar evento: " + e.getMessage() + ". Causa: " + e.getCause());
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                e.printStackTrace();
            }
        }
    }
}
