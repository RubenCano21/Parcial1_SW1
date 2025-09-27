package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

@Data
public class RelationshipSchema {

    private String id;
    private String sourceEntityId;
    private String targetEntityId;
    private String sourceEntityName;
    private String targetEntityName;
    private String type;
    private String label;
    private String sourceCardinality;
    private String targetCardinality;
}
