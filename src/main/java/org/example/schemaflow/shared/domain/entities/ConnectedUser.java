package org.example.schemaflow.shared.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedUser {
    private String userId;
    private String username;
    private Long connectedAt;
    private String avatar; // URL del avatar si lo tienes
    private String color; // Color asignado para el cursor

    public ConnectedUser(String userId, String username, long l) {

    }
}
