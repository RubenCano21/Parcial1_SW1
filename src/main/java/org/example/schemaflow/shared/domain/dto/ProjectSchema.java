package org.example.schemaflow.shared.domain.dto;

import jakarta.persistence.Table;
import lombok.Data;

import java.util.List;

@Data
@Table
public class ProjectSchema {

    private String projectName;
    private String packageBase;
    private List<EntitySchema> entities;
}
