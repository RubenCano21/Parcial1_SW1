package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class EntitySchema {

    private String name;
    private List<AttributeSchema> attributes;
}
