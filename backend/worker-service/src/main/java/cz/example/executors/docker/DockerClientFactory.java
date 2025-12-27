package cz.example.executors.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;

public class DockerClientFactory {

    public static DockerClient createInstance() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .build();
        // TODO: set env: DOCKER_HOST=tcp://localhost:2375

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient client = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        try {
            client.pingCmd().exec();
            System.out.println("[Docker factory] Docker connected successfully");
        } catch (Exception e) {
            System.err.println("[Docker factory] Failed to connect to Docker: " + e.getMessage());
            throw new RuntimeException("Failed to connect to Docker", e);
        }

        return client;
    }
}
