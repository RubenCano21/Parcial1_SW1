package org.example.schemaflow.shared.domain.dto.gemini;

import lombok.Data;

import java.util.List;

@Data
public class Entity {
    private String name;
    private List<String> attributes;
}