package org.example.schemaflow.shared.config.webSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.schemaflow.shared.infrastucture.repositories.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationAuthService {

    private final ProjectRepository projectRepository; // Tu repositorio de proyectos existente

    /**
     * Verifica si el usuario tiene permisos para acceder al proyecto
     */
    public boolean canAccessProject(String projectId, String userId, List<String> roles) {
        try {
            // Si es un proyecto temporal, permitir acceso
            if (projectId.startsWith("temp-") || "unknown".equals(projectId)) {
                return true;
            }

            // Verificar si el usuario es admin
            if (roles.contains("ROLE_ADMIN") || roles.contains("ADMIN")) {
                return true;
            }

            // Verificar ownership o permisos específicos del proyecto
            // Esto depende de tu modelo de datos específico
            return checkProjectPermissions(projectId, userId);

        } catch (Exception e) {
            log.error("Error checking project access for user {} in project {}", userId, projectId, e);
            return false;
        }
    }

    /**
     * Verifica si el usuario puede editar el proyecto
     */
    public boolean canEditProject(String projectId, String userId, List<String> roles) {
        try {
            // Los admins siempre pueden editar
            if (roles.contains("ROLE_ADMIN") || roles.contains("ADMIN")) {
                return true;
            }

            // Para proyectos temporales, permitir edición
            if (projectId.startsWith("temp-")) {
                return true;
            }

            // Verificar permisos de escritura específicos
            return checkEditPermissions(projectId, userId);

        } catch (Exception e) {
            log.error("Error checking edit permissions for user {} in project {}", userId, projectId, e);
            return false;
        }
    }

    private boolean checkProjectPermissions(String projectId, String userId) {
        // Implementar según tu lógica de negocio
        // Por ejemplo:
        // - Verificar si el usuario es owner del proyecto
        // - Verificar si el usuario tiene permisos compartidos
        // - Verificar membresía en equipos con acceso al proyecto

        // Placeholder implementation:
        return true; // Por ahora permitir acceso a todos
    }

    private boolean checkEditPermissions(String projectId, String userId) {
        // Similar a checkProjectPermissions pero para permisos de escritura
        return true; // Por ahora permitir edición a todos
    }
}
