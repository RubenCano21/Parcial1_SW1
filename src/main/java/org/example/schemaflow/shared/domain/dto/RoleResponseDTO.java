package org.example.schemaflow.shared.domain.dto;

import lombok.Data;
import org.example.schemaflow.shared.domain.entities.RoleName;

@Data
public class RoleResponseDTO {

    private Long id;
    private RoleName name;
}
