package org.example.schemaflow.shared.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.domain.entities.CollaborationMessage;
import org.example.schemaflow.shared.domain.entities.ConnectedUser;
import org.example.schemaflow.shared.infrastucture.repositories.ProjectRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

    private final ProjectRepository projectRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mapa para mantener sesiones activas por proyecto
    private final Map<String, Map<String, WebSocketSession>> projectSessions = new ConcurrentHashMap<>();

    // Mapa para mantener información de usuarios conectados
    private final Map<String, Map<String, ConnectedUser>> connectedUsers = new ConcurrentHashMap<>();

    // Mapa para manejar locks de nodos
    private final Map<String, Map<String, String>> nodeLocks = new ConcurrentHashMap<>();

    public void addUserSession(String projectId, String userId, String username, WebSocketSession session, boolean b) {
        projectSessions.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>()).put(userId, session);

        ConnectedUser user = new ConnectedUser(userId, username, System.currentTimeMillis());
        connectedUsers.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>()).put(userId, user);

        log.info("Added user session: {} to project: {}", username, projectId);
    }

    public void removeUserSession(String projectId, String userId) {
        Map<String, WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions != null) {
            sessions.remove(userId);
            if (sessions.isEmpty()) {
                projectSessions.remove(projectId);
            }
        }

        Map<String, ConnectedUser> users = connectedUsers.get(projectId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                connectedUsers.remove(projectId);
            }
        }

        // Liberar todos los locks del usuario
        releaseUserLocks(projectId, userId);

        log.info("Removed user session: {} from project: {}", userId, projectId);
    }

    public List<ConnectedUser> getConnectedUsers(String projectId) {
        Map<String, ConnectedUser> users = connectedUsers.get(projectId);
        return users != null ? new ArrayList<>(users.values()) : new ArrayList<>();
    }

    public void broadcastToProject(String projectId, CollaborationMessage message, String excludeUserId) {
        Map<String, WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions == null) return;

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error serializing message", e);
            return;
        }

        sessions.entrySet().parallelStream()
                .filter(entry -> !entry.getKey().equals(excludeUserId))
                .forEach(entry -> {
                    WebSocketSession session = entry.getValue();
                    try {
                        if (session.isOpen()) {
                            synchronized (session) {
                                session.sendMessage(new TextMessage(messageJson));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error broadcasting to session: {}", entry.getKey(), e);
                        // Remover sesión inválida
                        sessions.remove(entry.getKey());
                    }
                });
    }

    public boolean canUserEdit(String projectId, String userId) {
        // Implementar lógica de permisos según tus necesidades
        // Por ahora, permitir a todos los usuarios conectados
        return connectedUsers.containsKey(projectId) &&
                connectedUsers.get(projectId).containsKey(userId);
    }

    public boolean lockNode(String projectId, String nodeId, String userId) {
        Map<String, String> locks = nodeLocks.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());

        // Verificar si ya está bloqueado por otro usuario
        String currentLock = locks.get(nodeId);
        if (currentLock != null && !currentLock.equals(userId)) {
            return false; // Ya está bloqueado por otro usuario
        }

        locks.put(nodeId, userId);

        // También guardarlo en Redis para persistencia
        String key = "lock:" + projectId + ":" + nodeId;
        redisTemplate.opsForValue().set(key, userId, Duration.ofMinutes(10));

        return true;
    }

    public void unlockNode(String projectId, String nodeId, String userId) {
        Map<String, String> locks = nodeLocks.get(projectId);
        if (locks != null && userId.equals(locks.get(nodeId))) {
            locks.remove(nodeId);

            // Remover de Redis también
            String key = "lock:" + projectId + ":" + nodeId;
            redisTemplate.delete(key);
        }
    }

    private void releaseUserLocks(String projectId, String userId) {
        Map<String, String> locks = nodeLocks.get(projectId);
        if (locks != null) {
            locks.entrySet().removeIf(entry -> userId.equals(entry.getValue()));
        }

        // También limpiar de Redis
        Set<String> keys = redisTemplate.keys("lock:" + projectId + ":*");
        if (keys != null) {
            for (String key : keys) {
                String lockUserId = (String) redisTemplate.opsForValue().get(key);
                if (userId.equals(lockUserId)) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    @Async
    public void saveNodeChanges(CollaborationMessage message) {
        try {
            // Implementar guardado de cambios en base de datos
            String projectId = message.getProjectId();
            Map<String, Object> nodeData = message.getData();

            log.debug("Saving node changes for project: {}", projectId);

            // Aquí puedes implementar la lógica de guardado
            // Por ejemplo, guardar en una tabla de historial de cambios

        } catch (Exception e) {
            log.error("Error saving node changes", e);
        }
    }

    @Async
    public void saveEdgeChanges(CollaborationMessage message) {
        try {
            String projectId = message.getProjectId();
            Map<String, Object> edgeData = message.getData();

            log.debug("Saving edge changes for project: {}", projectId);

            // Implementar lógica de guardado de edges

        } catch (Exception e) {
            log.error("Error saving edge changes", e);
        }
    }

    @Async
    public void deleteNode(CollaborationMessage message) {
        try {
            String projectId = message.getProjectId();
            String nodeId = (String) message.getData().get("nodeId");

            log.debug("Deleting node {} from project: {}", nodeId, projectId);

            // Implementar lógica de eliminación

        } catch (Exception e) {
            log.error("Error deleting node", e);
        }
    }

    @Async
    public void deleteEdge(CollaborationMessage message) {
        try {
            String projectId = message.getProjectId();
            String edgeId = (String) message.getData().get("edgeId");

            log.debug("Deleting edge {} from project: {}", edgeId, projectId);

            // Implementar lógica de eliminación

        } catch (Exception e) {
            log.error("Error deleting edge", e);
        }
    }

    public Set<String> getLockedNodes(String projectId) {
        return null;
    }
}
