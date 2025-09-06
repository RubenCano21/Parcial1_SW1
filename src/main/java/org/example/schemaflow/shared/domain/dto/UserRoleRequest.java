package org.example.schemaflow.shared.domain.dto;

import lombok.Data;
import org.example.schemaflow.shared.domain.entities.RoleName;

@Data
public class UserRoleRequest {

    private Long userId;
    private RoleName roleName;
}
