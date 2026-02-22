package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.example.dto.VCSContract.*;

public class VcsNetworkClient {

    private final String serverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VcsNetworkClient(String serverUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.serverUrl = serverUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<String> initiatePush(String repoName, PushRequest pushRequest) throws IOException, InterruptedException {
        String jsonPayload = objectMapper.writeValueAsString(pushRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(serverUrl + "/repo/" + repoName + "/push-init"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 409 || response.statusCode() == 400) {
            throw new IllegalStateException(response.body());
        }
        else if (response.statusCode() != 200) {
            throw new IOException("Server error HTTP " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    public void uploadObjects(String repoName, Path zipFilePath) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(serverUrl + "/repo/" + repoName + "/push-objects"))
                .header("Content-Type", "application/zip")
                .POST(HttpRequest.BodyPublishers.ofFile(zipFilePath))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) throw new IOException("Upload failed: " + response.body());
    }

    public InputStream fetchRepositoryData(String repoName, String targetRef) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/repo/" + repoName + "/pull?target=" + targetRef))
                .GET()
                .build();

        // Used BodyHandlers.ofInputStream(), to avoid load whole ZIP to RAM
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Fetch failed: Server returned HTTP " + response.statusCode());
        }

        return response.body();
    }
}
