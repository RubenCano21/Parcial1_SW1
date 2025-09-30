package org.example.schemaflow.shared.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollaborationMessage {
    private CollaborationMessageType type;
    private String userId;
    private String username;
    private String projectId;
    private Map<String, Object> data;
    private Long timestamp;

    public CollaborationMessage(CollaborationMessageType type, String userId, String username,
                                String projectId, Map<String, Object> data) {
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.projectId = projectId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
