package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

import java.util.List;

@Data
public class RelationshipValidationRequest {
    private String fromEntity;
    private List<String> fromAttributes;
    private String toEntity;
    private List<String> toAttributes;
    private String cardinality;
    private String relationType;
}
