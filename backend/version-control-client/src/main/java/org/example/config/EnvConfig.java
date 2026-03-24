package org.example.config;

import java.nio.file.Paths;

public record EnvConfig(
        String keycloakUrl,
        String tokenFilePath,
        String vcsServerUrl,
        String clientId
) {

    public static EnvConfig load() {
        String homeDir = System.getProperty("user.home");
        String defaultTokenPath = Paths.get(homeDir, ".cilab-token").toString();

        return new EnvConfig(
                System.getenv().getOrDefault("KEYCLOAK_URL", "http://localhost:8085/realms/cilab-realm/protocol/openid-connect/token"),
                System.getenv().getOrDefault("TOKEN_FILE_PATH", defaultTokenPath),
                System.getenv().getOrDefault("VCS_SERVER_URL", "http://localhost:8000/api/v1/vcs"),
                System.getenv().getOrDefault("CLIENT_ID", "cilab-client")
        );
    }
}
