package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.example.config.EnvConfig;
import org.example.dto.VCSContract.*;

public class VcsNetworkClient {

    private final EnvConfig envConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VcsNetworkClient(EnvConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.envConfig = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<String> initiatePush(String repoName, PushRequest pushRequest) throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(pushRequest);

        HttpRequest request = createAuthorizedRequest("/repo/" + repoName + "/push-init")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(java.net.URI.create(envConfig.vcsServerUrl() + "/repo/" + repoName + "/push-init"))
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
//                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 409 || response.statusCode() == 400) {
            throw new IllegalStateException(response.body());
        }
        else if (response.statusCode() != 200) {
            throw new IOException("Server error HTTP " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    public void uploadObjects(String repoName, Path zipFilePath) throws Exception {
        HttpRequest request = createAuthorizedRequest("/repo/" + repoName + "/push-objects")
                .setHeader("Content-Type", "application/zip")
                .POST(HttpRequest.BodyPublishers.ofFile(zipFilePath))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) throw new IOException("Upload failed: " + response.body());
    }

    public InputStream fetchRepositoryData(String repoName, String targetRef) throws Exception {
        HttpRequest request = createAuthorizedRequest("/repo/" + repoName + "/pull?target=" + targetRef)
                .GET()
                .build();

        // Used BodyHandlers.ofInputStream(), to avoid load whole ZIP to RAM
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Fetch failed: Server returned HTTP " + response.statusCode());
        }

        return response.body();
    }

    public HttpRequest.Builder createAuthorizedRequest(String endpoint) throws Exception {
        java.nio.file.Path tokenPath = Paths.get(envConfig.tokenFilePath());

        if (!Files.exists(tokenPath)) {
            throw new RuntimeException("Brak sesji. Zaloguj się używając komendy: myvcs login");
        }

        String token = Files.readString(tokenPath).trim();

        return HttpRequest.newBuilder()
                .uri(URI.create(envConfig.vcsServerUrl() + endpoint))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
    }
}
