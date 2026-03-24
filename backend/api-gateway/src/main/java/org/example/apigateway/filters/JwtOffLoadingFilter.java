package org.example.apigateway.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
public class JwtOffLoadingFilter implements GlobalFilter, Ordered {
    private final ObjectMapper objectMapper;

    public JwtOffLoadingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] chunks = token.split("\\.");

                if (chunks.length > 1) {
                    String payloadBase64 = chunks[1];

                    // 1. Ręcznie naprawiamy brakujący padding dla Base64
                    int paddingLength = 4 - (payloadBase64.length() % 4);
                    if (paddingLength > 0 && paddingLength < 4) {
                        payloadBase64 += "=".repeat(paddingLength);
                    }

                    String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64));
                    JsonNode payloadNode = objectMapper.readTree(payloadJson);

                    String email = payloadNode.has("email") ? payloadNode.get("email").asText() : "";
                    String userId = payloadNode.has("sub") ? payloadNode.get("sub").asText() : "";

                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-User-Email", email)
                            .header("X-User-Id", userId)
                            .build();

                    ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();

                    return chain.filter(modifiedExchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
