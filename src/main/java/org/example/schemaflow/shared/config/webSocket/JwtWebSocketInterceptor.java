package org.example.schemaflow.shared.config.webSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.config.TokenJWTConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtWebSocketInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        String token = extractTokenFromRequest(request);

        if (token == null) {
            log.warn("WebSocket connection rejected - no token provided");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Remover el prefijo "Bearer " si está presente
        if (token.startsWith(TokenJWTConfig.TOKEN_PREFIX)) {
            token = token.substring(TokenJWTConfig.TOKEN_PREFIX.length());
        }

        if (jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);

            if (userId != null && username != null) {
                attributes.put("userId", userId);
                attributes.put("username", username);
                attributes.put("roles", roles);
                attributes.put("token", token);

                // Extraer projectId de la URL
                String path = request.getURI().getPath();
                String projectId = extractProjectIdFromPath(path);
                attributes.put("projectId", projectId);

                log.info("WebSocket connection authorized for user: {} (ID: {}) in project: {}",
                        username, userId, projectId);
                return true;
            } else {
                log.warn("WebSocket connection rejected - invalid token claims");
            }
        } else {
            log.warn("WebSocket connection rejected - token validation failed");
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        }
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        // 1. Intentar obtener del header Authorization
        String authHeader = request.getHeaders().getFirst(TokenJWTConfig.HEADER_AUTHORIZATION);
        if (authHeader != null) {
            return authHeader; // Incluye el prefijo "Bearer "
        }

        // 2. Intentar obtener de query parameters (para WebSocket desde browser)
        String query = request.getURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return TokenJWTConfig.TOKEN_PREFIX + param.substring(6);
                }
            }
        }

        // 3. Intentar obtener de cookies (si usas cookies para JWT)
        List<String> cookies = request.getHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                String[] pairs = cookie.split("; ");
                for (String pair : pairs) {
                    if (pair.startsWith("token=")) {
                        return TokenJWTConfig.TOKEN_PREFIX + pair.substring(6);
                    }
                }
            }
        }

        return null;
    }

    private String extractProjectIdFromPath(String path) {
        // Formato esperado: /ws/collaboration/{projectId}
        String[] pathParts = path.split("/");
        if (pathParts.length >= 3) {
            return pathParts[pathParts.length - 1];
        }
        return "unknown";
    }
}
