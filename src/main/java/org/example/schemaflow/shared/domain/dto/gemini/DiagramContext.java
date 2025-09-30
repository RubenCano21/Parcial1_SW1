package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

import java.util.List;

@Data
public class DiagramContext {
    private List<Entity> entities;
    private List<Relationship> existingRelationships;
    private String userIntent;
}






