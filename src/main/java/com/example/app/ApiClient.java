package com.example.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8000";
    private static final String DEFAULT_API_KEY = "tajny-klucz-123";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public ApiClient() {
        this.baseUrl = System.getProperty("api.baseUrl", DEFAULT_BASE_URL);
        this.apiKey = System.getProperty("api.key", DEFAULT_API_KEY);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<Map<String, Object>> getList(String path) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(path)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        return objectMapper.readValue(
                response.body(),
                new TypeReference<List<Map<String, Object>>>() {}
        );
    }

    public Map<String, Object> create(String path, Map<String, Object> payload) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(payload);

        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = send(request);
        return objectMapper.readValue(
                response.body(),
                new TypeReference<Map<String, Object>>() {}
        );
    }

    public Map<String, Object> update(String path, Map<String, Object> payload) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(payload);

        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = send(request);
        return objectMapper.readValue(
                response.body(),
                new TypeReference<Map<String, Object>>() {}
        );
    }

    public void delete(String path) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(path)
                .DELETE()
                .build();

        send(request);
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("x-api-key", apiKey);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "API zwróciło błąd HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        return response;
    }
}
