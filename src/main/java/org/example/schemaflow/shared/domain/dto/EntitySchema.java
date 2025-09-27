package org.example.schemaflow.shared.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntitySchema {

    private String id;
    private String name;
    private String color;
    private List<AttributeSchema> attributes = new ArrayList<>();
}
