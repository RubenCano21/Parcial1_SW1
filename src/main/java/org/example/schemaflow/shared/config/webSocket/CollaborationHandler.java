package org.example.schemaflow.shared.config.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.application.service.CollaborationService;
import org.example.schemaflow.shared.domain.entities.CollaborationMessage;
import org.example.schemaflow.shared.domain.entities.CollaborationMessageType;
import org.example.schemaflow.shared.domain.entities.ConnectedUser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CollaborationHandler extends TextWebSocketHandler {

    private final CollaborationService collaborationService;
    private final CollaborationAuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        String projectId = (String) session.getAttributes().get("projectId");
        List<String> roles = (List<String>) session.getAttributes().get("roles");

        // Verificar permisos de acceso al proyecto
        if (!authService.canAccessProject(projectId, userId, roles)) {
            log.warn("User {} denied access to project {}", username, projectId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Access denied to project"));
            return;
        }

        log.info("User {} connected to project {} with roles: {}", username, projectId, roles);

        // Registrar la sesión con información de permisos
        collaborationService.addUserSession(projectId, userId, username, session,
                authService.canEditProject(projectId, userId, roles));

        // Notificar a otros usuarios de la conexión
        CollaborationMessage joinMessage = new CollaborationMessage(
                CollaborationMessageType.USER_JOINED,
                userId,
                username,
                projectId,
                Map.of(
                        "username", username,
                        "userId", userId,
                        "canEdit", authService.canEditProject(projectId, userId, roles)
                )
        );

        collaborationService.broadcastToProject(projectId, joinMessage, userId);

        // Enviar estado actual del proyecto al nuevo usuario
        sendCurrentProjectState(session, projectId);
    }

    private void sendCurrentProjectState(WebSocketSession session, String projectId) {
        try {
            // Enviar lista de usuarios conectados
            List<ConnectedUser> connectedUsers = collaborationService.getConnectedUsers(projectId);
            CollaborationMessage usersMessage = new CollaborationMessage(
                    CollaborationMessageType.CONNECTED_USERS,
                    "system",
                    "System",
                    projectId,
                    Map.of("users", connectedUsers)
            );
            sendMessage(session, usersMessage);

            // Enviar nodos actualmente bloqueados
            Set<String> lockedNodes = collaborationService.getLockedNodes(projectId);
            CollaborationMessage locksMessage = new CollaborationMessage(
                    CollaborationMessageType.LOCKED_NODES,
                    "system",
                    "System",
                    projectId,
                    Map.of("lockedNodes", lockedNodes)
            );
            sendMessage(session, locksMessage);

        } catch (Exception e) {
            log.error("Error sending project state to new user", e);
        }
    }

    // Método para validar operaciones antes de procesarlas
    private boolean validateOperation(WebSocketSession session, CollaborationMessage message) {
        String userId = (String) session.getAttributes().get("userId");
        String projectId = (String) session.getAttributes().get("projectId");
        List<String> roles = (List<String>) session.getAttributes().get("roles");

        // Operaciones que requieren permisos de edición
        Set<CollaborationMessageType> editOperations = Set.of(
                CollaborationMessageType.NODE_CREATED,
                CollaborationMessageType.NODE_UPDATED,
                CollaborationMessageType.NODE_DELETED,
                CollaborationMessageType.EDGE_CREATED,
                CollaborationMessageType.EDGE_UPDATED,
                CollaborationMessageType.EDGE_DELETED,
                CollaborationMessageType.NODE_LOCKED,
                CollaborationMessageType.NODE_UNLOCKED
        );

        if (editOperations.contains(message.getType())) {
            if (!authService.canEditProject(projectId, userId, roles)) {
                sendErrorMessage(session, "No tienes permisos para editar este proyecto");
                return false;
            }
        }

        return true;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String userId = (String) session.getAttributes().get("userId");
            String username = (String) session.getAttributes().get("username");
            String projectId = (String) session.getAttributes().get("projectId");

            CollaborationMessage incomingMessage = objectMapper.readValue(
                    message.getPayload(), CollaborationMessage.class);

            incomingMessage.setUserId(userId);
            incomingMessage.setUsername(username);
            incomingMessage.setProjectId(projectId);
            incomingMessage.setTimestamp(System.currentTimeMillis());

            // Validar la operación antes de procesarla
            if (!validateOperation(session, incomingMessage)) {
                return;
            }

            log.debug("Processing message: {} from user: {} in project: {}",
                    incomingMessage.getType(), username, projectId);

            // Procesar el mensaje
            handleCollaborationMessage(incomingMessage);

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendErrorMessage(session, "Error procesando mensaje: " + e.getMessage());
        }
    }

    // Resto de métodos existentes...
    private void handleCollaborationMessage(CollaborationMessage message) {
        // Implementación existente...
    }

    private void sendMessage(WebSocketSession session, CollaborationMessage message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("Error sending message to session", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        CollaborationMessage error = new CollaborationMessage(
                CollaborationMessageType.ERROR,
                "system",
                "System",
                "",
                Map.of("message", errorMessage)
        );
        sendMessage(session, error);
    }
}
