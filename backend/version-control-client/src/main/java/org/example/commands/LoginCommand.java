package org.example.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.EnvConfig;

import java.io.Console;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LoginCommand implements Command {

    private final ObjectMapper mapper;
    private final EnvConfig envConfig;
    private final HttpClient httpClient;

    public LoginCommand(ObjectMapper objectMapper, HttpClient httpClient, EnvConfig envConfig) {
        this.mapper = objectMapper;
        this.envConfig = envConfig;
        this.httpClient = httpClient;
    }

    @Override
    public void execute(String[] args) throws Exception {
        String email = null;
        String password = null;

        for (int i = 0; i < args.length; i++) {
            if ("--email".equals(args[i]) && i + 1 < args.length) {
                email = args[i + 1];
                i++;
            } else if ("--password".equals(args[i]) && i + 1 < args.length) {
                password = args[i + 1];
                i++;
            }
        }
        if (email == null || password == null) {
            System.out.println("You need to pass your email and password.");
            System.out.println("Usage: myvcs login --email <mail> --password <password>");
            return;
        }

        System.out.println("--- Logowanie do CILab VCS ---");

        String formData = "client_id=" + URLEncoder.encode(envConfig.clientId(), StandardCharsets.UTF_8)
                + "&grant_type=password"
                + "&username=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        password = null;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(envConfig.keycloakUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode rootNode = mapper.readTree(response.body());

            if (rootNode.has("access_token")) {
                String token = rootNode.get("access_token").asText();

                Files.writeString(Paths.get(envConfig.tokenFilePath()), token);
                System.out.println("Login successful!");
            } else {
                System.out.println("Get 200 OK, but no 'access_token' in response.");
            }
        } else {
            System.out.println("Login failed (Code: " + response.statusCode() + ").");
            System.out.println("Szczegóły z Keycloaka: " + response.body());
            System.out.println("DEBUG: Używany Client ID to: '" + envConfig.clientId() + "'");
        }
    }

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public String getDescription() {
        return "Login to CILab VCS server";
    }
}
