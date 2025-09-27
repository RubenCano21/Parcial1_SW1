package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

@Data
public class AttributeRequest {
    private String id;
    private String name;
    private String type;

    // Para entidades ER
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isRequired;

    // Para clases UML
    private String visibility; // "private", "public", "protected"
    private Boolean isStatic;
}
