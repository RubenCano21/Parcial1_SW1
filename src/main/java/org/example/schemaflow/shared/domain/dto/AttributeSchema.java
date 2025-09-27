package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

@Data
public class AttributeSchema {

    private String name;
    private String type;
    private boolean primaryKey;
}
