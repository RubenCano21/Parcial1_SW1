package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

@Data
public class Relationship {
    private String fromEntity;
    private String toEntity;
    private String cardinality;
    private String type;
}
